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
import com.airplane.repository.OrganizationRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** RAG 파이프라인: 검색(Retrieve) → 범위 판정 → 문맥 구성(Augment) → 답변 생성(Generate). */
@Service
public class ChatService {

    private final RetrievalService retrievalService;
    private final OrganizationService organizationService;
    private final LlmClient llmClient;
    private final RagProperties properties;
    private final FeedbackService feedbackService;
    private final String outOfScopeAnswer;

    public ChatService(RetrievalService retrievalService, OrganizationService organizationService,
                       LlmClient llmClient, RagProperties properties, FeedbackService feedbackService,
                       OrganizationRepository organizationRepository) {
        this.feedbackService = feedbackService;
        this.outOfScopeAnswer = buildOutOfScopeAnswer(organizationRepository.findAll());
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
            String answer = result.unsupportedOrg() == null ? outOfScopeAnswer
                    : "'" + result.unsupportedOrg() + "' 관련 자료는 아직 없어서 정확히 답하기 어려워요. "
                      + "다른 기관 규정으로 대신 안내하면 틀릴 수 있어서, 해당 기관 공식 홈페이지나 고객센터에서 확인해 주세요.\n\n"
                      + outOfScopeAnswer.substring(outOfScopeAnswer.indexOf('\n') + 1);
            return new ChatResponse(answer, "out_of_scope", result.mode(), List.of());
        }

        String scopeLabel = result.orgFilter() != null ? result.orgFilter() : "전체 기관";
        String answer = llmClient.generate(request.question(), result.documents(), toHistory(request), scopeLabel);
        return new ChatResponse(answer, llmClient.mode(), result.mode(), toSources(result.documents()));
    }

    /** 설정의 기관 목록으로 범위 밖 안내 문구를 만든다 (기관이 늘어도 문구를 고칠 필요 없음). */
    static String buildOutOfScopeAnswer(List<Organization> orgs) {
        Map<String, List<String>> byType = new LinkedHashMap<>();
        for (Organization o : orgs) byType.computeIfAbsent(o.type(), k -> new ArrayList<>()).add(o.name());
        String scope = byType.entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + String.join(", ", e.getValue()))
                .collect(Collectors.joining("\n"));
        return "이 질문은 제가 가진 자료 범위 밖이라 정확히 답하기 어려워요.\n"
                + "지금은 아래 기관의 예약·취소·환불, 수하물, 체크인 관련 질문에 답할 수 있어요.\n" + scope;
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
