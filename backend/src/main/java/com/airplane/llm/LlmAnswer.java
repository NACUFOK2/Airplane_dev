package com.airplane.llm;

/**
 * 생성 결과와 실제로 답을 만든 방식.
 * @param mode claude (Claude 가 답함) | extractive (추출형. API 키가 없거나 Claude 호출이 실패해 대체된 경우 포함)
 */
public record LlmAnswer(String text, String mode) {

    public static final String CLAUDE = "claude";
    public static final String EXTRACTIVE = "extractive";
}
