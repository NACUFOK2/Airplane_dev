package com.airplane.feedback;

import java.util.List;

/**
 * 로그 한 줄. 답변 본문은 저장하지 않는다(질문·출처 id 만으로 데이터 공백을 찾기에 충분).
 * @param type    feedback | out_of_scope
 * @param rating  up | down (out_of_scope 이면 null)
 */
public record FeedbackEvent(String timestamp, String type, String rating, String question, String orgCode,
                            String mode, List<String> sourceIds) {}
