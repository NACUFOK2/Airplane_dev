package com.airplane.rag;

import com.airplane.domain.QaDocument;
import java.util.List;

/** 사전 계산된 L2 정규화 벡터에 대한 브루트포스 코사인(=내적) 검색. 83건 규모에선 ANN 불필요. */
public class DenseIndex {

    private final float[][] matrix;

    public DenseIndex(List<QaDocument> docs) {
        this.matrix = docs.stream().map(QaDocument::embedding).toArray(float[][]::new);
    }

    public double[] scores(float[] query) {
        double[] s = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            float[] v = matrix[i];
            double dot = 0;
            for (int j = 0; j < v.length; j++) dot += v[j] * query[j];
            s[i] = dot;
        }
        return s;
    }
}
