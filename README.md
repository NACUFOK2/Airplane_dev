# ✈️ Airplane QnA — 여행사·항공사 RAG 챗봇

여행사 4곳(하나투어·모두투어·노랑풍선·참좋은여행)과 항공사 3곳(대한항공·제주항공·에어로케이)의
Q&A 83건을 하이브리드 검색(임베딩 + BM25)해 답변하는 로컬 RAG 챗봇입니다.

- **Backend**: Java 21 · Spring Boot (MVC 레이어드 아키텍처) · `backend/`
- **Frontend**: Next.js (App Router) · 토스 스타일 UI · `frontend/`
- **Embedding**: Python FastAPI · `dragonkue/multilingual-e5-small-ko-v2` · `embedding-service/`
- **Dev 환경**: `.devcontainer/` (JDK 21 + Maven + Node 20 + Python 3.11 일괄 구성)
- **파라미터 설계 근거**: [docs/RAG_PARAMETERS.md](docs/RAG_PARAMETERS.md)
- **데이터 분석 (여행사 vs 항공사)**: [docs/DATA_ANALYSIS.md](docs/DATA_ANALYSIS.md)

> 배포하지 않으며 로컬에서만 실행합니다. 자세한 실행 방법은 아래 섹션 참고.

## 브랜치 / 커밋 규칙
[docs/COMMIT_CONVENTION.md](docs/COMMIT_CONVENTION.md) 참고.

## 보안
AI 키·토큰·개인정보·결제 관련 파일은 커밋 금지입니다. 클론 후 아래를 실행하세요.

```bash
git config core.hooksPath .githooks
```

## 로컬 실행

### 1) VSCode + Dev Container (권장)
VSCode 에서 폴더를 열고 **Reopen in Container** → JDK 21 / Maven / Node 20 이 자동 구성됩니다.

```bash
./.devcontainer/run-local.sh
```

### 2) 직접 실행
```bash
cd embedding-service && python3 -m venv .venv && .venv/bin/pip install -r requirements.txt && .venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001
```
```bash
cd backend && mvn spring-boot:run
```
```bash
cd frontend && npm install && npm run dev
```

- 웹: http://localhost:3000 (Next.js 가 `/api/*` 를 http://localhost:8080 으로 프록시)
- API: `GET /api/organizations`, `POST /api/chat` `{ "question": "...", "orgCode": "hanatour|modetour|ybtour|verygoodtour|koreanair|jejuair|aerok|ALL" }`
- 임베딩 서비스가 꺼져 있으면 BM25 키워드 검색만으로 자동 전환됩니다.

### Claude 연결 (선택)
`.env.example` 을 `.env` 로 복사해 `ANTHROPIC_API_KEY` 를 채우거나 셸에서 export 하세요.
키가 없으면 검색된 규정 원문을 보여주는 **추출형 모드**로 동작합니다. (`.env` 는 커밋되지 않습니다)

## 주요 기능

| 기능 | 설명 |
|---|---|
| 하이브리드 검색 | 임베딩(의미) + BM25(키워드)를 RRF 로 융합, 질문 속 기관명 자동 필터 |
| 후속 질문 | "기내는요?"처럼 짧은 후속 질문은 직전 질문의 기관·주제로 보완해 검색 |
| 범위 밖 차단 | 유사도가 낮은 질문은 LLM 을 부르지 않고 안내 (환각·비용 절감) |
| 출처 신뢰도 | 출처가 기관 공식 홈페이지인지 판별해 `공식 출처`/`참고 출처` 배지와 확인 안내 표시 |
| 피드백 리포트 | 👍/👎 와 답하지 못한 질문을 개인정보 마스킹 후 로컬 기록 → http://localhost:3000/insights |
| 데이터 갱신 | `embedding-service/scripts/build_index.py` 로 XLSX/CSV 에서 인덱스 재생성 (바뀐 행만 임베딩) |

> 피드백 로그는 `backend/chat-logs/` 에 저장되며 커밋되지 않습니다. `/api/insights` 는 같은 PC(루프백)에서만 열립니다.

## 아키텍처

```
[Next.js UI] ──/api──▶ [Controller] ─▶ [Service: Chat / Retrieval / Organization]
                                          │            │              │
                                   [LlmClient]   [EmbeddingClient]  [Repository: Qa / Organization]
                               Claude | Extractive    │                     │
                                          embedding-service:8001   data/qa_embeddings.jsonl (83건, 384차원)
```

| 레이어 | 패키지 | 역할 |
|---|---|---|
| Controller | `controller` | REST 엔드포인트, 요청 검증 |
| Service | `service` | RAG 파이프라인 (검색 → 문맥 구성 → 생성) |
| Repository | `repository` | Q&A·임베딩 / 기관 정보 적재 |
| Client | `client` | 질의 임베딩 사이드카 호출 (실패 시 BM25 폴백) |
| Domain / DTO | `domain`, `dto` | 내부 모델 / API 계약 |
| RAG | `rag` | BM25 · Dense(코사인) 인덱스 · RRF 융합 |
| LLM | `llm` | Claude(`claude-opus-5-5`) 또는 추출형 답변기 |

> Q&A 데이터는 각 기관 공식 페이지·정리 글에서 수집한 것으로 시점이 지나면 달라질 수 있습니다. 답변의 '원문 보기' 링크로 최신 규정을 확인하세요.
