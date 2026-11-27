package com.airplane.domain;

import java.util.List;

/**
 * 검색 단위: 원문 Q&A 한 건과 그 질문 변형들.
 *
 * @param id         원문 id (parent_id, 예: 항공-045)
 * @param orgType    기관 유형 (항공사 / 여행사 / 숙박업체)
 * @param question   원문 질문
 * @param variants   질문 변형 (원문·구어체·키워드·다른 표현). BM25 색인에 모두 쓴다
 * @param official   출처가 해당 기관의 공식 도메인인지 (블로그·정리글이면 false)
 * @param embeddings 변형별 384차원 L2 정규화 벡터. Dense 점수는 이 중 최댓값
 */
public record QaDocument(String id, String orgType, String org, String question, List<String> variants,
                         String answer, String source, boolean official, float[][] embeddings) {

    /** 변형이 없는 단일 문서 (테스트·단순 데이터용) */
    public static QaDocument single(String id, String orgType, String org, String question, String answer,
                                    String source, boolean official, float[] embedding) {
        return new QaDocument(id, orgType, org, question, List.of(question), answer, source, official,
                embedding == null ? new float[0][] : new float[][]{embedding});
    }
}
