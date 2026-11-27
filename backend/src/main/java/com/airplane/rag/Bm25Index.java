package com.airplane.rag;

import com.airplane.domain.QaDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 인메모리 BM25 (k1=1.2, b=0.75 — 짧은 FAQ 문서에 표준적으로 쓰이는 Lucene 기본값).
 * 기관명+질문 필드는 3배 가중해 '질문-질문' 어휘 일치를 우선한다.
 */
public class Bm25Index {

    static final double K1 = 1.2;
    static final double B = 0.75;
    static final int QUESTION_BOOST = 3;

    private final List<QaDocument> docs;
    private final List<Map<String, Integer>> termFreqs = new ArrayList<>();
    private final int[] docLengths;
    private final Map<String, Integer> docFreq = new HashMap<>();
    private final double avgDocLength;

    public Bm25Index(List<QaDocument> docs) {
        this.docs = List.copyOf(docs);
        this.docLengths = new int[docs.size()];
        long total = 0;
        for (int i = 0; i < docs.size(); i++) {
            QaDocument d = docs.get(i);
            Map<String, Integer> tf = new HashMap<>();
            List<String> qTerms = TextAnalyzer.analyze(d.org() + " " + d.question());
            for (int b = 0; b < QUESTION_BOOST; b++) qTerms.forEach(t -> tf.merge(t, 1, Integer::sum));
            TextAnalyzer.analyze(d.answer()).forEach(t -> tf.merge(t, 1, Integer::sum));
            termFreqs.add(tf);
            int len = tf.values().stream().mapToInt(Integer::intValue).sum();
            docLengths[i] = len;
            total += len;
            tf.keySet().forEach(t -> docFreq.merge(t, 1, Integer::sum));
        }
        this.avgDocLength = docs.isEmpty() ? 1 : (double) total / docs.size();
    }

    /** 문서 순서(docs 인덱스)와 같은 길이의 점수 배열을 반환한다. */
    public double[] scores(String query) {
        int n = docs.size();
        double[] s = new double[n];
        for (String q : TextAnalyzer.analyze(query)) {
            int df = docFreq.getOrDefault(q, 0);
            if (df == 0) continue;
            double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
            for (int i = 0; i < n; i++) {
                Integer f = termFreqs.get(i).get(q);
                if (f == null) continue;
                s[i] += idf * (f * (K1 + 1)) / (f + K1 * (1 - B + B * docLengths[i] / avgDocLength));
            }
        }
        return s;
    }
}
