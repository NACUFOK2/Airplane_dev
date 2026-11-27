"""질의 임베딩 사이드카 서비스 (로컬 전용, 127.0.0.1 바인딩).

문서 임베딩(data/qa_embeddings.jsonl)은 미리 계산돼 있으므로 이 서비스는
'질문' 한 문장을 같은 모델·같은 규칙으로 벡터화하는 일만 한다.

실행: .venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001
"""
from typing import List, Literal

from fastapi import FastAPI
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

# 문서 벡터를 만든 모델과 반드시 같아야 한다 (384차원, L2 정규화)
MODEL_NAME = "dragonkue/multilingual-e5-small-ko-v2"
# E5 계열은 비대칭 검색용 접두어로 학습됨: 질의 "query: ", 문서 "passage: "
PREFIX = {"query": "query: ", "passage": "passage: "}

app = FastAPI(title="Airplane QnA Embedding Service")
model = SentenceTransformer(MODEL_NAME, device="cpu")
# 데이터의 최대 길이는 127 토큰(평균 84) → 256 이면 잘림 없이 연산량을 절반으로 줄인다
model.max_seq_length = 256


class EmbedRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=32)
    kind: Literal["query", "passage"] = "query"


class EmbedResponse(BaseModel):
    model: str
    dimension: int
    vectors: List[List[float]]


@app.get("/health")
def health():
    return {"status": "UP", "model": MODEL_NAME, "dimension": model.get_sentence_embedding_dimension()}


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    texts = [PREFIX[req.kind] + t.strip()[:1000] for t in req.texts]
    vecs = model.encode(texts, normalize_embeddings=True, batch_size=32, convert_to_numpy=True)
    return EmbedResponse(model=MODEL_NAME, dimension=vecs.shape[1], vectors=vecs.round(6).tolist())
