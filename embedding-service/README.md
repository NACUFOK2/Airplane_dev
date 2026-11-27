# embedding-service

질문 문장을 `dragonkue/multilingual-e5-small-ko-v2` 로 임베딩하는 로컬 사이드카입니다.
Spring Boot 백엔드가 `POST http://127.0.0.1:8001/embed` 로 호출합니다. 꺼져 있으면 백엔드는 BM25 단독 검색으로 자동 전환됩니다.

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001
```

- `data/qa_embeddings.jsonl` : 문답 원문 178건 × 질문 변형 4종 = 712행 + 384차원 벡터 (v2 원본)
- `data/v1/qa_embeddings.jsonl` : 이전 버전 83건 (v1 평가 기록 재현용)
- `eval/evaluate_v2.py` : 현재 검색 파라미터 평가 (`.venv/bin/python eval/evaluate_v2.py`)
- `eval/evaluate.py`, `eval/evaluate_followup.py` : v1 기록 재현
- `eval/analyze_data.py` : 항공사·여행사·숙박업체 주제 분류 및 데이터 분석 (`python3 eval/analyze_data.py`)

## 데이터 갱신

Q&A 원본(XLSX 또는 CSV)을 고친 뒤 인덱스를 다시 만듭니다.

```bash
.venv/bin/python scripts/build_index.py ../v2_QA_augmented.xlsx --dry-run   # 변경 내역만 확인
.venv/bin/python scripts/build_index.py ../v2_QA_augmented.xlsx             # 인덱스 생성
```

- 한 행 = 질문 변형 1개. 열: `id, parent_id, variant, sheet, org, question, original_question, answer, source` (CSV 는 `category` 포함 가능)
- `variant` 는 `original`(원문)·`casual`(구어체)·`keyword`(키워드)·`rephrase`(다른 표현). 원문마다 `original` 이 정확히 1개여야 합니다.
- id 와 내용이 같은 행은 벡터를 그대로 유지하고, 새로 생기거나 바뀐 행만 임베딩합니다.
- 빈 칸, URL 이 아닌 출처, id 중복, 같은 원문의 변형끼리 답변·출처가 다른 경우 아무것도 쓰지 않고 멈춥니다.
- 결과는 `data/` 와 `backend/src/main/resources/data/` 두 곳에 같은 내용으로 저장됩니다 (백엔드 테스트 `IndexSyncTest` 가 일치 여부를 검사).
- 새 기관이 생기면 `backend/src/main/resources/application.yml` 의 `rag.organizations` 에도 추가하세요.
