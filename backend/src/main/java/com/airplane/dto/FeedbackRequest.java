package com.airplane.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FeedbackRequest(
        @NotBlank(message = "평가 값이 없어요.") @Pattern(regexp = "up|down", message = "평가는 좋아요(up) 또는 별로예요(down)만 보낼 수 있어요.") String rating,
        @NotBlank(message = "질문이 없어요.") @Size(max = 1000, message = "질문은 1000자 이내여야 해요.") String question,
        @Pattern(regexp = "^[a-z]{2,20}$|^ALL$", message = "올바른 기관 코드가 아니에요.") String orgCode,
        @Pattern(regexp = "claude|openai|extractive|out_of_scope", message = "올바른 답변 모드가 아니에요.") String mode,
        @Size(max = 10, message = "출처는 10개까지 보낼 수 있어요.")
        List<@Pattern(regexp = "^[가-힣]{2}-\\d{3}$", message = "올바른 출처 id 가 아니에요.") String> sourceIds) {}
