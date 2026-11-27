package com.airplane.repository;

import com.airplane.config.RagProperties;
import com.airplane.domain.Organization;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationRepository {

    private final Map<String, Organization> byCode = new LinkedHashMap<>();
    /** 질문 속 기관명 탐지용 (이름, 기관) 목록: 긴 이름 우선 매칭 ("트립닷컴"이 "트립"보다 먼저) */
    private final List<Map.Entry<String, Organization>> detectionNames;

    public OrganizationRepository(RagProperties properties) {
        for (RagProperties.OrganizationDef d : properties.organizations()) {
            byCode.put(d.code(), new Organization(d.code(), d.name(), d.type(), d.brandColor(), d.officialDomains(), d.aliases()));
        }
        List<Map.Entry<String, Organization>> names = new ArrayList<>();
        for (Organization o : byCode.values()) {
            for (String n : o.namesForDetection()) names.add(Map.entry(n.replace(" ", ""), o));
        }
        names.sort(Comparator.comparingInt((Map.Entry<String, Organization> e) -> e.getKey().length()).reversed());
        this.detectionNames = List.copyOf(names);
    }

    public List<Organization> findAll() {
        return List.copyOf(byCode.values());
    }

    public Optional<Organization> findByName(String name) {
        return byCode.values().stream().filter(o -> o.name().equals(name)).findFirst();
    }

    public Optional<Organization> findByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    /** 공백을 무시하고 정식 이름·별칭이 들어 있으면 해당 기관을 돌려준다. */
    public Optional<Organization> detectInText(String text) {
        if (text == null) return Optional.empty();
        String compact = text.replace(" ", "");
        return detectionNames.stream().filter(e -> compact.contains(e.getKey())).map(Map.Entry::getValue).findFirst();
    }
}
