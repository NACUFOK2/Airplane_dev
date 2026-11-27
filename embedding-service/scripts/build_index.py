"""문답 원본(XLSX/CSV) → 검색 인덱스(qa_embeddings.jsonl) 빌더. v2 형식(질문 변형 포함).

사용법
    .venv/bin/python scripts/build_index.py <v2_QA_augmented.xlsx | v2_qa_metadata.csv> [--dry-run] [--reembed-all]

입력 형식 (한 행 = 질문 변형 1개)
    id, parent_id, variant, sheet, [category], org, question, original_question, answer, source
    - variant: original(원문) / casual(구어체) / keyword(키워드) / rephrase(다른 표현)
    - category 가 없으면 시트명('항공사_Q&A' → 항공사)에서 가져온다.

동작
    - id 와 embed_text 가 기존 인덱스와 같으면 벡터를 재사용하고, 새로 생기거나 바뀐 행만 임베딩한다.
      (제공된 벡터를 유지하는 이유: docs/RAG_PARAMETERS.md '문서 벡터' 항목)
    - 원문마다 original 변형이 정확히 1개인지, 같은 원문의 변형끼리 기관·답변·출처가 같은지 검사한다.
    - 결과를 embedding-service/data 와 backend/src/main/resources/data 두 곳에 같은 내용으로 쓴다.
"""
import argparse, csv, hashlib, json, re, sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_FILES = [ROOT / "data/qa_embeddings.jsonl",
             ROOT.parent / "backend/src/main/resources/data/qa_embeddings.jsonl"]
MODEL_NAME = "dragonkue/multilingual-e5-small-ko-v2"
DIM = 384
KEYS = ["id", "parent_id", "variant", "sheet", "category", "org", "question", "original_question",
        "answer", "source", "embed_text", "embedding"]
VARIANTS = {"original", "casual", "keyword", "rephrase"}


def clean(v):
    return re.sub(r"[ \t]+", " ", str(v if v is not None else "")).strip()


def embed_text(r):
    return f"[{r['org']}] 질문: {r['question']}\n답변: {r['answer']}"


def normalize(raw, where):
    r = {k: clean(raw.get(k)) for k in KEYS[:10]}
    if not r["category"]:
        r["category"] = r["sheet"].split("_")[0] if "_" in r["sheet"] else ""
    r["_where"] = where
    return r


def read_xlsx(path):
    import openpyxl
    wb = openpyxl.load_workbook(path, read_only=True)
    out = []
    for ws in wb.worksheets:
        rows = list(ws.iter_rows(values_only=True))
        if not rows:
            continue
        header = [clean(h) for h in rows[0]]
        if "parent_id" not in header:
            print(f"  건너뜀: 시트 '{ws.title}' (parent_id 열 없음)")
            continue
        for i, row in enumerate(rows[1:], start=2):
            if row and any(row):
                out.append(normalize(dict(zip(header, row)), f"{ws.title}!{i}"))
    return out


def read_csv(path):
    with open(path, encoding="utf-8-sig") as f:
        return [normalize(r, f"csv:{i}") for i, r in enumerate(csv.DictReader(f), start=2)]


def validate(rows):
    errors, ids, groups = [], {}, defaultdict(list)
    for r in rows:
        for k in ("id", "parent_id", "variant", "category", "org", "question", "original_question", "answer"):
            if not r[k]:
                errors.append(f"{r['_where']}: '{k}' 비어 있음")
        if r["variant"] and r["variant"] not in VARIANTS:
            errors.append(f"{r['_where']}: 알 수 없는 variant '{r['variant']}'")
        if r["source"] and not r["source"].startswith(("http://", "https://")):
            errors.append(f"{r['_where']}: 출처가 URL 이 아님 → {r['source'][:40]}")
        if r["id"] in ids:
            errors.append(f"{r['_where']}: id 중복 (첫 등장 {ids[r['id']]}) → {r['id']}")
        ids[r["id"]] = r["_where"]
        groups[r["parent_id"]].append(r)
    for pid, g in groups.items():
        originals = [r for r in g if r["variant"] == "original"]
        if len(originals) != 1:
            errors.append(f"원문 {pid}: original 변형이 {len(originals)}개 (1개여야 함)")
        for k in ("org", "answer", "source", "original_question"):
            if len({r[k] for r in g}) > 1:
                errors.append(f"원문 {pid}: 변형끼리 '{k}' 값이 다름")
    return errors


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("source")
    ap.add_argument("--dry-run", action="store_true", help="파일을 쓰지 않고 변경 내역만 출력")
    ap.add_argument("--reembed-all", action="store_true", help="모든 행을 다시 임베딩")
    args = ap.parse_args()

    src = Path(args.source)
    rows = read_xlsx(src) if src.suffix.lower() in (".xlsx", ".xlsm") else read_csv(src)
    errors = validate(rows)
    if errors:
        print("❌ 데이터 오류:\n  " + "\n  ".join(errors[:50]))
        sys.exit(1)

    existing = {}
    if OUT_FILES[0].exists():
        for line in open(OUT_FILES[0], encoding="utf-8"):
            d = json.loads(line)
            existing[d["id"]] = d

    out, to_embed, stats = [], [], {"new": 0, "changed": 0, "same": 0}
    for r in rows:
        r = {k: v for k, v in r.items() if not k.startswith("_")}
        r["embed_text"] = embed_text(r)
        prev = existing.pop(r["id"], None)
        if prev and prev.get("embed_text") == r["embed_text"] and not args.reembed_all:
            r["embedding"] = prev["embedding"]
            stats["same"] += 1
        else:
            to_embed.append(r)
            stats["changed" if prev else "new"] += 1
        out.append(r)

    parents = {r["parent_id"] for r in out}
    print(f"원문 {len(parents)}건 / 변형 {len(out)}행: 신규 {stats['new']}, 변경 {stats['changed']}, "
          f"유지 {stats['same']}, 삭제 {len(existing)}")
    for d in list(existing.values())[:20]:
        print(f"  삭제됨: {d['id']} [{d['org']}] {d['question']}")
    for r in to_embed[:20]:
        print(f"  임베딩 대상: {r['id']} [{r['org']}] {r['question']}")
    if len(to_embed) > 20:
        print(f"  … 외 {len(to_embed) - 20}행")
    if args.dry_run:
        print("(dry-run: 파일을 쓰지 않음)")
        return

    if to_embed:
        from sentence_transformers import SentenceTransformer
        model = SentenceTransformer(MODEL_NAME, device="cpu")
        vecs = model.encode(["passage: " + r["embed_text"] for r in to_embed], normalize_embeddings=True, batch_size=32)
        for r, v in zip(to_embed, vecs):
            r["embedding"] = [round(float(x), 7) for x in v]

    assert all(len(r["embedding"]) == DIM for r in out)
    payload = "".join(json.dumps({k: r[k] for k in KEYS}, ensure_ascii=False) + "\n" for r in out)
    for f in OUT_FILES:
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(payload, encoding="utf-8")
        print(f"✅ {f.relative_to(ROOT.parent)}  sha256={hashlib.sha256(payload.encode()).hexdigest()[:12]}")
    orgs = sorted({r["org"] for r in out})
    print(f"기관 {len(orgs)}곳: {', '.join(orgs)}")
    print("⚠️  새 기관이 있으면 backend application.yml 의 rag.organizations 에도 추가하세요.")


if __name__ == "__main__":
    main()
