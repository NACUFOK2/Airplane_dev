package com.airplane.rag;

import com.airplane.domain.QaDocument;
import java.util.List;

/**
 * 사전 계산된 L2 정규화 벡터에 대한 브루트포스 코사인(=내적) 검색.
 * 원문마다 질문 변형 벡터가 여러 개이며, 원문 점수는 그중 최댓값이다.
 * (평가: 최댓값 Hit@1 0.941 > 평균 0.897 > 원문 벡터만 0.824 — docs/RAG_PARAMETERS.md)
 */
public class DenseIndex {

    private final float[][][] vectors;

    public DenseIndex(List<QaDocument> docs) {
        this.vectors = docs.stream().map(QaDocument::embeddings).toArray(float[][][]::new);
    }

    public double[] scores(float[] query) {
        double[] s = new double[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            double best = Double.NEGATIVE_INFINITY;
            for (float[] v : vectors[i]) {
                double dot = 0;
                for (int j = 0; j < v.length; j++) dot += v[j] * query[j];
                best = Math.max(best, dot);
            }
            s[i] = vectors[i].length == 0 ? 0 : best;
        }
        return s;
    }
}
