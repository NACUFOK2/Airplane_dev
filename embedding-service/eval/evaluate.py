"""검색 파라미터 선정용 오프라인 평가 스크립트.

python eval/evaluate.py  → BM25 / Dense / Hybrid(RRF·가중합) 의 Hit@k, MRR 과
도메인 밖 질문(OOD)의 최고 점수 분포를 출력한다. Java 구현과 같은 분석기·BM25 수식을 쓴다.
"""
import json, math, re, sys
from collections import Counter
from pathlib import Path
import numpy as np
from sentence_transformers import SentenceTransformer

ROOT = Path(__file__).resolve().parent.parent
MODEL = "dragonkue/multilingual-e5-small-ko-v2"
rows = [json.loads(l) for l in open(ROOT / "data/qa_embeddings.jsonl", encoding="utf-8")]
ids = [r["id"] for r in rows]
evalset = [json.loads(l) for l in open(ROOT / "eval/eval_set.jsonl", encoding="utf-8")]
ood = [l.strip() for l in open(ROOT / "eval/ood_set.txt", encoding="utf-8") if l.strip()]
ORGS = sorted({r["org"] for r in rows}, key=len, reverse=True)

# ---------- BM25 (Java TextAnalyzer / Bm25Index 와 동일) ----------
STOP = set("은 는 이 가 을 를 에 의 도 로 으로 와 과 어떻게 얼마 얼마나 있나요 되나요 하나요 인가요 알려줘 알려주세요 궁금해요".split())
def analyze(t):
    out = []
    for w in re.split(r"[^\w]+", t.lower()):
        if not w or w in STOP or w == "_": continue
        out.append(w)
        if re.search(r"[가-힣]", w) and len(w) >= 2:
            out += [w[i:i+2] for i in range(len(w) - 1)]
    return out

class BM25:
    def __init__(self, docs, k1=1.2, b=0.75, title_boost=3):
        self.k1, self.b = k1, b
        self.tf = []
        for r in docs:
            c = Counter()
            for _ in range(title_boost): c.update(analyze(r["org"] + " " + r["question"]))
            c.update(analyze(r["answer"]))
            self.tf.append(c)
        self.len = [sum(c.values()) for c in self.tf]
        self.avg = sum(self.len) / len(self.len)
        self.df = Counter(t for c in self.tf for t in c)
        self.n = len(docs)
    def scores(self, q):
        s = np.zeros(self.n)
        for t in analyze(q):
            df = self.df.get(t, 0)
            if not df: continue
            idf = math.log(1 + (self.n - df + 0.5) / (df + 0.5))
            for i, c in enumerate(self.tf):
                f = c.get(t)
                if f: s[i] += idf * f * (self.k1 + 1) / (f + self.k1 * (1 - self.b + self.b * self.len[i] / self.avg))
        return s

def detect_org(q):
    for o in ORGS:
        if o in q: return o
    return None

def ranks_of(scores, mask=None):
    s = scores.copy()
    if mask is not None: s[~mask] = -1e9
    return list(np.argsort(-s))

def rrf(rank_lists, k):
    fused = np.zeros(len(rows))
    for rl in rank_lists:
        for r, i in enumerate(rl): fused[i] += 1.0 / (k + r + 1)
    return fused

def minmax(x):
    lo, hi = x.min(), x.max()
    return (x - lo) / (hi - lo) if hi > lo else np.zeros_like(x)

def metrics(rank_fn):
    h1 = h3 = h5 = mrr = 0
    for e in evalset:
        order = [ids[i] for i in rank_fn(e["q"])]
        pos = min((order.index(g) for g in e["gold"]), default=999)
        h1 += pos < 1; h3 += pos < 3; h5 += pos < 5; mrr += 1 / (pos + 1)
    n = len(evalset)
    return f"Hit@1 {h1/n:.3f}  Hit@3 {h3/n:.3f}  Hit@5 {h5/n:.3f}  MRR {mrr/n:.3f}"

if __name__ == "__main__":
    model = SentenceTransformer(MODEL)
    E_given = np.array([r["embedding"] for r in rows], dtype=np.float32)
    E_local = model.encode(["passage: " + r["embed_text"] for r in rows], normalize_embeddings=True)
    bm25 = BM25(rows)
    qcache = {}
    def qv(q, prefix="query: "):
        key = prefix + q
        if key not in qcache: qcache[key] = model.encode([key], normalize_embeddings=True)[0]
        return qcache[key]
    org_mask = lambda q: (np.array([r["org"] == detect_org(q) for r in rows]) if detect_org(q) else None)

    print("== 단일 검색기 ==")
    print("BM25                    ", metrics(lambda q: ranks_of(bm25.scores(q))))
    print("Dense(제공 벡터)        ", metrics(lambda q: ranks_of(E_given @ qv(q))))
    print("Dense(로컬 재인코딩)    ", metrics(lambda q: ranks_of(E_local @ qv(q))))
    print("Dense(query: 접두어 없음)", metrics(lambda q: ranks_of(E_given @ qv(q, ""))))

    print("\n== 하이브리드 (RRF) ==")
    for k in (5, 20, 60):
        print(f"RRF k={k:<3}               ", metrics(lambda q, k=k: ranks_of(rrf([ranks_of(E_given @ qv(q)), ranks_of(bm25.scores(q))], k))))
    print("\n== 하이브리드 (min-max 가중합, alpha=dense 비중) ==")
    for a in (0.5, 0.6, 0.7, 0.8):
        print(f"alpha={a}               ", metrics(lambda q, a=a: ranks_of(a * minmax(E_given @ qv(q)) + (1 - a) * minmax(bm25.scores(q)))))

    print("\n== 질문 속 기관명 자동 필터 ==")
    print("Dense + org필터          ", metrics(lambda q: ranks_of(E_given @ qv(q), org_mask(q))))
    print("RRF k=60 + org필터       ", metrics(lambda q: ranks_of(rrf([ranks_of(E_given @ qv(q)), ranks_of(bm25.scores(q))], 60), org_mask(q))))
    for a in (0.6, 0.7):
        print(f"alpha={a} + org필터      ", metrics(lambda q, a=a: ranks_of(a * minmax(E_given @ qv(q)) + (1 - a) * minmax(bm25.scores(q)), org_mask(q))))

    print("\n== 거절 임계값 산정: dense top-1 코사인 ==")
    ind = sorted(float((E_given @ qv(e["q"])).max()) for e in evalset)
    out = sorted(float((E_given @ qv(q)).max()) for q in ood)
    print("in-domain  min/p10/median:", round(ind[0], 3), round(ind[len(ind)//10], 3), round(ind[len(ind)//2], 3))
    print("OOD        values         :", [round(x, 3) for x in out], " ← ", ood)

    print("\n== 정답 문서의 dense 순위 분포 (top-k 컨텍스트 크기 산정) ==")
    pos = []
    for e in evalset:
        order = [ids[i] for i in ranks_of(0.7 * minmax(E_given @ qv(e["q"])) + 0.3 * minmax(bm25.scores(e["q"])), org_mask(e["q"]))]
        pos.append(min(order.index(g) for g in e["gold"]) + 1)
    print("정답 순위:", sorted(pos))
