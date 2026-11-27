package com.airplane.service;

import com.airplane.config.RagProperties;
import com.airplane.domain.KnowledgeChunk;
import com.airplane.domain.RetrievedChunk;
import com.airplane.rag.Bm25Index;
import com.airplane.repository.KnowledgeRepository;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {

    private final Bm25Index index;
    private final RagProperties properties;

    public RetrievalService(KnowledgeRepository knowledgeRepository, RagProperties properties) {
        this.index = new Bm25Index(knowledgeRepository.findAll());
        this.properties = properties;
    }

    /** airlineCode 가 null 또는 ALL 이면 전체 항공사에서 검색한다. */
    public List<RetrievedChunk> retrieve(String query, String airlineCode) {
        Predicate<KnowledgeChunk> filter = (airlineCode == null || "ALL".equals(airlineCode))
                ? c -> true
                : c -> c.airlineCode().equals(airlineCode);
        return index.search(query, properties.topK(), filter).stream()
                .filter(r -> r.score() >= properties.minScore())
                .toList();
    }
}
