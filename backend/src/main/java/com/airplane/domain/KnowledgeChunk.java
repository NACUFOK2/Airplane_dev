package com.airplane.domain;

/** 항공사 규정 문서를 '## 섹션' 단위로 자른 검색 단위. */
public record KnowledgeChunk(String id, String airlineCode, String category, String title, String content) {}
