package com.airplane.repository;

import com.airplane.config.RagProperties;
import com.airplane.domain.QaDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

/** classpath 의 qa_embeddings.jsonl (Q&A + 사전 계산 임베딩)을 메모리에 적재한다. */
@Repository
public class QaRepository {

    private static final Logger log = LoggerFactory.getLogger(QaRepository.class);

    private final List<QaDocument> documents;

    public QaRepository(RagProperties properties, ObjectMapper mapper) {
        this.documents = List.copyOf(load(properties.dataFile(), properties.embedding().dimension(), mapper));
        log.info("Q&A 지식베이스 적재 완료: {}건 ({}차원)", documents.size(), properties.embedding().dimension());
    }

    public List<QaDocument> findAll() {
        return documents;
    }

    public List<QaDocument> findByOrg(String org) {
        return documents.stream().filter(d -> d.org().equals(org)).toList();
    }

    static List<QaDocument> load(String path, int dimension, ObjectMapper mapper) {
        List<QaDocument> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                JsonNode n = mapper.readTree(line);
                JsonNode emb = n.get("embedding");
                if (emb == null || emb.size() != dimension) {
                    throw new IllegalStateException("임베딩 차원 불일치: id=" + n.path("id").asText());
                }
                float[] v = new float[dimension];
                for (int i = 0; i < dimension; i++) v[i] = (float) emb.get(i).asDouble();
                result.add(new QaDocument(n.get("id").asText(), n.get("org_type").asText(), n.get("org").asText(),
                        n.get("question").asText(), n.get("answer").asText(), n.path("source").asText(""), v));
            }
        } catch (IOException e) {
            throw new IllegalStateException("지식베이스 파일을 읽을 수 없습니다: " + path, e);
        }
        return result;
    }
}
