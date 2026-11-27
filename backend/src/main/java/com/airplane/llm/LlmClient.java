package com.airplane.llm;

import com.airplane.domain.ChatTurn;
import com.airplane.domain.RetrievedDocument;
import java.util.List;

/** 검색된 문답 문맥으로 답변을 생성하는 LLM 추상화. */
public interface LlmClient {

    /** 답변과 함께 실제로 사용된 방식(mode)을 돌려준다. 대체(fallback)가 일어나면 mode 에 반영된다. */
    LlmAnswer generate(String question, List<RetrievedDocument> context, List<ChatTurn> history, String scopeLabel);
}
