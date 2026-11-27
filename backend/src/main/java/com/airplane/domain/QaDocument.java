package com.airplane.domain;

/**
 * 검색 단위: Q&A 한 쌍 + 미리 계산된 384차원 L2 정규화 문서 벡터.
 * @param official 출처가 해당 기관의 공식 도메인인지 (블로그·정리글이면 false)
 */
public record QaDocument(
        String id, String orgType, String org, String question, String answer, String source, boolean official, float[] embedding) {}
