package com.airplane.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 rag.* 설정 바인딩. 시크릿 값은 포함하지 않는다. 근거: docs/RAG_PARAMETERS.md */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        String dataFile,
        List<String> allowedOrigins,
        Retrieval retrieval,
        Embedding embedding,
        Llm llm,
        List<OrganizationDef> organizations) {

    public record Retrieval(int topK, int rrfK, int candidatePool, double denseMinScore, boolean autoOrgFilter) {}

    public record Embedding(String baseUrl, int timeoutMs, int dimension) {}

    /** provider: auto(키 있으면 claude) | claude | extractive */
    public record Llm(String provider, String model, long maxTokens, int historyTurns) {}

    public record OrganizationDef(String code, String name, String type, String brandColor) {}
}
