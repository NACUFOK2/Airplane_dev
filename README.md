# ✈️ Airplane 여행 상담 챗봇 — 항공사·여행사·숙박업체 RAG

항공사 10곳·여행사 4곳·숙박업체 6곳의 문답 178건(질문 변형 포함 712개)을
하이브리드 검색(임베딩 + BM25)해 답변하는 로컬 여행 상담 챗봇입니다.

- 항공사: 대한항공, 제주항공, 에어로케이, 델타항공, 유나이티드항공, 에미레이트항공, 싱가포르항공, 캐세이퍼시픽, 루프트한자, ANA(전일본공수)
- 여행사: 하나투어, 모두투어, 노랑풍선, 참좋은여행
- 숙박업체: 아고다, 호텔스컴바인, 트립닷컴, 부킹닷컴, 야놀자, 여기어때

- **Backend**: Java 21 · Spring Boot (MVC 레이어드 아키텍처) · `backend/`
- **Frontend**: Next.js (App Router) · 토스 스타일 UI · `frontend/`
- **Embedding**: Python FastAPI · `dragonkue/multilingual-e5-small-ko-v2` · `embedding-service/`
- **Dev 환경**: `.devcontainer/` (JDK 21 + Maven + Node 20 + Python 3.11 일괄 구성)
- **RAG 구현 명세**: [docs/RAG_SPEC.md](docs/RAG_SPEC.md)
- **파라미터 설계 근거**: [docs/RAG_PARAMETERS.md](docs/RAG_PARAMETERS.md)
- **데이터 분석 (항공사·여행사·숙박업체)**: [docs/DATA_ANALYSIS.md](docs/DATA_ANALYSIS.md)

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

### 2) 한 번에 실행 (컨테이너 없이, macOS·Linux)
```bash
./.devcontainer/run-local.sh
```
- 임베딩 서비스 가상환경이 없으면 `embedding-service/setup.sh` 로 **자동 설치**한 뒤 세 서버를 띄웁니다.
- 필요: JDK 21 · Maven · Node 20 · **Python 3.9~3.14** (인텔 맥은 3.9~3.12)
- 처음 실행은 임베딩 모델(~465MB) 다운로드로 수 분 걸릴 수 있습니다.

### 3) 서버별로 직접 실행
```bash
cd embedding-service && ./setup.sh && .venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001
```
```bash
cd backend && mvn spring-boot:run
```
```bash
cd frontend && npm install && npm run dev
```

**Windows (PowerShell)** — `setup.sh` 대신:
```powershell
cd embedding-service; py -3.12 -m venv .venv; .venv\Scripts\pip install -r requirements.txt; .venv\Scripts\uvicorn app:app --host 127.0.0.1 --port 8001
```

### 상태 확인
```bash
for u in http://127.0.0.1:8001/health http://localhost:8080/api/health http://localhost:3000; do printf "%-35s " "$u"; curl -s -o /dev/null -m 3 -w "%{http_code}\n" "$u"; done
```
세 줄 모두 `200` 이면 정상입니다. 임베딩 `/health` 의 `selfCheck` 가 0.95 이상이면 설치된 라이브러리가 문서 벡터와 맞습니다.

### 임베딩 서버가 안 뜰 때
| 증상 | 원인 | 해결 |
|---|---|---|
| `.venv/bin/uvicorn: No such file` | 가상환경 미설치 | `cd embedding-service && ./setup.sh` |
| `No matching distribution found for torch…/numpy…` | 지원하지 않는 Python 버전 | Python 3.11~3.12 설치 후 `./setup.sh --recreate` (예: `brew install python@3.12`) |
| 인텔 맥에서 torch 설치 실패 | torch 가 인텔 맥은 2.2 까지만 지원 | Python 3.12 이하로 `./setup.sh --recreate` |
| 실행 후 한참 응답 없음 | 첫 실행 모델 다운로드(~465MB) 중 | 로그에 `임베딩 모델 준비 완료` 가 뜰 때까지 대기. 회사망이면 huggingface.co 접속 허용 필요 |
| `Address already in use` | 8001 포트를 다른 프로세스가 사용 | `lsof -iTCP:8001 -sTCP:LISTEN` 으로 확인 후 종료 |
| `selfCheck` 0.95 미만 경고 | 라이브러리 버전 차이가 큼 | `./setup.sh --recreate`, 계속되면 이슈로 공유 |

임베딩 서버가 꺼져 있어도 백엔드는 키워드 검색(BM25)만으로 계속 동작합니다 (정확도 소폭 하락).

- 웹: http://localhost:3000 (Next.js 가 `/api/*` 를 http://localhost:8080 으로 프록시)
- API: `GET /api/organizations`, `POST /api/chat` `{ "question": "...", "orgCode": "koreanair|delta|booking|…|ALL" }` (기관 코드는 `application.yml` 의 `rag.organizations`)
- 임베딩 서비스가 꺼져 있으면 BM25 키워드 검색만으로 자동 전환됩니다.

### 답변 생성 모델 연결 (선택)
`.env.example` 을 `.env` 로 복사해 키를 하나 채우면 됩니다. `.env` 는 커밋되지 않습니다.

| 키 | 사용 모델 |
|---|---|
| `ANTHROPIC_API_KEY` | Claude (`claude-opus-5-5`) — 둘 다 있으면 이쪽 우선 |
| `OPENAI_API_KEY` | OpenAI (`gpt-5.6-sol`, `OPENAI_MODEL` 로 변경 가능) |
| (없음) | 검색된 문답 원문을 보여주는 **추출형 모드** |

> ⚠️ 키는 채팅·이슈·커밋에 붙여넣지 마세요. 노출됐다면 발급처에서 즉시 폐기(revoke)하고 새로 발급하세요.

## 주요 기능

| 기능 | 설명 |
|---|---|
| 하이브리드 검색 | 질문 변형 4종을 활용한 임베딩(의미) + BM25(키워드)를 RRF 로 융합, 질문 속 기관명·별칭 자동 필터 |
| 후속 질문 | "기내는요?"처럼 짧은 후속 질문은 직전 질문의 기관·주제로 보완해 검색 |
| 범위 밖 차단 | 유사도가 낮은 질문과 자료에 없는 기관(아시아나·KTX 등) 질문은 LLM 을 부르지 않고 안내 |
| 기관 미지정 질문 | "환불은 얼마나 걸려요?"처럼 여러 기관에 해당하면 기관별로 나눠 답하고 이용처를 되물음 |
| 출처 신뢰도 | 출처가 기관 공식 홈페이지인지 판별해 `공식 출처`/`참고 출처` 배지와 확인 안내 표시 |
| 피드백 리포트 | 👍/👎 와 답하지 못한 질문을 개인정보 마스킹 후 로컬 기록 → http://localhost:3000/insights |
| 데이터 갱신 | `embedding-service/scripts/build_index.py` 로 v2 XLSX/CSV 에서 인덱스 재생성 (바뀐 행만 임베딩) |

> 피드백 로그는 `backend/chat-logs/` 에 저장되며 커밋되지 않습니다. `/api/insights` 는 같은 PC(루프백)에서만 열립니다.

## 아키텍처

```
[Next.js UI] ──/api──▶ [Controller] ─▶ [Service: Chat / Retrieval / Organization]
                                          │            │              │
                                   [LlmClient]   [EmbeddingClient]  [Repository: Qa / Organization]
                               Claude | Extractive    │                     │
                                          embedding-service:8001   data/qa_embeddings.jsonl (원문 178건 × 변형 4, 384차원)
```

| 레이어 | 패키지 | 역할 |
|---|---|---|
| Controller | `controller` | REST 엔드포인트, 요청 검증 |
| Service | `service` | RAG 파이프라인 (검색 → 문맥 구성 → 생성) |
| Repository | `repository` | 문답·임베딩 / 기관 정보 적재 |
| Client | `client` | 질의 임베딩 사이드카 호출 (실패 시 BM25 폴백) |
| Domain / DTO | `domain`, `dto` | 내부 모델 / API 계약 |
| RAG | `rag` | BM25 · Dense(코사인) 인덱스 · RRF 융합 |
| LLM | `llm` | Claude(`claude-opus-5-5`) 또는 추출형 답변기 |

> 문답 데이터는 각 기관 공식 페이지·정리 글에서 수집한 것으로 시점이 지나면 달라질 수 있습니다. 답변의 '원문 보기' 링크로 최신 규정을 확인하세요.
