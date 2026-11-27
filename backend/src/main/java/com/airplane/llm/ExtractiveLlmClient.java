package com.airplane.llm;

import com.airplane.domain.ChatTurn;
import com.airplane.domain.RetrievedChunk;
import java.util.List;

/** API 키 없이도 동작하는 추출형 답변기: 상위 검색 결과를 그대로 정리해 보여준다. */
public class ExtractiveLlmClient implements LlmClient {

    @Override
    public String generate(String question, List<RetrievedChunk> context, List<ChatTurn> history, String airlineLabel) {
        if (context.isEmpty()) {
            return "제공된 규정에서 관련 내용을 찾지 못했어요. 질문을 조금 더 구체적으로 해주시거나 항공사 고객센터에 문의해 주세요.";
        }
        StringBuilder sb = new StringBuilder("관련 규정을 찾았어요.\n\n");
        int limit = Math.min(2, context.size());
        for (int i = 0; i < limit; i++) {
            var c = context.get(i).chunk();
            sb.append("**").append(c.title()).append("** [").append(i + 1).append("]\n")
              .append(c.content()).append("\n\n");
        }
        sb.append("_AI 요약 없이 검색된 원문을 보여드리고 있어요._");
        return sb.toString().strip();
    }

    @Override
    public String mode() {
        return "extractive";
    }
}
