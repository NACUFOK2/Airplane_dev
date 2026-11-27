package com.airplane.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.airplane.config.RagProperties;
import com.airplane.domain.QaDocument;
import com.airplane.domain.RetrievedDocument;
import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.services.blocking.MessageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 실제 API 를 부르지 않고 SDK 응답·예외를 흉내 내 대체(fallback) 경로와 mode 표기를 검증한다. */
class ClaudeLlmClientTest {

    private final AnthropicClient client = mock(AnthropicClient.class);
    private final MessageService messages = mock(MessageService.class);
    private final ClaudeLlmClient claude = new ClaudeLlmClient(client,
            new RagProperties.Llm("claude", "claude-opus-5-5", 2000, 3));
    private final List<RetrievedDocument> context = List.of(new RetrievedDocument(
            QaDocument.single("항공-014", "항공사", "대한항공", "보조배터리는 어떻게 가져가나요?",
                    "1인 5개까지 휴대 수하물로만 운송 가능합니다.", "https://www.koreanair.com", true, null),
            1.0, 0.8, 1, 1));

    ClaudeLlmClientTest() {
        when(client.messages()).thenReturn(messages);
    }

    private static Message message(String text, String stopReason) {
        Message m = mock(Message.class);
        when(m.stopReason()).thenReturn(Optional.of(StopReason.of(stopReason)));
        ContentBlock block = mock(ContentBlock.class);
        TextBlock tb = mock(TextBlock.class);
        when(tb.text()).thenReturn(text);
        when(block.text()).thenReturn(Optional.of(tb));
        when(m.content()).thenReturn(List.of(block));
        return m;
    }

    private LlmAnswer ask() {
        return claude.generate("보조배터리 몇 개?", context, List.of(), "대한항공");
    }

    @Test
    void successIsReportedAsClaude() {
        Message response = message("5개까지 가능해요 [1]", "end_turn");
        when(messages.create(any(MessageCreateParams.class))).thenReturn(response);
        assertThat(ask()).isEqualTo(new LlmAnswer("5개까지 가능해요 [1]", LlmAnswer.CLAUDE));
    }

    @Test
    void refusalIsAnsweredPolitelyAsClaude() {
        Message response = message("", "refusal");
        when(messages.create(any(MessageCreateParams.class))).thenReturn(response);
        LlmAnswer a = ask();
        assertThat(a.mode()).isEqualTo(LlmAnswer.CLAUDE);
        assertThat(a.text()).contains("답변을 드리기 어려워요");
    }

    @Test
    void emptyResponseFallsBackAndIsReportedAsExtractive() {
        Message response = message("  ", "end_turn");
        when(messages.create(any(MessageCreateParams.class))).thenReturn(response);
        LlmAnswer a = ask();
        assertThat(a.mode()).isEqualTo(LlmAnswer.EXTRACTIVE);
        assertThat(a.text()).contains("**대한항공** 기준으로 안내드려요", "1인 5개까지");
    }

    @Test
    void networkErrorFallsBackInsteadOfFailing() {
        when(messages.create(any(MessageCreateParams.class))).thenThrow(new AnthropicIoException("Request timed out"));
        LlmAnswer a = ask();
        assertThat(a.mode()).isEqualTo(LlmAnswer.EXTRACTIVE);
        assertThat(a.text()).contains("1인 5개까지");
    }

    @Test
    void otherSdkErrorFallsBackInsteadOfFailing() {
        when(messages.create(any(MessageCreateParams.class))).thenThrow(new AnthropicException("retries exhausted"));
        assertThat(ask().mode()).isEqualTo(LlmAnswer.EXTRACTIVE);
    }
}
