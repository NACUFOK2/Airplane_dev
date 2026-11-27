package com.airplane.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ChatRequest(
        @NotBlank(message = "질문을 입력해 주세요.") @Size(max = 1000, message = "질문은 1000자 이내로 입력해 주세요.") String question,
        @Pattern(regexp = "^[A-Z0-9]{2,3}$|^ALL$", message = "올바른 항공사 코드가 아닙니다.") String airlineCode,
        @Valid @Size(max = 20) List<HistoryMessage> history) {

    public record HistoryMessage(
            @Pattern(regexp = "user|assistant") String role,
            @Size(max = 4000) String content) {}
}
