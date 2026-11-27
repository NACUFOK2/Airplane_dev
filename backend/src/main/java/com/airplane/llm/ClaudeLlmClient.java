package com.airplane.llm;

import com.airplane.config.RagProperties;
import com.airplane.domain.ChatTurn;
import com.airplane.domain.RetrievedChunk;
import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.TextBlock;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Anthropic Claude 기반 답변 생성기. */
public class ClaudeLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeLlmClient.class);

    private final AnthropicClient client;
    private final RagProperties.Llm config;
    private final ExtractiveLlmClient fallback = new ExtractiveLlmClient();

    public ClaudeLlmClient(AnthropicClient client, RagProperties.Llm config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public String generate(String question, List<RetrievedChunk> context, List<ChatTurn> history, String airlineLabel) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(config.model())
                .maxTokens(config.maxTokens())
                .system(PromptBuilder.SYSTEM_PROMPT)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                // 안전 분류기로 거절될 경우 서버 측에서 자동으로 대체 모델로 재시도
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));

        for (ChatTurn turn : history) {
            if (turn.role() == ChatTurn.Role.USER) builder.addUserMessage(turn.content());
            else builder.addAssistantMessage(turn.content());
        }
        builder.addUserMessage(PromptBuilder.userMessage(question, context, airlineLabel));

        try {
            Message response = client.messages().create(builder.build());
            if (response.stopReason().map(r -> r.toString().equals("refusal")).orElse(false)) {
                return "죄송해요, 이 질문에는 답변을 드리기 어려워요. 다른 방식으로 질문해 주세요.";
            }
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining("\n"))
                    .strip();
            return text.isEmpty() ? fallback.generate(question, context, history, airlineLabel) : text;
        } catch (RateLimitException e) {
            log.warn("Claude rate limit → 추출형 답변으로 대체");
            return fallback.generate(question, context, history, airlineLabel);
        } catch (AnthropicServiceException e) {
            log.error("Claude API 오류 (status={}) → 추출형 답변으로 대체", e.statusCode());
            return fallback.generate(question, context, history, airlineLabel);
        }
    }

    @Override
    public String mode() {
        return "claude";
    }
}
