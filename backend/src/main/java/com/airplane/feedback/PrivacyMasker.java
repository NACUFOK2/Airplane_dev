package com.airplane.feedback;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 로그 저장 전 개인정보 마스킹. 오탐(과잉 마스킹)은 허용하고 누락을 줄이는 방향으로 설계했다.
 * 순서가 중요하다: 긴 패턴(주민번호·카드번호)을 먼저 처리한다.
 */
public final class PrivacyMasker {

    private record Rule(Pattern pattern, String replacement) {}

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"), "[이메일]"),
            new Rule(Pattern.compile("\\b\\d{6}\\s?-\\s?[1-8]\\d{6}\\b"), "[주민번호]"),
            new Rule(Pattern.compile("\\b(?:\\d{4}[\\s-]?){3}\\d{4}\\b"), "[카드번호]"),
            new Rule(Pattern.compile("\\b01[016789][\\s-]?\\d{3,4}[\\s-]?\\d{4}\\b"), "[전화번호]"),
            new Rule(Pattern.compile("\\b[A-Za-z]{1,2}\\d{7,8}\\b"), "[여권번호]"),
            // 예약번호류: 영문+숫자가 섞인 6자 이상 토큰 (예: AB12CD, 7C1234X)
            // 단, '43000mAh', '160Wh', '23kg' 같은 숫자+단위는 제외
            new Rule(Pattern.compile("(?i)\\b(?!\\d+(?:kg|g|mah|wh|cm|mm|ml|km|kb|mb|gb)\\b)"
                    + "(?=[A-Za-z0-9]*\\d)(?=[A-Za-z0-9]*[A-Za-z])[A-Za-z0-9]{6,}\\b"), "[예약번호]"),
            new Rule(Pattern.compile("\\b\\d{9,}\\b"), "[번호]"));

    private PrivacyMasker() {}

    public static String mask(String text) {
        if (text == null) return null;
        String out = text;
        for (Rule r : RULES) out = r.pattern().matcher(out).replaceAll(r.replacement());
        return out;
    }
}
