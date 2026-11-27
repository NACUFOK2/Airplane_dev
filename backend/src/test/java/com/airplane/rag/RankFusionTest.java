package com.airplane.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RankFusionTest {

    @Test
    void docRankedHighByBothWins() {
        int[] dense = {0, 1, 2};
        int[] bm25 = {1, 0, 2};
        double[] fused = RankFusion.rrf(List.of(dense, bm25), 3, 60);
        // 0: 1/61+1/62, 1: 1/62+1/61 → 동률, 2: 꼴찌
        assertThat(fused[0]).isEqualTo(fused[1]);
        assertThat(fused[2]).isLessThan(fused[0]);
    }

    @Test
    void docMissingFromOneListStillGetsCredit() {
        double[] fused = RankFusion.rrf(List.of(new int[]{2}, new int[]{}), 3, 60);
        assertThat(fused[2]).isEqualTo(1.0 / 61);
    }

    @Test
    void rankRespectsFilterAndDropsZero() {
        double[] scores = {0.9, 0.0, 0.5};
        assertThat(RankFusion.rank(scores, new boolean[]{false, true, true}, 5, true)).containsExactly(2);
    }
}
