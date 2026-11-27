package com.airplane.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.domain.QaDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class Bm25IndexTest {

    private static QaDocument doc(String id, String org, String q, String a) {
        return new QaDocument(id, "항공사", org, q, a, "", true, new float[0]);
    }

    private final List<QaDocument> docs = List.of(
            doc("1", "대한항공", "무료 위탁 수하물 허용량은 얼마인가요?", "일반석은 23kg 1개 무료입니다."),
            doc("2", "대한항공", "온라인 체크인은 언제 할 수 있나요?", "출발 48시간 전부터 체크인 가능합니다."),
            doc("3", "제주항공", "무료 위탁 수하물은 얼마인가요?", "스탠다드 15kg 입니다."));
    private final Bm25Index index = new Bm25Index(docs);

    private int top(String q) {
        boolean[] all = {true, true, true};
        return RankFusion.rank(index.scores(q), all, 3, true)[0];
    }

    @Test
    void ranksByKoreanBigramOverlap() {
        assertThat(top("체크인은 언제부터 돼요")).isEqualTo(1);
    }

    @Test
    void orgNameInQuestionFieldBoostsMatchingOrg() {
        assertThat(top("제주항공 위탁 수하물")).isEqualTo(2);
    }

    @Test
    void unrelatedQueryScoresZero() {
        double[] s = index.scores("비트코인");
        assertThat(s).containsOnly(0.0);
    }
}
