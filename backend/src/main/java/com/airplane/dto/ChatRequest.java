package com.airplane.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ChatRequest(
        @NotBlank(message = "질문을 입력해 주세요.") @Size(max = 1000, message = "질문은 1000자 이내로 입력해 주세요.") String question,
        @Pattern(regexp = "^[a-z]{2,20}$|^ALL$", message = "올바른 기관 코드가 아니에요.") String orgCode,
        @Valid @Size(max = 20, message = "대화 기록은 20개까지 보낼 수 있어요.") List<HistoryMessage> history) {

    public record HistoryMessage(
            @Pattern(regexp = "user|assistant", message = "대화 역할 값이 올바르지 않아요.") String role,
            @Size(max = 4000, message = "대화 기록 한 개는 4000자 이내여야 해요.") String content) {}
}
