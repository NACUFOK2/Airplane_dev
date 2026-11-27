package com.airplane.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 형태소 분석기 없이 한국어를 검색하기 위한 경량 분석기.
 * 어절 전체 + 한글 문자 바이그램을 색인어로 사용해 조사/어미 변화에 강하게 만든다.
 */
public final class TextAnalyzer {

    private static final Set<String> STOPWORDS = Set.of(
            "은", "는", "이", "가", "을", "를", "에", "의", "도", "로", "으로", "와", "과",
            "어떻게", "얼마", "얼마나", "있나요", "되나요", "하나요", "인가요", "알려줘", "알려주세요", "궁금해요",
            "the", "a", "an", "is", "are", "of", "to", "and", "or", "for");

    private TextAnalyzer() {}

    public static List<String> analyze(String text) {
        List<String> terms = new ArrayList<>();
        if (text == null) return terms;
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.isBlank() || STOPWORDS.contains(word)) continue;
            terms.add(word);
            if (containsHangul(word) && word.length() >= 2) {
                for (int i = 0; i < word.length() - 1; i++) {
                    terms.add(word.substring(i, i + 2));
                }
            }
        }
        return terms;
    }

    private static boolean containsHangul(String s) {
        return s.codePoints().anyMatch(cp -> cp >= 0xAC00 && cp <= 0xD7A3);
    }
}
