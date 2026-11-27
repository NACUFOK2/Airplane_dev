"""Q&A 원본(XLSX/CSV) → 검색 인덱스(qa_embeddings.jsonl) 빌더.

사용법
    .venv/bin/python scripts/build_index.py <Q&A.xlsx | qa_metadata.csv> [--dry-run] [--reembed-all]

- XLSX: 시트명이 '<유형>_Q&A' 이고 헤더가 (기관명, 질문, 답변, 출처) 인 시트를 모두 읽는다.
- CSV : id,sheet,org_type,org,question,answer,source 형식 (qa_metadata.csv).
- id 는 (기관, 질문)이 같으면 기존 값을 유지하고, 새 행에만 다음 번호를 붙인다 → 검색 평가셋 정답 id 가 깨지지 않는다.
- 임베딩은 embed_text 가 바뀐 행만 새로 계산한다(--reembed-all 로 전체 재계산).
  기존 벡터를 재사용하는 이유: docs/RAG_PARAMETERS.md '문서 벡터' 항목 참고.
- 결과를 embedding-service/data 와 backend/src/main/resources/data 두 곳에 같은 내용으로 쓴다.
"""
import argparse, csv, hashlib, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_FILES = [ROOT / "data/qa_embeddings.jsonl",
             ROOT.parent / "backend/src/main/resources/data/qa_embeddings.jsonl"]
MODEL_NAME = "dragonkue/multilingual-e5-small-ko-v2"
DIM = 384


def embed_text(r):
    return f"[{r['org']}] 질문: {r['question']}\n답변: {r['answer']}"


def clean(v):
    return re.sub(r"[ \t]+", " ", str(v or "")).strip()


def read_xlsx(path):
    import openpyxl
    wb = openpyxl.load_workbook(path, read_only=True)
    out = []
    for ws in wb.worksheets:
        m = re.match(r"^(.+)_Q&A$", ws.title)
        if not m:
            print(f"  건너뜀: 시트 '{ws.title}' (이름이 '<유형>_Q&A' 형식 아님)")
            continue
        rows = list(ws.iter_rows(values_only=True))
        for i, row in enumerate(rows[1:], start=2):
            if not row or not any(row):
                continue
            org, q, a, src = (list(row) + [None] * 4)[:4]
            out.append({"sheet": ws.title, "org_type": m.group(1), "org": clean(org), "question": clean(q),
                        "answer": clean(a), "source": clean(src), "_where": f"{ws.title}!{i}"})
    return out


def read_csv(path):
    with open(path, encoding="utf-8-sig") as f:
        return [{"sheet": r["sheet"], "org_type": r["org_type"], "org": clean(r["org"]), "question": clean(r["question"]),
                 "answer": clean(r["answer"]), "source": clean(r["source"]), "_where": f"csv:{i}"}
                for i, r in enumerate(csv.DictReader(f), start=2)]


def validate(rows):
    errors, seen = [], {}
    for r in rows:
        for k in ("org_type", "org", "question", "answer"):
            if not r[k]:
                errors.append(f"{r['_where']}: '{k}' 비어 있음")
        if r["source"] and not r["source"].startswith(("http://", "https://")):
            errors.append(f"{r['_where']}: 출처가 URL 이 아님 → {r['source'][:40]}")
        key = (r["org"], r["question"])
        if key in seen:
            errors.append(f"{r['_where']}: 중복 질문 (첫 등장 {seen[key]}) → {r['question']}")
        seen[key] = r["_where"]
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
        print("❌ 데이터 오류:\n  " + "\n  ".join(errors))
        sys.exit(1)

    existing = {}
    if OUT_FILES[0].exists():
        for line in open(OUT_FILES[0], encoding="utf-8"):
            d = json.loads(line)
            existing[(d["org"], d["question"])] = d
    next_id = max((int(d["id"]) for d in existing.values()), default=-1) + 1

    out, to_embed, stats = [], [], {"new": 0, "changed": 0, "same": 0}
    for r in rows:
        r = {k: v for k, v in r.items() if not k.startswith("_")}
        r["embed_text"] = embed_text(r)
        prev = existing.pop((r["org"], r["question"]), None)
        if prev:
            r["id"] = prev["id"]
            if prev["embed_text"] == r["embed_text"] and not args.reembed_all:
                r["embedding"] = prev["embedding"]
                stats["same"] += 1
            else:
                to_embed.append(r)
                stats["changed"] += 1
        else:
            r["id"] = f"{next_id:03d}"
            next_id += 1
            to_embed.append(r)
            stats["new"] += 1
        out.append(r)

    print(f"총 {len(out)}건: 신규 {stats['new']}, 변경 {stats['changed']}, 유지 {stats['same']}, 삭제 {len(existing)}")
    for d in existing.values():
        print(f"  삭제됨: {d['id']} [{d['org']}] {d['question']}")
    for r in to_embed:
        print(f"  임베딩 대상: {r['id']} [{r['org']}] {r['question']}")
    if args.dry_run:
        print("(dry-run: 파일을 쓰지 않음)")
        return

    if to_embed:
        from sentence_transformers import SentenceTransformer
        model = SentenceTransformer(MODEL_NAME, device="cpu")
        vecs = model.encode(["passage: " + r["embed_text"] for r in to_embed], normalize_embeddings=True, batch_size=32)
        for r, v in zip(to_embed, vecs):
            r["embedding"] = [round(float(x), 7) for x in v]

    out.sort(key=lambda r: r["id"])
    keys = ["id", "sheet", "org_type", "org", "question", "answer", "source", "embed_text", "embedding"]
    payload = "".join(json.dumps({k: r[k] for k in keys}, ensure_ascii=False) + "\n" for r in out)
    assert all(len(r["embedding"]) == DIM for r in out)
    for f in OUT_FILES:
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(payload, encoding="utf-8")
        print(f"✅ {f.relative_to(ROOT.parent)}  sha256={hashlib.sha256(payload.encode()).hexdigest()[:12]}")
    orgs = sorted({r["org"] for r in out})
    print(f"기관 {len(orgs)}곳: {', '.join(orgs)}")
    print("⚠️  새 기관이 있으면 backend application.yml 의 rag.organizations 에도 추가하세요.")


if __name__ == "__main__":
    main()
