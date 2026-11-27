package com.airplane.service;

import com.airplane.config.RagProperties;
import com.airplane.domain.Airline;
import com.airplane.domain.ChatTurn;
import com.airplane.domain.RetrievedChunk;
import com.airplane.dto.ChatRequest;
import com.airplane.dto.ChatResponse;
import com.airplane.dto.SourceResponse;
import com.airplane.llm.LlmClient;
import com.airplane.repository.AirlineRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** RAG 파이프라인: 검색(Retrieve) → 문맥 구성(Augment) → 답변 생성(Generate). */
@Service
public class ChatService {

    private final RetrievalService retrievalService;
    private final AirlineService airlineService;
    private final AirlineRepository airlineRepository;
    private final LlmClient llmClient;
    private final RagProperties properties;

    public ChatService(RetrievalService retrievalService, AirlineService airlineService,
                       AirlineRepository airlineRepository, LlmClient llmClient, RagProperties properties) {
        this.retrievalService = retrievalService;
        this.airlineService = airlineService;
        this.airlineRepository = airlineRepository;
        this.llmClient = llmClient;
        this.properties = properties;
    }

    public ChatResponse ask(ChatRequest request) {
        String code = request.airlineCode() == null ? "ALL" : request.airlineCode();
        String airlineLabel = "ALL".equals(code) ? "전체 항공사" : airlineService.getByCode(code).name();

        List<RetrievedChunk> context = retrievalService.retrieve(request.question(), code);
        String answer = llmClient.generate(request.question(), context, toHistory(request), airlineLabel);

        List<SourceResponse> sources = new ArrayList<>();
        for (int i = 0; i < context.size(); i++) {
            RetrievedChunk r = context.get(i);
            String name = airlineRepository.findByCode(r.chunk().airlineCode()).map(Airline::name).orElse(r.chunk().airlineCode());
            sources.add(new SourceResponse(i + 1, r.chunk().airlineCode(), name, r.chunk().category(),
                    r.chunk().title(), snippet(r.chunk().content()), Math.round(r.score() * 100) / 100.0));
        }
        return new ChatResponse(answer, llmClient.mode(), sources);
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
        return flat.length() > 140 ? flat.substring(0, 140) + "…" : flat;
    }
}
