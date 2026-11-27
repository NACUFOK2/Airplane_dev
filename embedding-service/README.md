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
