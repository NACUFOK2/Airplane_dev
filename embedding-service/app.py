"""질의 임베딩 사이드카 서비스 (로컬 전용, 127.0.0.1 바인딩).

문서 임베딩(data/qa_embeddings.jsonl)은 미리 계산돼 있으므로 이 서비스는
'질문' 한 문장을 같은 모델·같은 규칙으로 벡터화하는 일만 한다.

설치: ./setup.sh
실행: .venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001
"""
import json
import logging
from pathlib import Path
from typing import List, Literal

import numpy as np

from fastapi import FastAPI
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

# 문서 벡터를 만든 모델과 반드시 같아야 한다 (384차원, L2 정규화)
MODEL_NAME = "dragonkue/multilingual-e5-small-ko-v2"
# E5 계열은 비대칭 검색용 접두어로 학습됨: 질의 "query: ", 문서 "passage: "
PREFIX = {"query": "query: ", "passage": "passage: "}

log = logging.getLogger("uvicorn.error")
app = FastAPI(title="Airplane QnA Embedding Service")

# 모델을 다 불러온 뒤에야 포트가 열린다. 처음 실행이면 ~465MB 를 받느라 수 분 걸릴 수 있다.
log.info("임베딩 모델 불러오는 중: %s (처음 실행이면 ~465MB 다운로드)", MODEL_NAME)
model = SentenceTransformer(MODEL_NAME, device="cpu")
# 데이터의 최대 길이는 127 토큰(평균 84) → 256 이면 잘림 없이 연산량을 절반으로 줄인다
model.max_seq_length = 256


def _self_check() -> float:
    """설치된 라이브러리로 만든 벡터가 저장된 문서 벡터와 맞는지 확인 (버전 차이 감지용)."""
    path = Path(__file__).resolve().parent / "data" / "qa_embeddings.jsonl"
    try:
        row = json.loads(path.open(encoding="utf-8").readline())
    except OSError:
        return float("nan")
    v = model.encode([PREFIX["passage"] + row["embed_text"]], normalize_embeddings=True)[0]
    return float(np.dot(v, np.asarray(row["embedding"], dtype=np.float32)))


SELF_CHECK = _self_check()
if SELF_CHECK == SELF_CHECK and SELF_CHECK < 0.95:  # NaN 이 아니고 0.95 미만
    log.warning("저장된 문서 벡터와 일치도가 낮습니다 (%.3f). 라이브러리 버전을 확인하세요.", SELF_CHECK)
log.info("임베딩 모델 준비 완료 (문서 벡터 일치도 %.3f)", SELF_CHECK)


class EmbedRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=32)
    kind: Literal["query", "passage"] = "query"


class EmbedResponse(BaseModel):
    model: str
    dimension: int
    vectors: List[List[float]]


@app.get("/health")
def health():
    return {"status": "UP", "model": MODEL_NAME, "dimension": model.get_sentence_embedding_dimension(),
            "selfCheck": round(SELF_CHECK, 3) if SELF_CHECK == SELF_CHECK else None}


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    texts = [PREFIX[req.kind] + t.strip()[:1000] for t in req.texts]
    vecs = model.encode(texts, normalize_embeddings=True, batch_size=32, convert_to_numpy=True)
    return EmbedResponse(model=MODEL_NAME, dimension=vecs.shape[1], vectors=vecs.round(6).tolist())
