package com.airplane.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 rag.* 설정 바인딩. 시크릿 값은 포함하지 않는다. */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        int topK,
        double minScore,
        Llm llm,
        List<AirlineDef> airlines,
        List<String> allowedOrigins) {

    /** provider: auto(키 있으면 claude) | claude | extractive */
    public record Llm(String provider, String model, long maxTokens, int historyTurns) {}

    public record AirlineDef(String code, String name, String nameEn, String brandColor, String customerCenter, String file) {}
}
