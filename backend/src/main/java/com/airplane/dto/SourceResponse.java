package com.airplane.dto;

/** @param official 출처가 기관 공식 홈페이지인지 (false 면 블로그·정리글 등 참고 출처) */
public record SourceResponse(int index, String id, String org, String orgType, String question, String snippet, String url, boolean official, double score) {}
