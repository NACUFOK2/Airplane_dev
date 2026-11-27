package com.airplane.rag;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Reciprocal Rank Fusion: score(d) = Σ 1 / (k + rank_i(d)).
 * 점수 척도가 다른 BM25(0~수십)와 코사인(0~1)을 정규화 없이 순위만으로 합친다.
 */
public final class RankFusion {

    private RankFusion() {}

    /** 점수 내림차순 인덱스 목록 (allowed=false 인 문서와 점수 0 이하는 제외). */
    public static int[] rank(double[] scores, boolean[] allowed, int limit, boolean dropNonPositive) {
        return IntStream.range(0, scores.length)
                .filter(i -> allowed[i] && (!dropNonPositive || scores[i] > 0))
                .boxed()
                .sorted(Comparator.comparingDouble((Integer i) -> scores[i]).reversed())
                .limit(limit)
                .mapToInt(Integer::intValue)
                .toArray();
    }

    public static double[] rrf(List<int[]> rankings, int size, int k) {
        double[] fused = new double[size];
        for (int[] ranking : rankings) {
            for (int r = 0; r < ranking.length; r++) {
                fused[ranking[r]] += 1.0 / (k + r + 1);
            }
        }
        return fused;
    }

    /** rank 배열에서 문서 i 의 1-based 순위 (없으면 -1) */
    public static int positionOf(int[] ranking, int doc) {
        for (int r = 0; r < ranking.length; r++) if (ranking[r] == doc) return r + 1;
        return -1;
    }

    static boolean[] all(int size) {
        boolean[] b = new boolean[size];
        Arrays.fill(b, true);
        return b;
    }
}
