package com.airplane.llm;

import com.airplane.config.RagProperties;
import com.airplane.domain.ChatTurn;
import com.airplane.domain.RetrievedDocument;
import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
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
    public LlmAnswer generate(String question, List<RetrievedDocument> context, List<ChatTurn> history, String scopeLabel) {
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
        builder.addUserMessage(PromptBuilder.userMessage(question, context, scopeLabel));

        try {
            Message response = client.messages().create(builder.build());
            if (response.stopReason().map(r -> r.toString().equals("refusal")).orElse(false)) {
                return new LlmAnswer("죄송해요, 이 질문에는 답변을 드리기 어려워요. 다른 방식으로 질문해 주세요.", LlmAnswer.CLAUDE);
            }
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining("\n"))
                    .strip();
            if (text.isEmpty()) {
                log.warn("Claude 빈 응답 → 추출형 답변으로 대체");
                return fallback.generate(question, context, history, scopeLabel);
            }
            return new LlmAnswer(text, LlmAnswer.CLAUDE);
        } catch (RateLimitException e) {
            log.warn("Claude 요청 한도 초과(429) → 추출형 답변으로 대체");
            return fallback.generate(question, context, history, scopeLabel);
        } catch (AnthropicServiceException e) {
            log.error("Claude API 오류 (status={}) → 추출형 답변으로 대체", e.statusCode());
            return fallback.generate(question, context, history, scopeLabel);
        } catch (AnthropicIoException e) {
            // 타임아웃·연결 실패 등 네트워크 오류 (HTTP 상태 오류와 다른 예외 계층)
            log.error("Claude 네트워크 오류 ({}) → 추출형 답변으로 대체", e.getClass().getSimpleName());
            return fallback.generate(question, context, history, scopeLabel);
        } catch (AnthropicException e) {
            // 재시도 소진 등 그 밖의 SDK 오류도 사용자에게 500 대신 추출형 답변을 준다
            log.error("Claude SDK 오류 ({}) → 추출형 답변으로 대체", e.getClass().getSimpleName());
            return fallback.generate(question, context, history, scopeLabel);
        }
    }
}
