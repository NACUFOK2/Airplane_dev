package com.airplane.domain;

import java.util.List;

/**
 * @param mode             hybrid | bm25 (임베딩 서비스 장애 시)
 * @param outOfScope       데이터 범위 밖으로 판정 (유사도 미달 또는 자료에 없는 기관)
 * @param orgFilter        적용된 기관 필터 (없으면 null)
 * @param unsupportedOrg   질문에 나온 '자료에 없는 기관' 이름 (없으면 null)
 */
public record RetrievalResult(List<RetrievedDocument> documents, String mode, boolean outOfScope, String orgFilter,
                              double topDenseScore, String unsupportedOrg) {

    public RetrievalResult(List<RetrievedDocument> documents, String mode, boolean outOfScope, String orgFilter, double topDenseScore) {
        this(documents, mode, outOfScope, orgFilter, topDenseScore, null);
    }
}
