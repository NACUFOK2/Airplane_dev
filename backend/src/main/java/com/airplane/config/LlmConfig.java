package com.airplane.config;

import com.airplane.llm.ClaudeLlmClient;
import com.airplane.llm.ExtractiveLlmClient;
import com.airplane.llm.LlmClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ANTHROPIC_API_KEY 환경변수가 있으면 Claude, 없으면 추출형(오프라인) 답변기를 사용한다.
 * API 키는 환경변수로만 읽으며 설정 파일/로그에 남기지 않는다.
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    public LlmClient llmClient(RagProperties properties) {
        String key = System.getenv("ANTHROPIC_API_KEY");
        boolean forceExtractive = "extractive".equalsIgnoreCase(properties.llm().provider());
        if (forceExtractive || key == null || key.isBlank()) {
            log.warn("ANTHROPIC_API_KEY 미설정 → 추출형(오프라인) 답변 모드로 동작합니다.");
            return new ExtractiveLlmClient();
        }
        log.info("Claude LLM 모드 활성화 (model={})", properties.llm().model());
        return new ClaudeLlmClient(AnthropicOkHttpClient.fromEnv(), properties.llm());
    }
}
