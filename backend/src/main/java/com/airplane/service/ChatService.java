package com.airplane.service;

import com.airplane.config.RagProperties;
import com.airplane.domain.ChatTurn;
import com.airplane.domain.Organization;
import com.airplane.domain.RetrievalResult;
import com.airplane.domain.RetrievedDocument;
import com.airplane.dto.ChatRequest;
import com.airplane.dto.ChatResponse;
import com.airplane.dto.SourceResponse;
import com.airplane.feedback.FeedbackService;
import com.airplane.llm.LlmClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** RAG 파이프라인: 검색(Retrieve) → 범위 판정 → 문맥 구성(Augment) → 답변 생성(Generate). */
@Service
public class ChatService {

    static final String OUT_OF_SCOPE_ANSWER = """
            이 질문은 제가 가진 여행사·항공사 Q&A 자료 범위 밖이라 정확히 답하기 어려워요.
            지금은 하나투어, 모두투어, 노랑풍선, 참좋은여행, 대한항공, 제주항공, 에어로케이의 \
            예약·취소·환불, 수하물, 체크인 관련 질문에 답할 수 있어요.""";

    private final RetrievalService retrievalService;
    private final OrganizationService organizationService;
    private final LlmClient llmClient;
    private final RagProperties properties;
    private final FeedbackService feedbackService;

    public ChatService(RetrievalService retrievalService, OrganizationService organizationService,
                       LlmClient llmClient, RagProperties properties, FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
        this.retrievalService = retrievalService;
        this.organizationService = organizationService;
        this.llmClient = llmClient;
        this.properties = properties;
    }

    public ChatResponse ask(ChatRequest request) {
        String code = request.orgCode() == null ? "ALL" : request.orgCode();
        Organization selected = "ALL".equals(code) ? null : organizationService.getByCode(code);

        RetrievalResult result = retrievalService.retrieve(request.question(), selected, previousUserQuestion(request));

        if (result.outOfScope() || result.documents().isEmpty()) {
            // 근거 없는 상태로 LLM 을 부르지 않는다: 환각 방지 + 비용 절감. 데이터 공백 파악용으로 기록
            feedbackService.recordOutOfScope(request.question(), code);
            return new ChatResponse(OUT_OF_SCOPE_ANSWER, "out_of_scope", result.mode(), List.of());
        }

        String scopeLabel = result.orgFilter() != null ? result.orgFilter() : "전체 기관";
        String answer = llmClient.generate(request.question(), result.documents(), toHistory(request), scopeLabel);
        return new ChatResponse(answer, llmClient.mode(), result.mode(), toSources(result.documents()));
    }

    private static List<SourceResponse> toSources(List<RetrievedDocument> docs) {
        List<SourceResponse> sources = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            RetrievedDocument r = docs.get(i);
            double score = Double.isNaN(r.denseScore()) ? 0 : Math.round(r.denseScore() * 1000) / 1000.0;
            sources.add(new SourceResponse(i + 1, r.doc().id(), r.doc().org(), r.doc().orgType(), r.doc().question(),
                    snippet(r.doc().answer()), r.doc().source(), r.doc().official(), score));
        }
        return sources;
    }

    private static String previousUserQuestion(ChatRequest request) {
        if (request.history() == null) return null;
        for (int i = request.history().size() - 1; i >= 0; i--) {
            ChatRequest.HistoryMessage m = request.history().get(i);
            if ("user".equals(m.role()) && m.content() != null && !m.content().isBlank()) return m.content();
        }
        return null;
    }

    private List<ChatTurn> toHistory(ChatRequest request) {
        if (request.history() == null) return List.of();
        int max = properties.llm().historyTurns() * 2;
        List<ChatRequest.HistoryMessage> h = request.history();
        List<ChatRequest.HistoryMessage> recent = h.subList(Math.max(0, h.size() - max), h.size());
        List<ChatTurn> turns = new ArrayList<>();
        for (ChatRequest.HistoryMessage m : recent) {
            if (m.content() == null || m.content().isBlank()) continue;
            ChatTurn.Role role = "assistant".equals(m.role()) ? ChatTurn.Role.ASSISTANT : ChatTurn.Role.USER;
            // Messages API 는 user 로 시작하고 역할이 번갈아야 하므로 정규화
            if (turns.isEmpty() && role == ChatTurn.Role.ASSISTANT) continue;
            if (!turns.isEmpty() && turns.get(turns.size() - 1).role() == role) continue;
            turns.add(new ChatTurn(role, m.content()));
        }
        if (!turns.isEmpty() && turns.get(turns.size() - 1).role() == ChatTurn.Role.USER) {
            turns.remove(turns.size() - 1);
        }
        return turns;
    }

    private static String snippet(String content) {
        String flat = content.replaceAll("\\s+", " ").strip();
        return flat.length() > 120 ? flat.substring(0, 120) + "…" : flat;
    }
}
