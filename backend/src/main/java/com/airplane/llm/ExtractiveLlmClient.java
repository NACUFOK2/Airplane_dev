package com.airplane.llm;

import com.airplane.domain.ChatTurn;
import com.airplane.domain.RetrievedDocument;
import java.util.List;

/** API 키 없이도 동작하는 추출형 답변기: 1위 Q&A 답변을 그대로, 2위는 관련 질문으로 안내한다. */
public class ExtractiveLlmClient implements LlmClient {

    @Override
    public String generate(String question, List<RetrievedDocument> context, List<ChatTurn> history, String scopeLabel) {
        if (context.isEmpty()) {
            return "제공된 자료에서 관련 내용을 찾지 못했어요. 질문을 조금 더 구체적으로 해주시거나 고객센터에 문의해 주세요.";
        }
        var top = context.get(0).doc();
        StringBuilder sb = new StringBuilder()
                .append("**").append(top.org()).append("** 기준으로 안내드려요.\n\n")
                .append(top.answer()).append(" [1]\n");
        if (context.size() > 1) {
            sb.append("\n함께 보면 좋은 질문\n");
            for (int i = 1; i < Math.min(3, context.size()); i++) {
                var d = context.get(i).doc();
                sb.append("- ").append(d.org()).append(" · ").append(d.question()).append(" [").append(i + 1).append("]\n");
            }
        }
        sb.append("\n_AI 요약 없이 가장 관련 높은 Q&A 원문을 보여드리고 있어요._");
        return sb.toString().strip();
    }

    @Override
    public String mode() {
        return "extractive";
    }
}
