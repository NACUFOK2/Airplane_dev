package com.airplane.repository;

import com.airplane.config.RagProperties;
import com.airplane.domain.KnowledgeChunk;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

/**
 * classpath:knowledge/*.md 문서를 읽어 '## [카테고리] 제목' 섹션 단위 청크로 적재한다.
 */
@Repository
public class KnowledgeRepository {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRepository.class);

    private final List<KnowledgeChunk> chunks;

    public KnowledgeRepository(RagProperties properties) {
        List<KnowledgeChunk> loaded = new ArrayList<>();
        for (RagProperties.AirlineDef def : properties.airlines()) {
            loaded.addAll(load(def));
        }
        this.chunks = List.copyOf(loaded);
        log.info("지식 베이스 적재 완료: {}개 항공사, {}개 청크", properties.airlines().size(), chunks.size());
    }

    public List<KnowledgeChunk> findAll() {
        return chunks;
    }

    public List<KnowledgeChunk> findByAirline(String airlineCode) {
        return chunks.stream().filter(c -> c.airlineCode().equals(airlineCode)).toList();
    }

    private List<KnowledgeChunk> load(RagProperties.AirlineDef def) {
        ClassPathResource resource = new ClassPathResource("knowledge/" + def.file());
        try (InputStream in = resource.getInputStream()) {
            return parse(def.code(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("지식 문서를 읽을 수 없습니다: " + def.file(), e);
        }
    }

    static List<KnowledgeChunk> parse(String airlineCode, String markdown) {
        List<KnowledgeChunk> result = new ArrayList<>();
        String category = "일반";
        String title = null;
        StringBuilder body = new StringBuilder();
        int seq = 0;
        for (String line : markdown.split("\\R")) {
            if (line.startsWith("## ")) {
                if (title != null && !body.isEmpty()) {
                    result.add(new KnowledgeChunk(airlineCode + "-" + (++seq), airlineCode, category, title, body.toString().strip()));
                }
                String heading = line.substring(3).strip();
                if (heading.startsWith("[") && heading.contains("]")) {
                    category = heading.substring(1, heading.indexOf(']')).strip();
                    title = heading.substring(heading.indexOf(']') + 1).strip();
                } else {
                    category = "일반";
                    title = heading;
                }
                body.setLength(0);
            } else if (title != null && !line.startsWith("# ") && !line.startsWith(">")) {
                body.append(line).append('\n');
            }
        }
        if (title != null && !body.isEmpty()) {
            result.add(new KnowledgeChunk(airlineCode + "-" + (++seq), airlineCode, category, title, body.toString().strip()));
        }
        return result;
    }
}
