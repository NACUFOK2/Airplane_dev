package com.airplane.llm;

import com.airplane.config.RagProperties;
import com.airplane.domain.ChatTurn;
import com.airplane.domain.RetrievedDocument;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * OpenAI Chat Completions(POST /v1/chat/completions) 기반 답변 생성기.
 * 프롬프트·문맥·대화 기록은 Claude 와 똑같이 PromptBuilder 를 쓰고, 실패하면 추출형으로 대체한다.
 * API 키는 생성자로만 받으며 로그·설정 파일에 남기지 않는다.
 */
public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final RestClient restClient;
    private final RagProperties.OpenAi config;
    private final ExtractiveLlmClient fallback = new ExtractiveLlmClient();

    public OpenAiLlmClient(RestClient.Builder builder, String apiKey, RagProperties.OpenAi config) {
        this(builder.requestFactory(timeouts(config))
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build(), config);
    }

    /** 테스트에서 가짜 HTTP 서버에 묶은 RestClient 를 넣을 때 쓴다. */
    OpenAiLlmClient(RestClient restClient, RagProperties.OpenAi config) {
        this.restClient = restClient;
        this.config = config;
    }

    private static SimpleClientHttpRequestFactory timeouts(RagProperties.OpenAi config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofMillis(config.timeoutMs()));
        return factory;
    }

    @Override
    public LlmAnswer generate(String question, List<RetrievedDocument> context, List<ChatTurn> history, String scopeLabel) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", PromptBuilder.SYSTEM_PROMPT));
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role() == ChatTurn.Role.USER ? "user" : "assistant", "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", PromptBuilder.userMessage(question, context, scopeLabel)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.model());
        body.put("messages", messages);
        body.put("max_completion_tokens", config.maxCompletionTokens());
        if (config.reasoningEffort() != null && !config.reasoningEffort().isBlank()) {
            body.put("reasoning_effort", config.reasoningEffort());
        }

        try {
            JsonNode res = restClient.post().uri("/chat/completions").body(body).retrieve().body(JsonNode.class);
            JsonNode message = res == null ? null : res.path("choices").path(0).path("message");
            if (message == null || message.isMissingNode()) {
                log.warn("OpenAI 응답 형식 오류 → 추출형 답변으로 대체");
                return fallback.generate(question, context, history, scopeLabel);
            }
            if (message.hasNonNull("refusal") && !message.get("refusal").asText().isBlank()) {
                return new LlmAnswer("죄송해요, 이 질문에는 답변을 드리기 어려워요. 다른 방식으로 질문해 주세요.", LlmAnswer.OPENAI);
            }
            String text = message.path("content").asText("").strip();
            if (text.isEmpty()) {
                // 추론 토큰이 상한을 다 쓰면 finish_reason=length 로 빈 답이 올 수 있다
                log.warn("OpenAI 빈 응답 (finish_reason={}) → 추출형 답변으로 대체",
                        res.path("choices").path(0).path("finish_reason").asText("?"));
                return fallback.generate(question, context, history, scopeLabel);
            }
            return new LlmAnswer(text, LlmAnswer.OPENAI);
        } catch (RestClientResponseException e) {
            // 401(키 오류)·404(모델 없음)·429(한도)·5xx. 응답 본문에는 요청 내용이 섞일 수 있어 상태 코드만 남긴다
            log.error("OpenAI API 오류 (status={}) → 추출형 답변으로 대체", e.getStatusCode().value());
            return fallback.generate(question, context, history, scopeLabel);
        } catch (ResourceAccessException e) {
            log.error("OpenAI 네트워크 오류 ({}) → 추출형 답변으로 대체", e.getClass().getSimpleName());
            return fallback.generate(question, context, history, scopeLabel);
        } catch (RestClientException e) {
            log.error("OpenAI 호출 오류 ({}) → 추출형 답변으로 대체", e.getClass().getSimpleName());
            return fallback.generate(question, context, history, scopeLabel);
        }
    }
}
