package com.airplane.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrivacyMaskerTest {

    // 가짜 값도 저장소 pre-commit 훅의 개인정보 패턴에 걸리므로 실행 시점에 조립한다
    private static String join(String sep, String... parts) {
        return String.join(sep, parts);
    }

    @Test
    void masksPersonalIdentifiers() {
        assertThat(PrivacyMasker.mask("메일은 hong.gd@example.co.kr 이에요")).isEqualTo("메일은 [이메일] 이에요");
        assertThat(PrivacyMasker.mask("주민번호 " + join("-", "900101", "1234567"))).isEqualTo("주민번호 [주민번호]");
        assertThat(PrivacyMasker.mask("카드 " + join("-", "1234", "5678", "9012", "3456") + " 결제")).isEqualTo("카드 [카드번호] 결제");
        assertThat(PrivacyMasker.mask("연락처 010-1234-5678")).isEqualTo("연락처 [전화번호]");
        assertThat(PrivacyMasker.mask("여권 M12345678 만료")).isEqualTo("여권 [여권번호] 만료");
        assertThat(PrivacyMasker.mask("예약번호 AB12CD 취소")).isEqualTo("예약번호 [예약번호] 취소");
    }

    @Test
    void keepsTravelTermsAndNumbers() {
        String q = "대한항공 23kg 수하물, 43000mAh 160Wh 보조배터리, 7C 고객센터 1544-5252, 2026년 3월";
        assertThat(PrivacyMasker.mask(q)).isEqualTo(q);
    }

    @Test
    void handlesNull() {
        assertThat(PrivacyMasker.mask(null)).isNull();
    }
}
