package com.airplane.client;

import java.util.Optional;

/** 질의 임베딩 제공자. 실패 시 Optional.empty() 를 반환해 호출 측이 BM25 로 폴백하게 한다. */
public interface EmbeddingClient {
    Optional<float[]> embedQuery(String text);
}
