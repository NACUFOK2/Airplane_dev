"""v2 데이터(원문 178건 × 질문 변형 4종 = 712벡터) 검색 전략 비교.

python eval/evaluate_v2.py
  - Dense: 원문 벡터만 / 변형 전체 중 최댓값(max) / 변형 평균(mean)
  - BM25 : 원문 질문만 / 변형 질문까지 모두 색인
  - 융합 : RRF(k=60) + 질문 속 기관명 자동 필터(별칭 포함)
  - 범위 밖 임계값 산정, 후속 질문 전략 재확인
"""
import json, sys
from collections import defaultdict
from pathlib import Path
import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from evaluate import BM25, SentenceTransformer, MODEL, ranks_of  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
rows = [json.loads(l) for l in open(ROOT / "data/qa_embeddings.jsonl", encoding="utf-8")]
parents = [r for r in rows if r["variant"] == "original"]
pids = [p["parent_id"] for p in parents]
pidx = {p: i for i, p in enumerate(pids)}
evalset = [json.loads(l) for l in open(ROOT / "eval/eval_set_v2.jsonl", encoding="utf-8")]
followups = [json.loads(l) for l in open(ROOT / "eval/followup_set_v2.jsonl", encoding="utf-8")]
ood = [l.strip() for l in open(ROOT / "eval/ood_set.txt", encoding="utf-8") if l.strip()]

# 기관명 별칭 (backend application.yml 의 aliases 와 동일)
ALIASES = {"대한항공": [], "제주항공": [], "에어로케이": [], "델타항공": ["델타"], "유나이티드항공": ["유나이티드"],
           "에미레이트항공": ["에미레이트"], "싱가포르항공": [], "캐세이퍼시픽": ["캐세이"], "루프트한자": [],
           "ANA(전일본공수)": ["ANA", "전일본공수"], "하나투어": [], "모두투어": [], "노랑풍선": [], "참좋은여행": [],
           "아고다": [], "호텔스컴바인": [], "트립닷컴": [], "부킹닷컴": [], "야놀자": [], "여기어때": []}
NAMES = sorted(((n, o) for o, al in ALIASES.items() for n in [o] + al), key=lambda x: -len(x[0]))


def detect_org(q):
    c = q.replace(" ", "")
    for n, o in NAMES:
        if n in c:
            return o
    return None


# 변형 벡터를 원문 단위로 묶기
groups = defaultdict(list)
for r in rows:
    groups[r["parent_id"]].append(np.array(r["embedding"], dtype=np.float32))
E_orig = np.array([np.array(p["embedding"], dtype=np.float32) for p in parents])
E_mean = np.array([np.mean(groups[p], axis=0) for p in pids])
E_mean /= np.linalg.norm(E_mean, axis=1, keepdims=True)
V = np.array([np.array(r["embedding"], dtype=np.float32) for r in rows])
V_parent = np.array([pidx[r["parent_id"]] for r in rows])

bm_orig = BM25([{"org": p["org"], "question": p["original_question"], "answer": p["answer"]} for p in parents])
var_q = defaultdict(list)
for r in rows:
    var_q[r["parent_id"]].append(r["question"])
bm_all = BM25([{"org": p["org"], "question": " ".join(var_q[p["parent_id"]]), "answer": p["answer"]} for p in parents], title_boost=1)

model = SentenceTransformer(MODEL)
cache = {}


def qv(q):
    if q not in cache:
        cache[q] = model.encode(["query: " + q], normalize_embeddings=True)[0]
    return cache[q]


def dense(q, how):
    if how == "orig":
        return E_orig @ qv(q)
    if how == "mean":
        return E_mean @ qv(q)
    s = V @ qv(q)  # max
    out = np.full(len(pids), -1.0)
    np.maximum.at(out, V_parent, s)
    return out


def rrf(rank_lists, k):
    fused = np.zeros(len(pids))
    for rl in rank_lists:
        for r, i in enumerate(rl):
            fused[i] += 1.0 / (k + r + 1)
    return fused


def mask_for(org):
    return np.array([p["org"] == org for p in parents]) if org else None


def search(q, dense_how, bm, use_dense=True, use_bm=True, org=None, auto_org=True):
    org = org or (detect_org(q) if auto_org else None)
    m = mask_for(org)
    lists = []
    if use_dense: lists.append(ranks_of(dense(q, dense_how), m))
    if use_bm: lists.append(ranks_of(bm.scores(q), m))
    return ranks_of(rrf(lists, 60), m) if len(lists) > 1 else lists[0]


def metrics(fn, cases=None):
    cases = cases or evalset
    h1 = h3 = mrr = 0
    for e in cases:
        order = [pids[i] for i in fn(e)]
        pos = min((order.index(g) for g in e["gold"]), default=999)
        h1 += pos < 1; h3 += pos < 3; mrr += 1 / (pos + 1)
    n = len(cases)
    return f"Hit@1 {h1/n:.3f}  Hit@3 {h3/n:.3f}  MRR {mrr/n:.3f}"


if __name__ == "__main__":
    print(f"원문 {len(parents)}건, 벡터 {len(rows)}개, 평가 {len(evalset)}문항 (기존 40 + 신규 {len(evalset)-40})")
    print("\n== 단일 검색기 (기관 필터 없음) ==")
    for how in ("orig", "max", "mean"):
        print(f"Dense {how:5s}              ", metrics(lambda e, h=how: search(e["q"], h, bm_orig, use_bm=False, auto_org=False)))
    print("BM25 원문 질문만          ", metrics(lambda e: search(e["q"], "orig", bm_orig, use_dense=False, auto_org=False)))
    print("BM25 변형 질문 포함       ", metrics(lambda e: search(e["q"], "orig", bm_all, use_dense=False, auto_org=False)))
    print("\n== RRF + 기관 자동 필터 ==")
    for how in ("orig", "max", "mean"):
        for name, bm in (("원문", bm_orig), ("변형포함", bm_all)):
            print(f"Dense {how:5s} + BM25 {name:4s}  ", metrics(lambda e, h=how, b=bm: search(e["q"], h, b)))
    print("\n== 기관 필터 효과 (Dense max + BM25 변형포함) ==")
    print("필터 없음                 ", metrics(lambda e: search(e["q"], "max", bm_all, auto_org=False)))
    print("필터 있음                 ", metrics(lambda e: search(e["q"], "max", bm_all)))
    old = evalset[:40]; new = evalset[40:]
    print("  ↳ 기존 40문항           ", metrics(lambda e: search(e["q"], "max", bm_all), old))
    print("  ↳ 신규 28문항           ", metrics(lambda e: search(e["q"], "max", bm_all), new))

    print("\n== 범위 밖 임계값 (dense top-1) ==")
    for how in ("orig", "max"):
        ind = sorted(float(dense(e["q"], how).max()) for e in evalset)
        out = [(q, round(float(dense(q, how).max()), 3)) for q in ood]
        print(f"[{how}] 범위 안 최소 {ind[0]:.3f} / 하위5% {ind[len(ind)//20]:.3f} / 중앙 {ind[len(ind)//2]:.3f}")
        print(f"[{how}] 범위 밖:", out)

    print("\n== 후속 질문 (Dense max + BM25 변형포함) ==")
    def fu(e, n):
        org = detect_org(e["q"]) or detect_org(e["prev"])
        q = e["prev"] + " " + e["q"] if len(e["q"].replace(" ", "")) < n else e["q"]
        return search(q, "max", bm_all, org=org)
    print("현재 질문만               ", metrics(lambda e: search(e["q"], "max", bm_all), followups))
    for n in (6, 8, 10, 12):
        print(f"기관 이어받기 + <{n:2d}자 결합", metrics(lambda e, n=n: fu(e, n), followups))

    pos = []
    for e in evalset:
        order = [pids[i] for i in search(e["q"], "max", bm_all)]
        pos.append(min(order.index(g) for g in e["gold"]) + 1)
    print("\n정답 순위 분포:", {k: pos.count(k) for k in sorted(set(pos))})
