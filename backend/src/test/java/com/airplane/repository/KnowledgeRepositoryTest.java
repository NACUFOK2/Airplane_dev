package com.airplane.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.domain.KnowledgeChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeRepositoryTest {

    @Test
    void parsesSectionsWithCategory() {
        String md = """
                # 테스트항공
                > 샘플
                ## [수하물] 위탁 수하물
                15kg 무료
                ## 일반 안내
                내용
                """;
        List<KnowledgeChunk> chunks = KnowledgeRepository.parse("XX", md);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).category()).isEqualTo("수하물");
        assertThat(chunks.get(0).title()).isEqualTo("위탁 수하물");
        assertThat(chunks.get(0).content()).isEqualTo("15kg 무료");
        assertThat(chunks.get(1).category()).isEqualTo("일반");
    }
}
