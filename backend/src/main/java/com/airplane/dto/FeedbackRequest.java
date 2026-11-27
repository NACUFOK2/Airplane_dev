package com.airplane.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FeedbackRequest(
        @NotBlank @Pattern(regexp = "up|down", message = "rating 은 up 또는 down 이어야 합니다.") String rating,
        @NotBlank @Size(max = 1000) String question,
        @Pattern(regexp = "^[a-z]{2,20}$|^ALL$") String orgCode,
        @Pattern(regexp = "claude|extractive|out_of_scope") String mode,
        @Size(max = 10) List<@Pattern(regexp = "^\\d{3,5}$") String> sourceIds) {}
