package com.airplane.domain;

/** 검색 단위: Q&A 한 쌍 + 미리 계산된 384차원 L2 정규화 문서 벡터. */
public record QaDocument(
        String id, String orgType, String org, String question, String answer, String source, float[] embedding) {}
