package com.airplane.llm;

import com.airplane.domain.ChatTurn;
import com.airplane.domain.RetrievedDocument;
import java.util.List;

/** 검색된 Q&A 문맥으로 답변을 생성하는 LLM 추상화. */
public interface LlmClient {

    String generate(String question, List<RetrievedDocument> context, List<ChatTurn> history, String scopeLabel);

    /** 응답 메타데이터용 모드 이름 (claude / extractive) */
    String mode();
}
