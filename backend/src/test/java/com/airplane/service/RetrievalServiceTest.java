package com.airplane.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.client.EmbeddingClient;
import com.airplane.config.RagProperties;
import com.airplane.domain.Organization;
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
            new RagProperties.Retrieval(4, 60, 200, 0.60, true, true, 8, List.of("아시아나", "KTX", "에어비앤비")),
            new RagProperties.Embedding("http://unused", 1000, 384),
            new RagProperties.Llm("extractive", "m", 100, 3),
            new RagProperties.Feedback(false, "unused"),
            List.of(new RagProperties.OrganizationDef("koreanair", "대한항공", "항공사", "#000", List.of("koreanair.com"), List.of()),
                    new RagProperties.OrganizationDef("jejuair", "제주항공", "항공사", "#000", List.of("jejuair.net"), List.of()),
                    new RagProperties.OrganizationDef("delta", "델타항공", "항공사", "#000", List.of("delta.com"), List.of("델타")),
                    new RagProperties.OrganizationDef("ana", "ANA(전일본공수)", "항공사", "#000", List.of("ana.co.jp"), List.of("ANA", "전일본공수"))));
    private final OrganizationRepository orgs = new OrganizationRepository(props);
    private final QaRepository repo = new QaRepository(props, new ObjectMapper(), orgs);

    private RetrievalService service(EmbeddingClient client) {
        return new RetrievalService(repo, client, orgs, props);
    }

    private QaDocument byId(String id) {
        return repo.findAll().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void hybridRanksDocWhoseVectorMatchesQueryFirst() {
        QaDocument target = byId("항공-014"); // 대한항공 보조배터리
        RetrievalResult r = service(q -> Optional.of(target.embeddings()[1])).retrieve("보조배터리 몇 개", null);
        assertThat(r.mode()).isEqualTo("hybrid");
        assertThat(r.documents().get(0).doc().id()).isEqualTo("항공-014");
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
        assertThat(r.documents().get(0).doc().id()).isEqualTo("항공-006");
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

    @Test
    void followupInheritsOrgFromPreviousQuestion() {
        RetrievalResult r = service(q -> Optional.empty()).retrieve("유아는 좌석 줘요?", null, "제주항공 반려동물 탈 수 있어요?");
        assertThat(r.orgFilter()).isEqualTo("제주항공");
        assertThat(r.documents()).allMatch(d -> d.doc().org().equals("제주항공"));
    }

    @Test
    void orgNamedInFollowupOverridesPrevious() {
        RetrievalResult r = service(q -> Optional.empty()).retrieve("대한항공은요?", null, "제주항공 보조배터리 몇 개?");
        assertThat(r.orgFilter()).isEqualTo("대한항공");
    }

    @Test
    void shortFollowupIsCombinedWithPreviousQuestion() {
        RetrievalService s = service(q -> Optional.empty());
        assertThat(s.isShortFollowup("기내는요?")).isTrue();
        assertThat(s.isShortFollowup("카운터는 언제 마감해요?")).isFalse();
        // '기내는요?' 단독으론 주제가 없지만 직전 질문과 합치면 기내 수하물(항공-011)을 찾는다
        RetrievalResult r = s.retrieve("기내는요?", null, "대한항공 무료 위탁 수하물 얼마예요?");
        assertThat(r.documents()).extracting(d -> d.doc().id()).contains("항공-011");
    }

    @Test
    void selectedOrgChipWinsOverPreviousQuestion() {
        Organization ke = orgs.findByCode("koreanair").orElseThrow();
        RetrievalResult r = service(q -> Optional.empty()).retrieve("유아는?", ke, "제주항공 반려동물");
        assertThat(r.orgFilter()).isEqualTo("대한항공");
    }

    @Test
    void detectsOrgByAlias() {
        assertThat(service(q -> Optional.empty()).retrieve("델타 베이직 이코노미 환불", null).orgFilter()).isEqualTo("델타항공");
        assertThat(service(q -> Optional.empty()).retrieve("전일본공수 수하물", null).orgFilter()).isEqualTo("ANA(전일본공수)");
    }

    @Test
    void returnsEachOriginalOnlyOnce() {
        RetrievalResult r = service(q -> Optional.of(byId("항공-014").embeddings()[0])).retrieve("보조배터리", null);
        assertThat(r.documents()).extracting(d -> d.doc().id()).doesNotHaveDuplicates();
    }

    @Test
    void unsupportedOrgWithoutSupportedOrgIsOutOfScope() {
        RetrievalResult r = service(q -> Optional.empty()).retrieve("아시아나 마일리지 어떻게 돼요?", null);
        assertThat(r.outOfScope()).isTrue();
        assertThat(r.unsupportedOrg()).isEqualTo("아시아나");
        assertThat(service(q -> Optional.empty()).retrieve("ktx 취소 수수료", null).unsupportedOrg()).isEqualTo("KTX");
    }

    @Test
    void supportedOrgMentionWinsOverUnsupported() {
        // 대한항공에서 산 표 질문에 다른 항공사 이름이 섞여도 대한항공 자료로 답한다
        RetrievalResult r = service(q -> Optional.empty()).retrieve("대한항공 앱에서 산 아시아나 공동운항편 체크인", null);
        assertThat(r.unsupportedOrg()).isNull();
        assertThat(r.orgFilter()).isEqualTo("대한항공");
    }
}
