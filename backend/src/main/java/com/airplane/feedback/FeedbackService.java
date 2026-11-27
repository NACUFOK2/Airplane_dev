package com.airplane.feedback;

import com.airplane.config.RagProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 👍/👎 피드백과 범위 밖 질문을 로컬 JSONL 로 기록하고 집계한다 (데이터 공백 발견용). */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);
    static final String FILE_NAME = "feedback.jsonl";

    private final boolean enabled;
    private final Path file;
    private final ObjectMapper mapper;

    public FeedbackService(RagProperties properties, ObjectMapper mapper) {
        RagProperties.Feedback cfg = properties.feedback();
        this.enabled = cfg != null && cfg.enabled();
        this.file = Path.of(cfg == null ? "chat-logs" : cfg.logDir()).resolve(FILE_NAME);
        this.mapper = mapper;
    }

    public void recordFeedback(String rating, String question, String orgCode, String mode, List<String> sourceIds) {
        append(new FeedbackEvent(now(), "feedback", rating, PiiMasker.mask(question), orgCode, mode,
                sourceIds == null ? List.of() : List.copyOf(sourceIds)));
    }

    public void recordOutOfScope(String question, String orgCode) {
        append(new FeedbackEvent(now(), "out_of_scope", null, PiiMasker.mask(question), orgCode, "out_of_scope", List.of()));
    }

    public Insights insights(int limit) {
        List<FeedbackEvent> events = readAll();
        List<FeedbackEvent> fb = events.stream().filter(e -> "feedback".equals(e.type())).toList();
        long up = fb.stream().filter(e -> "up".equals(e.rating())).count();
        List<FeedbackEvent> down = fb.stream().filter(e -> "down".equals(e.rating())).toList();
        List<FeedbackEvent> oos = events.stream().filter(e -> "out_of_scope".equals(e.type())).toList();
        Map<String, Long> downBySource = down.stream().flatMap(e -> e.sourceIds().stream().limit(1))
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        return new Insights(fb.size(), up, down.size(), oos.size(),
                top(oos, limit), top(down, limit), topEntries(downBySource, limit));
    }

    public record Insights(long feedbackCount, long upCount, long downCount, long outOfScopeCount,
                           List<Count> topOutOfScopeQuestions, List<Count> topDownvotedQuestions,
                           List<Count> topDownvotedSourceIds) {}

    public record Count(String key, long count) {}

    private static List<Count> top(List<FeedbackEvent> events, int limit) {
        Map<String, Long> m = events.stream().collect(Collectors.groupingBy(
                e -> e.question().strip(), LinkedHashMap::new, Collectors.counting()));
        return topEntries(m, limit);
    }

    private static List<Count> topEntries(Map<String, Long> m, int limit) {
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit).map(e -> new Count(e.getKey(), e.getValue())).toList();
    }

    private synchronized void append(FeedbackEvent event) {
        if (!enabled) return;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, mapper.writeValueAsString(event) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("피드백 로그 기록 실패: {}", e.getClass().getSimpleName());
        }
    }

    private synchronized List<FeedbackEvent> readAll() {
        if (!Files.exists(file)) return List.of();
        List<FeedbackEvent> out = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) out.add(mapper.readValue(line, FeedbackEvent.class));
            }
        } catch (IOException e) {
            log.warn("피드백 로그 읽기 실패: {}", e.getClass().getSimpleName());
        }
        return out;
    }

    private static String now() {
        return OffsetDateTime.now().withNano(0).toString();
    }
}
