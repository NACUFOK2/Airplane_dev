package com.airplane.domain;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/** 답변 대상 기관 (여행사 / 항공사). */
public record Organization(String code, String name, String type, String brandColor, List<String> officialDomains) {

    public Organization {
        officialDomains = officialDomains == null ? List.of() : List.copyOf(officialDomains);
    }

    /** 출처 URL 의 호스트가 공식 도메인이거나 그 하위 도메인이면 true. */
    public boolean isOfficialSource(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            String host = URI.create(url.strip()).getHost();
            if (host == null) return false;
            String h = host.toLowerCase(Locale.ROOT);
            return officialDomains.stream().anyMatch(d -> h.equals(d) || h.endsWith("." + d));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
