"""후속 질문(멀티턴) 검색 전략 비교. python eval/evaluate_followup.py

직전 사용자 질문(prev)이 있을 때 검색 질의를 어떻게 만들지 비교한다.
  S0 현재 질문만
  S1 기관 이어받기: 현재 질문에 기관명이 없으면 직전 질문의 기관으로 필터
  S2 S1 + 짧은 질문(N자 미만)이면 '직전 질문 + 현재 질문'으로 검색
  S3 S1 + 항상 '직전 질문 + 현재 질문'으로 검색
단일 질문 평가셋(eval_set.jsonl)에는 prev 가 없으므로 어느 전략이든 결과가 같다.
"""
import json, sys
from pathlib import Path
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import evaluate as ev  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
cases = [json.loads(l) for l in open(ROOT / "eval/followup_set.jsonl", encoding="utf-8")]
model = ev.SentenceTransformer(ev.MODEL)
E = np.array([r["embedding"] for r in ev.rows], dtype=np.float32)
bm = ev.BM25(ev.rows)


def search(query, org):
    mask = np.array([r["org"] == org for r in ev.rows]) if org else None
    qv = model.encode(["query: " + query], normalize_embeddings=True)[0]
    return ev.ranks_of(ev.rrf([ev.ranks_of(E @ qv, mask), ev.ranks_of(bm.scores(query), mask)], 60), mask)


def strategy(name, n=None):
    def run(c):
        org = ev.detect_org(c["q"])
        if name != "S0" and org is None:
            org = ev.detect_org(c["prev"])
        query = c["q"]
        if name == "S3" or (name == "S2" and len(c["q"].replace(" ", "")) < n):
            query = c["prev"] + " " + c["q"]
        return search(query, org)
    return run


def score(fn):
    h1 = h3 = mrr = 0
    for c in cases:
        order = [ev.ids[i] for i in fn(c)]
        pos = min((order.index(g) for g in c["gold"] if g in order), default=999)
        h1 += pos < 1; h3 += pos < 3; mrr += 1 / (pos + 1) if pos < 999 else 0
    n = len(cases)
    return f"Hit@1 {h1/n:.3f}  Hit@3 {h3/n:.3f}  MRR {mrr/n:.3f}"


if __name__ == "__main__":
    print(f"후속 질문 {len(cases)}개")
    print("S0 현재 질문만            ", score(strategy("S0")))
    print("S1 기관 이어받기          ", score(strategy("S1")))
    for n in (8, 12, 16, 20):
        print(f"S2 + 짧으면 결합 (<{n:2d}자)  ", score(strategy("S2", n)))
    print("S3 + 항상 결합            ", score(strategy("S3")))
    print("\n현재 질문 길이(공백 제외):", sorted(len(c["q"].replace(" ", "")) for c in cases))
