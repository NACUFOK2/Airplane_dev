package com.airplane.repository;

import com.airplane.config.RagProperties;
import com.airplane.domain.Organization;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationRepository {

    private final Map<String, Organization> byCode = new LinkedHashMap<>();
    /** 질문 속 기관명 탐지용: 긴 이름 우선 매칭 */
    private final List<Organization> byNameLengthDesc;

    public OrganizationRepository(RagProperties properties) {
        for (RagProperties.OrganizationDef d : properties.organizations()) {
            byCode.put(d.code(), new Organization(d.code(), d.name(), d.type(), d.brandColor()));
        }
        byNameLengthDesc = byCode.values().stream()
                .sorted(Comparator.comparingInt((Organization o) -> o.name().length()).reversed())
                .toList();
    }

    public List<Organization> findAll() {
        return List.copyOf(byCode.values());
    }

    public Optional<Organization> findByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public Optional<Organization> detectInText(String text) {
        String compact = text.replace(" ", "");
        return byNameLengthDesc.stream().filter(o -> compact.contains(o.name())).findFirst();
    }
}
