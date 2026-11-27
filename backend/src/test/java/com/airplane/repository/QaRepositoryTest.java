package com.airplane.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.domain.QaDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class QaRepositoryTest {

    @Test
    void loadsAll83QaWithNormalizedVectors() {
        List<QaDocument> docs = QaRepository.load("data/qa_embeddings.jsonl", 384, new ObjectMapper());
        assertThat(docs).hasSize(83);
        assertThat(docs).extracting(QaDocument::org).contains("하나투어", "대한항공", "에어로케이");
        for (QaDocument d : docs) {
            double norm = 0;
            for (float x : d.embedding()) norm += x * x;
            assertThat(Math.sqrt(norm)).isBetween(0.999, 1.001);
        }
    }
}
