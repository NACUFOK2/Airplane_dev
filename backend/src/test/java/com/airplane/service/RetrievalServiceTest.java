package com.airplane.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.client.EmbeddingClient;
import com.airplane.config.RagProperties;
import com.airplane.domain.QaDocument;
import com.airplane.domain.RetrievalResult;
import com.airplane.repository.OrganizationRepository;
import com.airplane.repository.QaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RetrievalServiceTest {

    private final RagProperties props = new RagProperties(
            "data/qa_embeddings.jsonl", List.of(),
            new RagProperties.Retrieval(4, 60, 50, 0.50, true),
            new RagProperties.Embedding("http://unused", 1000, 384),
            new RagProperties.Llm("extractive", "m", 100, 3),
            List.of(new RagProperties.OrganizationDef("koreanair", "대한항공", "항공사", "#000"),
                    new RagProperties.OrganizationDef("jejuair", "제주항공", "항공사", "#000")));
    private final QaRepository repo = new QaRepository(props, new ObjectMapper());
    private final OrganizationRepository orgs = new OrganizationRepository(props);

    private RetrievalService service(EmbeddingClient client) {
        return new RetrievalService(repo, client, orgs, props);
    }

    private QaDocument byId(String id) {
        return repo.findAll().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void hybridRanksDocWhoseVectorMatchesQueryFirst() {
        QaDocument target = byId("052"); // 대한항공 보조배터리
        RetrievalResult r = service(q -> Optional.of(target.embedding())).retrieve("보조배터리 몇 개", null);
        assertThat(r.mode()).isEqualTo("hybrid");
        assertThat(r.documents().get(0).doc().id()).isEqualTo("052");
        assertThat(r.outOfScope()).isFalse();
        assertThat(r.documents()).hasSize(4);
    }

    @Test
    void autoDetectsOrgNameInQuery() {
        RetrievalResult r = service(q -> Optional.empty()).retrieve("제주항공 반려동물 몇 kg", null);
        assertThat(r.orgFilter()).isEqualTo("제주항공");
        assertThat(r.documents()).allMatch(d -> d.doc().org().equals("제주항공"));
    }

    @Test
    void fallsBackToBm25WhenEmbeddingUnavailable() {
        RetrievalResult r = service(q -> Optional.empty()).retrieve("대한항공 온라인 체크인 언제", null);
        assertThat(r.mode()).isEqualTo("bm25");
        assertThat(r.documents().get(0).doc().id()).isEqualTo("044");
    }

    @Test
    void lowDenseSimilarityIsOutOfScope() {
        float[] orthogonal = new float[384]; // 모든 문서와 내적 0
        orthogonal[0] = 1f;
        float[] q = orthogonal;
        RetrievalResult r = service(x -> Optional.of(q)).retrieve("오늘 날씨", null);
        assertThat(r.topDenseScore()).isLessThan(0.5);
        assertThat(r.outOfScope()).isTrue();
    }
}
