package com.airplane.config;

import com.airplane.llm.ClaudeLlmClient;
import com.airplane.llm.ExtractiveLlmClient;
import com.airplane.llm.LlmClient;
import com.airplane.llm.OpenAiLlmClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import java.util.Locale;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 답변 생성기 선택. API 키는 환경변수로만 읽으며 설정 파일·로그에 남기지 않는다.
 * <pre>
 * provider=auto      : ANTHROPIC_API_KEY → claude, 없으면 OPENAI_API_KEY → openai, 둘 다 없으면 extractive
 * provider=claude    : ANTHROPIC_API_KEY 필요 (없으면 extractive)
 * provider=openai    : OPENAI_API_KEY 필요 (없으면 extractive)
 * provider=extractive: 항상 추출형
 * </pre>
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public LlmClient llmClient(RagProperties properties, RestClient.Builder restClientBuilder) {
        return select(properties, System::getenv, restClientBuilder);
    }

    static LlmClient select(RagProperties properties, Function<String, String> env, RestClient.Builder restClientBuilder) {
        RagProperties.Llm llm = properties.llm();
        String provider = llm.provider() == null ? "auto" : llm.provider().toLowerCase(Locale.ROOT);
        boolean hasClaude = present(env.apply("ANTHROPIC_API_KEY"));
        boolean hasOpenAi = present(env.apply("OPENAI_API_KEY")) && llm.openai() != null;

        boolean useClaude = hasClaude && (provider.equals("auto") || provider.equals("claude"));
        boolean useOpenAi = !useClaude && hasOpenAi && (provider.equals("auto") || provider.equals("openai"));

        if (useClaude) {
            log.info("Claude 답변 모드 (model={})", llm.model());
            return new ClaudeLlmClient(AnthropicOkHttpClient.fromEnv(), llm);
        }
        if (useOpenAi) {
            log.info("OpenAI 답변 모드 (model={})", llm.openai().model());
            return new OpenAiLlmClient(restClientBuilder, env.apply("OPENAI_API_KEY").strip(), llm.openai());
        }
        log.warn("사용 가능한 API 키 없음 (provider={}) → 추출형(오프라인) 답변 모드", provider);
        return new ExtractiveLlmClient();
    }

    private static boolean present(String v) {
        return v != null && !v.isBlank();
    }
}
