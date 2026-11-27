# embedding-service

질문 문장을 `dragonkue/multilingual-e5-small-ko-v2` 로 임베딩하는 로컬 사이드카입니다.
Spring Boot 백엔드가 `POST http://127.0.0.1:8001/embed` 로 호출합니다. 꺼져 있으면 백엔드는 BM25 단독 검색으로 자동 전환됩니다.

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001
```

- `data/qa_embeddings.jsonl` : 83개 Q&A + 384차원 문서 벡터 (원본 데이터)
- `eval/evaluate.py` : 검색 파라미터 선정용 평가 스크립트 (`.venv/bin/python eval/evaluate.py`)
- `eval/analyze_data.py` : 여행사/항공사 주제 분류 및 데이터 분석 (`python3 eval/analyze_data.py`)

## 데이터 갱신

Q&A 원본(XLSX 또는 CSV)을 고친 뒤 인덱스를 다시 만듭니다.

```bash
.venv/bin/python scripts/build_index.py "../Q&A_정리.xlsx" --dry-run   # 변경 내역만 확인
.venv/bin/python scripts/build_index.py "../Q&A_정리.xlsx"             # 인덱스 생성
```

- XLSX 시트명은 `<유형>_Q&A`(예: `항공사_Q&A`), 헤더는 `기관명 | 질문 | 답변 | 출처` 순서입니다.
- (기관, 질문)이 같은 행은 id 와 벡터를 그대로 유지하고, 새로 생기거나 내용이 바뀐 행만 임베딩합니다.
- 빈 칸, URL 이 아닌 출처, 중복 질문이 있으면 아무것도 쓰지 않고 멈춥니다.
- 결과는 `data/` 와 `backend/src/main/resources/data/` 두 곳에 같은 내용으로 저장됩니다 (백엔드 테스트 `IndexSyncTest` 가 일치 여부를 검사).
- 새 기관이 생기면 `backend/src/main/resources/application.yml` 의 `rag.organizations` 에도 추가하세요.
