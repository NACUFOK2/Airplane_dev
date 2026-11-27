package com.airplane.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** embedding-service/data 와 backend 리소스의 인덱스가 같은지 확인 (scripts/build_index.py 가 두 곳에 쓴다). */
class IndexSyncTest {

    @Test
    void backendIndexMatchesEmbeddingServiceIndex() throws Exception {
        Path backendCopy = Path.of("src/main/resources/data/qa_embeddings.jsonl");
        Path serviceCopy = Path.of("../embedding-service/data/qa_embeddings.jsonl");
        Assumptions.assumeTrue(Files.exists(serviceCopy), "embedding-service 가 없는 환경에서는 건너뜀");
        assertThat(Files.mismatch(backendCopy, serviceCopy))
                .as("두 인덱스가 다릅니다. embedding-service/scripts/build_index.py 로 다시 생성하세요.")
                .isEqualTo(-1L);
    }
}
