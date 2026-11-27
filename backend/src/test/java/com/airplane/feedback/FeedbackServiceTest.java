package com.airplane.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.config.RagProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeedbackServiceTest {

    @TempDir
    Path dir;

    private FeedbackService service(boolean enabled) {
        RagProperties props = new RagProperties(null, List.of(), null, null, null,
                new RagProperties.Feedback(enabled, dir.toString()), List.of());
        return new FeedbackService(props, new ObjectMapper());
    }

    @Test
    void aggregatesFeedbackAndOutOfScope() {
        FeedbackService s = service(true);
        s.recordFeedback("up", "보조배터리 몇 개?", "ALL", "extractive", List.of("052"));
        s.recordFeedback("down", "대한항공 환불 수수료?", "koreanair", "extractive", List.of("043", "050"));
        s.recordFeedback("down", "대한항공 환불 수수료?", "koreanair", "extractive", List.of("043"));
        s.recordOutOfScope("아시아나 마일리지", "ALL");

        FeedbackService.Insights in = s.insights(5);
        assertThat(in.feedbackCount()).isEqualTo(3);
        assertThat(in.upCount()).isEqualTo(1);
        assertThat(in.downCount()).isEqualTo(2);
        assertThat(in.outOfScopeCount()).isEqualTo(1);
        assertThat(in.topDownvotedQuestions().get(0)).isEqualTo(new FeedbackService.Count("대한항공 환불 수수료?", 2));
        assertThat(in.topDownvotedSourceIds().get(0)).isEqualTo(new FeedbackService.Count("043", 2));
        assertThat(in.topOutOfScopeQuestions().get(0).key()).isEqualTo("아시아나 마일리지");
    }

    @Test
    void masksPiiBeforeWritingToDisk() throws Exception {
        service(true).recordOutOfScope("예약번호 AB12CD, 연락처 010-1234-5678", "ALL");
        String line = Files.readString(dir.resolve(FeedbackService.FILE_NAME));
        assertThat(line).contains("[예약번호]", "[전화번호]").doesNotContain("AB12CD", "010-1234-5678");
    }

    @Test
    void disabledWritesNothing() {
        service(false).recordOutOfScope("질문", "ALL");
        assertThat(Files.exists(dir.resolve(FeedbackService.FILE_NAME))).isFalse();
    }
}
