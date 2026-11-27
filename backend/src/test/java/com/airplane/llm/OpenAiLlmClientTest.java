package com.airplane.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.airplane.config.RagProperties;
import com.airplane.domain.ChatTurn;
import com.airplane.domain.QaDocument;
import com.airplane.domain.RetrievedDocument;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** 실제 OpenAI 를 부르지 않고 가짜 HTTP 서버로 요청 형식과 대체(fallback) 경로를 검증한다. */
class OpenAiLlmClientTest {

    private static final String BASE = "https://api.openai.test/v1";
    private final RagProperties.OpenAi config = new RagProperties.OpenAi("gpt-test", BASE, 4000, "low", 5000);
    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE).defaultHeader("Authorization", "Bearer test-key");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final OpenAiLlmClient openai = new OpenAiLlmClient(builder.build(), config);
    private final List<RetrievedDocument> context = List.of(new RetrievedDocument(
            QaDocument.single("항공-014", "항공사", "대한항공", "보조배터리는 어떻게 가져가나요?",
                    "1인 5개까지 휴대 수하물로만 운송 가능합니다.", "https://www.koreanair.com", true, null),
            1.0, 0.8, 1, 1));

    private LlmAnswer ask() {
        return openai.generate("보조배터리 몇 개?", context,
                List.of(new ChatTurn(ChatTurn.Role.USER, "대한항공 수하물"), new ChatTurn(ChatTurn.Role.ASSISTANT, "이전 답변")),
                "대한항공");
    }

    private static String completion(String content, String finishReason) {
        return """
                {"id":"chatcmpl-1","object":"chat.completion","choices":[{"index":0,
                 "message":{"role":"assistant","content":%s,"refusal":null},"finish_reason":"%s"}]}
                """.formatted(content == null ? "null" : "\"" + content + "\"", finishReason);
    }

    @Test
    void sendsChatCompletionsRequestAndReportsOpenAiMode() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-test"))
                .andExpect(jsonPath("$.max_completion_tokens").value(4000))
                .andExpect(jsonPath("$.reasoning_effort").value("low"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("대한항공 수하물"))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"))
                .andExpect(jsonPath("$.messages[3].content").value(org.hamcrest.Matchers.containsString("<documents>")))
                .andRespond(withSuccess(completion("5개까지 가능해요 [1]", "stop"), MediaType.APPLICATION_JSON));
        assertThat(ask()).isEqualTo(new LlmAnswer("5개까지 가능해요 [1]", LlmAnswer.OPENAI));
        server.verify();
    }

    @Test
    void refusalIsAnsweredPolitely() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withSuccess("""
                {"choices":[{"message":{"role":"assistant","content":null,"refusal":"I can't help"},"finish_reason":"stop"}]}
                """, MediaType.APPLICATION_JSON));
        LlmAnswer a = ask();
        assertThat(a.mode()).isEqualTo(LlmAnswer.OPENAI);
        assertThat(a.text()).contains("답변을 드리기 어려워요");
    }

    @Test
    void emptyContentFromTokenLimitFallsBack() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andRespond(withSuccess(completion("", "length"), MediaType.APPLICATION_JSON));
        LlmAnswer a = ask();
        assertThat(a.mode()).isEqualTo(LlmAnswer.EXTRACTIVE);
        assertThat(a.text()).contains("1인 5개까지");
    }

    @Test
    void invalidKeyOrUnknownModelFallsBack() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"code\":\"invalid_api_key\"}}"));
        assertThat(ask().mode()).isEqualTo(LlmAnswer.EXTRACTIVE);
    }

    @Test
    void rateLimitFallsBack() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThat(ask().mode()).isEqualTo(LlmAnswer.EXTRACTIVE);
    }

    @Test
    void networkErrorFallsBack() {
        // 닫힌 포트로 실제 연결 실패를 일으킨다
        RagProperties.OpenAi closed = new RagProperties.OpenAi("gpt-test", "http://127.0.0.1:9/v1", 4000, "low", 2000);
        OpenAiLlmClient unreachable = new OpenAiLlmClient(RestClient.builder(), "test-key", closed);
        assertThat(unreachable.generate("질문", context, List.of(), "대한항공").mode()).isEqualTo(LlmAnswer.EXTRACTIVE);
    }
}
