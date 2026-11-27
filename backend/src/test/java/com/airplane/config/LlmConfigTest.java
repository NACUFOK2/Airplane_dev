package com.airplane.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.llm.ExtractiveLlmClient;
import com.airplane.llm.LlmClient;
import com.airplane.llm.OpenAiLlmClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class LlmConfigTest {

    private static RagProperties props(String provider) {
        return new RagProperties(null, List.of(), null, null,
                new RagProperties.Llm(provider, "claude-opus-5-5", 2000, 3,
                        new RagProperties.OpenAi("gpt-test", "https://api.openai.test/v1", 4000, "low", 5000)),
                null, List.of());
    }

    private static LlmClient select(String provider, Map<String, String> env) {
        return LlmConfig.select(props(provider), env::get, RestClient.builder());
    }

    @Test
    void autoUsesOpenAiWhenOnlyOpenAiKeyExists() {
        assertThat(select("auto", Map.of("OPENAI_API_KEY", "x"))).isInstanceOf(OpenAiLlmClient.class);
    }

    @Test
    void noKeyMeansExtractive() {
        assertThat(select("auto", Map.of())).isInstanceOf(ExtractiveLlmClient.class);
        assertThat(select("openai", Map.of())).isInstanceOf(ExtractiveLlmClient.class);
        assertThat(select("auto", Map.of("OPENAI_API_KEY", "  "))).isInstanceOf(ExtractiveLlmClient.class);
    }

    @Test
    void providerSettingIsRespected() {
        assertThat(select("extractive", Map.of("OPENAI_API_KEY", "x"))).isInstanceOf(ExtractiveLlmClient.class);
        assertThat(select("claude", Map.of("OPENAI_API_KEY", "x"))).isInstanceOf(ExtractiveLlmClient.class);
        assertThat(select("openai", Map.of("OPENAI_API_KEY", "x"))).isInstanceOf(OpenAiLlmClient.class);
    }
}
