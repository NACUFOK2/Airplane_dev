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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

/**
 * classpath 의 qa_embeddings.jsonl 을 메모리에 적재한다.
 * 한 줄 = 질문 변형 1개(원문·구어체·키워드·다른 표현). parent_id 가 같은 줄을 원문 1건으로 묶는다.
 */
@Repository
public class QaRepository {

    private static final Logger log = LoggerFactory.getLogger(QaRepository.class);

    private final List<QaDocument> documents;

    public QaRepository(RagProperties properties, ObjectMapper mapper, OrganizationRepository organizations) {
        this.documents = List.copyOf(load(properties.dataFile(), properties.embedding().dimension(), mapper, organizations));
        log.info("Q&A 지식베이스 적재 완료: 원문 {}건, 질문 변형 {}개 ({}차원), 공식 출처 {}건",
                documents.size(), documents.stream().mapToInt(d -> d.variants().size()).sum(),
                properties.embedding().dimension(), documents.stream().filter(QaDocument::official).count());
    }

    public List<QaDocument> findAll() {
        return documents;
    }

    public List<QaDocument> findByOrg(String org) {
        return documents.stream().filter(d -> d.org().equals(org)).toList();
    }

    private static final class Group {
        JsonNode original;
        final List<String> variants = new ArrayList<>();
        final List<float[]> vectors = new ArrayList<>();
    }

    static List<QaDocument> load(String path, int dimension, ObjectMapper mapper, OrganizationRepository organizations) {
        Map<String, Group> groups = new LinkedHashMap<>();
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
                Group g = groups.computeIfAbsent(n.path("parent_id").asText(n.path("id").asText()), k -> new Group());
                if ("original".equals(n.path("variant").asText("original")) || g.original == null) g.original = n;
                g.variants.add(n.get("question").asText());
                g.vectors.add(v);
            }
        } catch (IOException e) {
            throw new IllegalStateException("지식베이스 파일을 읽을 수 없습니다: " + path, e);
        }

        List<QaDocument> result = new ArrayList<>();
        for (Map.Entry<String, Group> e : groups.entrySet()) {
            JsonNode o = e.getValue().original;
            String org = o.get("org").asText();
            String source = o.path("source").asText("");
            if (organizations.findByName(org).isEmpty()) {
                log.warn("설정에 없는 기관입니다 (application.yml rag.organizations 에 추가 필요): {}", org);
            }
            boolean official = organizations.findByName(org).map(x -> x.isOfficialSource(source)).orElse(false);
            String orgType = o.path("category").asText(o.path("org_type").asText(""));
            String question = o.path("original_question").asText(o.get("question").asText());
            result.add(new QaDocument(e.getKey(), orgType, org, question, List.copyOf(e.getValue().variants),
                    o.get("answer").asText(), source, official, e.getValue().vectors.toArray(float[][]::new)));
        }
        return result;
    }
}
