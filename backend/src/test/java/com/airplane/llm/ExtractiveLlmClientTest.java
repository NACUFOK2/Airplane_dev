package com.airplane.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.airplane.domain.QaDocument;
import com.airplane.domain.RetrievedDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExtractiveLlmClientTest {

    private static RetrievedDocument doc(String id, String org, String answer, boolean official) {
        return new RetrievedDocument(QaDocument.single(id, "숙박업체", org, "환불은 얼마나 걸리나요?", answer, "", official, null),
                1.0, 0.8, 1, 1);
    }

    private final ExtractiveLlmClient client = new ExtractiveLlmClient();

    @Test
    void ambiguousQuestionIsSplitByOrganization() {
        String a = client.generate("환불은 얼마나 걸려요?",
                List.of(doc("1", "여기어때", "카드 3~5일", true), doc("2", "노랑풍선", "7~14영업일", false),
                        doc("3", "여기어때", "중복", true), doc("4", "아고다", "5~15일", false)),
                List.of(), "전체 기관");
        assertThat(a).contains("기관마다 규정이 달라요", "**여기어때**: 카드 3~5일 [1]", "**노랑풍선**: 7~14영업일 [2]",
                "**아고다**: 5~15일 [4]", "이용하시는 곳을 알려주시면");
        assertThat(a).doesNotContain("중복");
    }

    @Test
    void selectedOrganizationAnswersDirectly() {
        String a = client.generate("환불은 얼마나 걸려요?",
                List.of(doc("1", "여기어때", "카드 3~5일", true), doc("2", "여기어때", "다른 답", true)), List.of(), "여기어때");
        assertThat(a).startsWith("**여기어때** 기준으로 안내드려요.").doesNotContain("기관마다");
    }
}
