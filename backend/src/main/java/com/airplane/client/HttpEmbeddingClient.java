package com.airplane.client;

import com.airplane.config.RagProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** embedding-service(FastAPI, 127.0.0.1:8001) 호출 클라이언트. */
@Component
public class HttpEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(HttpEmbeddingClient.class);

    private final RestClient restClient;
    private final int dimension;

    public HttpEmbeddingClient(RagProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(500));
        factory.setReadTimeout(Duration.ofMillis(properties.embedding().timeoutMs()));
        this.restClient = RestClient.builder().baseUrl(properties.embedding().baseUrl()).requestFactory(factory).build();
        this.dimension = properties.embedding().dimension();
    }

    record EmbedResponse(String model, int dimension, List<List<Double>> vectors) {}

    @Override
    public Optional<float[]> embedQuery(String text) {
        try {
            EmbedResponse res = restClient.post().uri("/embed")
                    .body(Map.of("texts", List.of(text), "kind", "query"))
                    .retrieve().body(EmbedResponse.class);
            if (res == null || res.vectors().isEmpty() || res.vectors().get(0).size() != dimension) {
                log.warn("임베딩 응답 형식 오류 → BM25 폴백");
                return Optional.empty();
            }
            List<Double> v = res.vectors().get(0);
            float[] out = new float[v.size()];
            for (int i = 0; i < out.length; i++) out[i] = v.get(i).floatValue();
            return Optional.of(out);
        } catch (RestClientException e) {
            log.warn("임베딩 서비스 호출 실패({}) → BM25 폴백", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}
