package com.airplane.domain;

/** 답변 대상 기관 (여행사 / 항공사). */
public record Organization(String code, String name, String type, String brandColor) {}
