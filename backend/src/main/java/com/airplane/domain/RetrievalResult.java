package com.airplane.domain;

import java.util.List;

/**
 * @param mode       hybrid | bm25 (임베딩 서비스 장애 시)
 * @param outOfScope dense top-1 점수가 임계값 미만이라 데이터 범위 밖으로 판정
 * @param orgFilter  적용된 기관 필터 (없으면 null)
 */
public record RetrievalResult(List<RetrievedDocument> documents, String mode, boolean outOfScope, String orgFilter, double topDenseScore) {}
