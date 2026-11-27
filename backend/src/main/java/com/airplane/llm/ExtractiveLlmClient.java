package com.airplane.llm;

import com.airplane.domain.ChatTurn;
import com.airplane.domain.QaDocument;
import com.airplane.domain.RetrievedDocument;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * API 키 없이도 동작하는 추출형 답변기.
 * - 기관이 정해진 질문: 1위 문답의 답변을 그대로, 2~3위는 관련 질문으로 안내
 * - 기관을 밝히지 않았고 상위 결과가 여러 기관에 걸친 질문("환불은 얼마나 걸려요?"): 기관별로 나눠 보여주고 이용처를 묻는다
 */
public class ExtractiveLlmClient implements LlmClient {

    static final String ALL_SCOPE = "전체 기관";

    @Override
    public LlmAnswer generate(String question, List<RetrievedDocument> context, List<ChatTurn> history, String scopeLabel) {
        return new LlmAnswer(text(context, scopeLabel), LlmAnswer.EXTRACTIVE);
    }

    String text(List<RetrievedDocument> context, String scopeLabel) {
        if (context.isEmpty()) {
            return "제공된 자료에서 관련 내용을 찾지 못했어요. 질문을 조금 더 구체적으로 해주시거나 고객센터에 문의해 주세요.";
        }
        StringBuilder sb = ALL_SCOPE.equals(scopeLabel) && spansMultipleOrgs(context)
                ? perOrganization(context) : singleOrganization(context);
        sb.append("\n_자동 요약 없이 가장 관련 높은 문답 원문을 보여드리고 있어요._");
        return sb.toString().strip();
    }

    private static boolean spansMultipleOrgs(List<RetrievedDocument> context) {
        return context.size() > 1 && !context.get(0).doc().org().equals(context.get(1).doc().org());
    }

    private static StringBuilder singleOrganization(List<RetrievedDocument> context) {
        QaDocument top = context.get(0).doc();
        StringBuilder sb = new StringBuilder()
                .append("**").append(top.org()).append("** 기준으로 안내드려요.\n\n")
                .append(top.answer()).append(" [1]\n");
        if (context.size() > 1) {
            sb.append("\n함께 보면 좋은 질문\n");
            for (int i = 1; i < Math.min(3, context.size()); i++) {
                QaDocument d = context.get(i).doc();
                sb.append("- ").append(d.org()).append(" · ").append(d.question()).append(" [").append(i + 1).append("]\n");
            }
        }
        if (!top.official()) {
            sb.append("\n이 답변은 비공식 정리 자료 기준이에요. ").append(top.org()).append(" 공식 홈페이지에서 한 번 더 확인해 주세요.\n");
        }
        return sb;
    }

    private static StringBuilder perOrganization(List<RetrievedDocument> context) {
        StringBuilder sb = new StringBuilder("기관마다 규정이 달라요. 자료에 있는 곳부터 정리해 드릴게요.\n\n");
        Set<String> seen = new LinkedHashSet<>();
        boolean anyUnofficial = false;
        for (int i = 0; i < context.size() && seen.size() < 3; i++) {
            QaDocument d = context.get(i).doc();
            if (!seen.add(d.org())) continue;
            anyUnofficial |= !d.official();
            sb.append("- **").append(d.org()).append("**: ").append(d.answer()).append(" [").append(i + 1).append("]\n");
        }
        sb.append("\n이용하시는 곳을 알려주시면 더 정확히 안내할게요.\n");
        if (anyUnofficial) sb.append("일부는 비공식 정리 자료 기준이라 공식 홈페이지에서 한 번 더 확인해 주세요.\n");
        return sb;
    }
}
