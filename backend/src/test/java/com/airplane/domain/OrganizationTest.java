package com.airplane.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrganizationTest {

    private final Organization ke = new Organization("koreanair", "대한항공", "항공사", "#000", List.of("koreanair.com"), List.of());

    @Test
    void acceptsExactAndSubdomains() {
        assertThat(ke.isOfficialSource("https://www.koreanair.com/booking")).isTrue();
        assertThat(ke.isOfficialSource("https://news.koreanair.com/x")).isTrue();
        assertThat(ke.isOfficialSource("https://koreanair.com")).isTrue();
    }

    @Test
    void rejectsLookalikeAndInvalidUrls() {
        assertThat(ke.isOfficialSource("https://fakekoreanair.com")).isFalse();
        assertThat(ke.isOfficialSource("https://koreanair.com.evil.kr")).isFalse();
        assertThat(ke.isOfficialSource("블로그")).isFalse();
        assertThat(ke.isOfficialSource(null)).isFalse();
    }
}
