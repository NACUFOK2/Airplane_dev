package com.airplane.dto;

import java.util.List;

/**
 * @param mode      claude | extractive | out_of_scope
 * @param retrieval hybrid | bm25
 */
public record ChatResponse(String answer, String mode, String retrieval, List<SourceResponse> sources) {}
