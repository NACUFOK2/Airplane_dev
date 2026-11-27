package com.airplane.service;

import com.airplane.client.EmbeddingClient;
import com.airplane.config.RagProperties;
import com.airplane.domain.Organization;
import com.airplane.domain.QaDocument;
import com.airplane.domain.RetrievalResult;
import com.airplane.domain.RetrievedDocument;
import com.airplane.rag.Bm25Index;
import com.airplane.rag.DenseIndex;
import com.airplane.rag.RankFusion;
import com.airplane.repository.OrganizationRepository;
import com.airplane.repository.QaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 하이브리드 검색: (1) 기관 필터 → (2) Dense·BM25 각각 순위화 → (3) RRF 융합 → (4) 범위 밖 판정.
 */
@Service
public class RetrievalService {

    private final List<QaDocument> docs;
    private final Bm25Index bm25;
    private final DenseIndex dense;
    private final EmbeddingClient embeddingClient;
    private final OrganizationRepository organizationRepository;
    private final RagProperties.Retrieval cfg;

    public RetrievalService(QaRepository qaRepository, EmbeddingClient embeddingClient,
                            OrganizationRepository organizationRepository, RagProperties properties) {
        this.docs = qaRepository.findAll();
        this.bm25 = new Bm25Index(docs);
        this.dense = new DenseIndex(docs);
        this.embeddingClient = embeddingClient;
        this.organizationRepository = organizationRepository;
        this.cfg = properties.retrieval();
    }

    /** @param selectedOrg 사용자가 고른 기관 (null 이면 전체) */
    public RetrievalResult retrieve(String query, Organization selectedOrg) {
        return retrieve(query, selectedOrg, null);
    }

    /**
     * 멀티턴 검색. 후속 질문은 직전 사용자 질문으로 보완한다 (근거: docs/RAG_PARAMETERS.md '후속 질문').
     * @param previousQuestion 직전 사용자 질문 (없으면 null)
     */
    public RetrievalResult retrieve(String question, Organization selectedOrg, String previousQuestion) {
        boolean hasPrev = previousQuestion != null && !previousQuestion.isBlank();
        String query = hasPrev && isShortFollowup(question) ? previousQuestion + " " + question : question;
        String orgFilter = resolveOrgFilter(question, selectedOrg);
        if (orgFilter == null && hasPrev && cfg.followupCarryOrg() && cfg.autoOrgFilter()) {
            orgFilter = organizationRepository.detectInText(previousQuestion).map(Organization::name).orElse(null);
        }
        return search(query, orgFilter);
    }

    boolean isShortFollowup(String question) {
        return question.replaceAll("\\s", "").length() < cfg.followupShortChars();
    }

    private RetrievalResult search(String query, String orgFilter) {
        boolean[] allowed = new boolean[docs.size()];
        for (int i = 0; i < docs.size(); i++) allowed[i] = orgFilter == null || docs.get(i).org().equals(orgFilter);

        int[] bm25Rank = RankFusion.rank(bm25.scores(query), allowed, cfg.candidatePool(), true);
        Optional<float[]> qv = embeddingClient.embedQuery(query);

        if (qv.isEmpty()) {
            // 임베딩 서비스 장애: BM25 단독. BM25 점수는 OOD 판별력이 없어 범위 판정은 LLM 프롬프트에 맡긴다.
            List<RetrievedDocument> out = new ArrayList<>();
            for (int r = 0; r < Math.min(cfg.topK(), bm25Rank.length); r++) {
                out.add(new RetrievedDocument(docs.get(bm25Rank[r]), 1.0 / (cfg.rrfK() + r + 1), Double.NaN, -1, r + 1));
            }
            return new RetrievalResult(out, "bm25", false, orgFilter, Double.NaN);
        }

        double[] denseScores = dense.scores(qv.get());
        int[] denseRank = RankFusion.rank(denseScores, allowed, cfg.candidatePool(), false);
        double topDense = denseRank.length == 0 ? 0 : denseScores[denseRank[0]];

        double[] fused = RankFusion.rrf(List.of(denseRank, bm25Rank), docs.size(), cfg.rrfK());
        int[] fusedRank = RankFusion.rank(fused, allowed, cfg.topK(), true);

        List<RetrievedDocument> out = new ArrayList<>();
        for (int i : fusedRank) {
            out.add(new RetrievedDocument(docs.get(i), fused[i], denseScores[i],
                    RankFusion.positionOf(denseRank, i), RankFusion.positionOf(bm25Rank, i)));
        }
        boolean outOfScope = topDense < cfg.denseMinScore();
        return new RetrievalResult(out, "hybrid", outOfScope, orgFilter, topDense);
    }

    private String resolveOrgFilter(String query, Organization selectedOrg) {
        if (selectedOrg != null) return selectedOrg.name();
        if (!cfg.autoOrgFilter()) return null;
        return organizationRepository.detectInText(query).map(Organization::name).orElse(null);
    }
}
