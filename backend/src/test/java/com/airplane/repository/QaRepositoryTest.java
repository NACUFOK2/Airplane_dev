package com.airplane.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.config.RagProperties;
import com.airplane.domain.QaDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class QaRepositoryTest {

    private static RagProperties.OrganizationDef org(String name, String type, String... domains) {
        return new RagProperties.OrganizationDef(name, name, type, "#000", List.of(domains), List.of());
    }

    private final OrganizationRepository orgs = new OrganizationRepository(new RagProperties(
            "data/qa_embeddings.jsonl", List.of(), null, null, null, null, List.of(
            org("하나투어", "여행사", "hanatour.com"), org("모두투어", "여행사", "modetour.co.kr"),
            org("노랑풍선", "여행사", "ybtour.co.kr"), org("참좋은여행", "여행사", "verygoodtour.com"),
            org("대한항공", "항공사", "koreanair.com"), org("제주항공", "항공사", "jejuair.net"),
            org("에어로케이", "항공사", "aerok.com"), org("델타항공", "항공사", "delta.com"),
            org("유나이티드항공", "항공사", "united.com"), org("에미레이트항공", "항공사", "emirates.com"),
            org("싱가포르항공", "항공사", "singaporeair.com"), org("캐세이퍼시픽", "항공사", "cathaypacific.com"),
            org("루프트한자", "항공사", "lufthansa.com"), org("ANA(전일본공수)", "항공사", "ana.co.jp"),
            org("아고다", "숙박업체", "agoda.com"), org("호텔스컴바인", "숙박업체", "hotelscombined.co.kr", "hotelscombined.com"),
            org("트립닷컴", "숙박업체", "trip.com"), org("부킹닷컴", "숙박업체", "booking.com"),
            org("야놀자", "숙박업체", "yanolja.com"), org("여기어때", "숙박업체", "yeogi.com", "goodchoice.kr"))));

    private final List<QaDocument> docs = QaRepository.load("data/qa_embeddings.jsonl", 384, new ObjectMapper(), orgs);

    @Test
    void groupsVariantsIntoOriginalQuestions() {
        assertThat(docs).hasSize(178);
        assertThat(docs).allMatch(d -> d.variants().size() == 4 && d.embeddings().length == 4);
        Map<String, Long> byType = docs.stream().collect(Collectors.groupingBy(QaDocument::orgType, Collectors.counting()));
        assertThat(byType).containsEntry("항공사", 92L).containsEntry("숙박업체", 48L).containsEntry("여행사", 38L);
        QaDocument d = docs.stream().filter(x -> x.id().equals("항공-014")).findFirst().orElseThrow();
        assertThat(d.question()).isEqualTo("보조배터리는 어떻게 가져가나요?");
        assertThat(d.variants().get(0)).isEqualTo(d.question());
    }

    @Test
    void vectorsAreNormalized() {
        for (QaDocument d : docs) {
            for (float[] v : d.embeddings()) {
                double norm = 0;
                for (float x : v) norm += x * x;
                assertThat(Math.sqrt(norm)).isBetween(0.999, 1.001);
            }
        }
    }

    @Test
    void marksOfficialSourcesByDomain() {
        // docs/DATA_ANALYSIS.md: 항공사 73/92, 숙박업체 23/48, 여행사 15/38 이 공식 출처
        Map<String, Long> official = docs.stream().filter(QaDocument::official)
                .collect(Collectors.groupingBy(QaDocument::orgType, Collectors.counting()));
        assertThat(official).containsEntry("항공사", 73L).containsEntry("숙박업체", 23L).containsEntry("여행사", 15L);
        assertThat(docs.stream().filter(d -> d.id().equals("여행-012")).findFirst().orElseThrow().official()).isTrue();
        assertThat(docs.stream().filter(d -> d.id().equals("여행-002")).findFirst().orElseThrow().official()).isFalse();
    }
}
