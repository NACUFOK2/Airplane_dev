package com.airplane.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.config.RagProperties;
import com.airplane.domain.QaDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class QaRepositoryTest {

    private static RagProperties.OrganizationDef org(String code, String name, String type, String domain) {
        return new RagProperties.OrganizationDef(code, name, type, "#000", List.of(domain));
    }

    private final OrganizationRepository orgs = new OrganizationRepository(new RagProperties(
            "data/qa_embeddings.jsonl", List.of(), null, null, null,
            List.of(org("hanatour", "하나투어", "여행사", "hanatour.com"), org("modetour", "모두투어", "여행사", "modetour.co.kr"),
                    org("ybtour", "노랑풍선", "여행사", "ybtour.co.kr"), org("verygoodtour", "참좋은여행", "여행사", "verygoodtour.com"),
                    org("koreanair", "대한항공", "항공사", "koreanair.com"), org("jejuair", "제주항공", "항공사", "jejuair.net"),
                    org("aerok", "에어로케이", "항공사", "aerok.com"))));

    private final List<QaDocument> docs = QaRepository.load("data/qa_embeddings.jsonl", 384, new ObjectMapper(), orgs);

    @Test
    void loadsAll83QaWithNormalizedVectors() {
        assertThat(docs).hasSize(83);
        assertThat(docs).extracting(QaDocument::org).contains("하나투어", "대한항공", "에어로케이");
        for (QaDocument d : docs) {
            double norm = 0;
            for (float x : d.embedding()) norm += x * x;
            assertThat(Math.sqrt(norm)).isBetween(0.999, 1.001);
        }
    }

    @Test
    void marksOfficialSourcesByDomain() {
        // 데이터 분석(docs/DATA_ANALYSIS.md): 여행사 15/38, 항공사 45/45 가 공식 출처
        assertThat(docs.stream().filter(d -> d.orgType().equals("여행사") && d.official()).count()).isEqualTo(15);
        assertThat(docs.stream().filter(d -> d.orgType().equals("항공사") && d.official()).count()).isEqualTo(45);
        // 하위 도메인(samsungcardair.modetour.co.kr)도 공식으로 인정
        assertThat(docs.stream().filter(d -> d.id().equals("012")).findFirst().orElseThrow().official()).isTrue();
        // 블로그 출처
        assertThat(docs.stream().filter(d -> d.id().equals("002")).findFirst().orElseThrow().official()).isFalse();
    }
}
