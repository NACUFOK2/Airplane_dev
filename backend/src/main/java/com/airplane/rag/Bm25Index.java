package com.airplane.rag;

import com.airplane.domain.KnowledgeChunk;
import com.airplane.domain.RetrievedChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** 인메모리 BM25 검색 인덱스. 제목 텍스트에 가중치를 주어 색인한다. */
public class Bm25Index {

    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final int TITLE_BOOST = 3;

    private final List<KnowledgeChunk> docs;
    private final List<Map<String, Integer>> termFreqs = new ArrayList<>();
    private final int[] docLengths;
    private final Map<String, Integer> docFreq = new HashMap<>();
    private final double avgDocLength;

    public Bm25Index(List<KnowledgeChunk> docs) {
        this.docs = List.copyOf(docs);
        this.docLengths = new int[docs.size()];
        long total = 0;
        for (int i = 0; i < docs.size(); i++) {
            KnowledgeChunk c = docs.get(i);
            Map<String, Integer> tf = new HashMap<>();
            List<String> titleTerms = TextAnalyzer.analyze(c.category() + " " + c.title());
            for (int b = 0; b < TITLE_BOOST; b++) titleTerms.forEach(t -> tf.merge(t, 1, Integer::sum));
            TextAnalyzer.analyze(c.content()).forEach(t -> tf.merge(t, 1, Integer::sum));
            termFreqs.add(tf);
            int len = tf.values().stream().mapToInt(Integer::intValue).sum();
            docLengths[i] = len;
            total += len;
            tf.keySet().forEach(t -> docFreq.merge(t, 1, Integer::sum));
        }
        this.avgDocLength = docs.isEmpty() ? 1 : (double) total / docs.size();
    }

    public List<RetrievedChunk> search(String query, int topK, Predicate<KnowledgeChunk> filter) {
        List<String> queryTerms = TextAnalyzer.analyze(query);
        int n = docs.size();
        List<RetrievedChunk> scored = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            KnowledgeChunk doc = docs.get(i);
            if (!filter.test(doc)) continue;
            Map<String, Integer> tf = termFreqs.get(i);
            double score = 0;
            for (String q : queryTerms) {
                Integer f = tf.get(q);
                if (f == null) continue;
                int df = docFreq.getOrDefault(q, 0);
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                score += idf * (f * (K1 + 1)) / (f + K1 * (1 - B + B * docLengths[i] / avgDocLength));
            }
            if (score > 0) scored.add(new RetrievedChunk(doc, score));
        }
        scored.sort(Comparator.comparingDouble(RetrievedChunk::score).reversed());
        return scored.subList(0, Math.min(topK, scored.size()));
    }
}
