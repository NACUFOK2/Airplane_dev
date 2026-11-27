package com.airplane.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.domain.KnowledgeChunk;
import com.airplane.domain.RetrievedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class Bm25IndexTest {

    private final Bm25Index index = new Bm25Index(List.of(
            new KnowledgeChunk("A-1", "A", "수하물", "위탁 수하물 무료 허용량", "일반석은 23kg 1개 무료입니다."),
            new KnowledgeChunk("A-2", "A", "체크인", "온라인 체크인", "출발 48시간 전부터 체크인 가능합니다."),
            new KnowledgeChunk("B-1", "B", "수하물", "위탁 수하물 무료 허용량", "15kg까지 무료입니다.")));

    @Test
    void findsRelevantChunkForKoreanQuery() {
        List<RetrievedChunk> result = index.search("수하물은 몇 kg까지 무료인가요?", 3, c -> true);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).chunk().category()).isEqualTo("수하물");
    }

    @Test
    void appliesAirlineFilter() {
        List<RetrievedChunk> result = index.search("위탁 수하물", 3, c -> c.airlineCode().equals("B"));
        assertThat(result).extracting(r -> r.chunk().id()).containsExactly("B-1");
    }

    @Test
    void handlesJosaVariationsViaBigrams() {
        List<RetrievedChunk> result = index.search("체크인은 언제부터 돼요", 1, c -> true);
        assertThat(result.get(0).chunk().id()).isEqualTo("A-2");
    }
}
