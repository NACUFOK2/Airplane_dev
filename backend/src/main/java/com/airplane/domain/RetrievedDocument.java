package com.airplane.domain;

/**
 * 융합 검색 결과.
 * @param fusedScore RRF 점수 (정렬 기준)
 * @param denseScore 질의-문서 코사인 유사도 (dense 비활성 시 NaN)
 * @param denseRank  dense 순위 (1부터, 없으면 -1)
 * @param bm25Rank   BM25 순위 (1부터, 없으면 -1)
 */
public record RetrievedDocument(QaDocument doc, double fusedScore, double denseScore, int denseRank, int bm25Rank) {}
