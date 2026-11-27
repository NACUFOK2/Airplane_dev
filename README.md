# ✈️ Airplane QnA — 항공사 RAG 챗봇

여러 항공사의 규정(수하물, 예약/변경, 체크인 등)을 검색해 답변하는 로컬 RAG 챗봇 서비스입니다.

- **Backend**: Java 21 · Spring Boot (MVC 레이어드 아키텍처) · `backend/`
- **Frontend**: Next.js (App Router) · 토스 스타일 UI · `frontend/`
- **Dev 환경**: `.devcontainer/` (JDK 21 + Maven + Node 20 일괄 구성)

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
cd backend && mvn spring-boot:run
```
```bash
cd frontend && npm install && npm run dev
```

- 웹: http://localhost:3000 (Next.js 가 `/api/*` 를 http://localhost:8080 으로 프록시)
- API: `GET /api/airlines`, `POST /api/chat` `{ "question": "...", "airlineCode": "KE|OZ|7C|LJ|TW|ALL" }`

### Claude 연결 (선택)
`.env.example` 을 `.env` 로 복사해 `ANTHROPIC_API_KEY` 를 채우거나 셸에서 export 하세요.
키가 없으면 검색된 규정 원문을 보여주는 **추출형 모드**로 동작합니다. (`.env` 는 커밋되지 않습니다)

## 아키텍처

```
[Next.js UI] ──/api──▶ [Controller] ─▶ [Service: Chat / Retrieval / Airline]
                                          │             │
                                   [LlmClient]   [Repository: Knowledge / Airline]
                               Claude | Extractive      │
                                                 knowledge/*.md (항공사별 규정)
```

| 레이어 | 패키지 | 역할 |
|---|---|---|
| Controller | `controller` | REST 엔드포인트, 요청 검증 |
| Service | `service` | RAG 파이프라인 (검색 → 문맥 구성 → 생성) |
| Repository | `repository` | 항공사/규정 문서 적재 |
| Domain / DTO | `domain`, `dto` | 내부 모델 / API 계약 |
| RAG | `rag` | 한글 바이그램 분석기 + BM25 인덱스 |
| LLM | `llm` | Claude(`claude-opus-5-5`) 또는 추출형 답변기 |

> `backend/src/main/resources/knowledge/` 의 항공사 규정은 **데모용 샘플**입니다. 실제 규정으로 교체해 사용하세요.
