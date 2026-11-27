package com.airplane.dto;

import java.util.List;

public record ChatResponse(String answer, String mode, List<SourceResponse> sources) {}
