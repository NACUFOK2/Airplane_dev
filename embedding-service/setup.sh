#!/usr/bin/env bash
# 임베딩 서비스 설치: 알맞은 Python 선택 → .venv 생성 → 의존성 설치 → 모델 미리 받기 → 자가 점검
# 사용법: ./setup.sh            (이미 .venv 가 있으면 의존성만 맞춤)
#         ./setup.sh --recreate (.venv 를 지우고 새로 만듦)
set -euo pipefail
cd "$(dirname "$0")"

say() { printf '\033[1m[embedding-setup]\033[0m %s\n' "$*"; }
fail() { printf '\033[31m[embedding-setup] ❌ %s\033[0m\n' "$*" >&2; exit 1; }

OS="$(uname -s)"; ARCH="$(uname -m)"
if [ "$OS" = "Darwin" ] && [ "$ARCH" = "x86_64" ]; then MAX_MINOR=12; else MAX_MINOR=14; fi

# 1) Python 고르기: 3.12 → 3.11 → 3.13 → 3.10 → 3.14 → 3.9 → python3 순으로, 지원 범위 안의 첫 번째
pick_python() {
  for c in python3.12 python3.11 python3.13 python3.10 python3.14 python3.9 python3; do
    command -v "$c" >/dev/null 2>&1 || continue
    v="$("$c" -c 'import sys; print(f"{sys.version_info[0]} {sys.version_info[1]}")' 2>/dev/null)" || continue
    set -- $v
    if [ "$1" = 3 ] && [ "$2" -ge 9 ] && [ "$2" -le "$MAX_MINOR" ]; then echo "$c"; return 0; fi
  done
  return 1
}
PY="${PYTHON:-$(pick_python || true)}"
[ -n "$PY" ] || fail "Python 3.9~3.$MAX_MINOR 이 필요합니다. (현재: $(python3 --version 2>&1 || echo 없음)) 예) brew install python@3.12"
say "Python: $PY ($("$PY" --version 2>&1)), OS: $OS/$ARCH"

# 2) 가상환경
if [ "${1:-}" = "--recreate" ]; then rm -rf .venv; fi
if [ ! -x .venv/bin/python ]; then
  say ".venv 생성"
  "$PY" -m venv .venv || fail "가상환경 생성 실패. 리눅스라면 python3-venv 패키지가 필요할 수 있습니다 (apt install python3-venv)."
fi
.venv/bin/python -m pip install -q --upgrade pip

# 3) 의존성 (리눅스는 CPU 전용 torch: CUDA 패키지 수 GB 를 받지 않음)
EXTRA=()
if [ "$OS" = "Linux" ]; then EXTRA=(--extra-index-url https://download.pytorch.org/whl/cpu); say "리눅스: CPU 전용 torch 사용"; fi
say "의존성 설치 중 (처음엔 수 분 걸릴 수 있음)"
.venv/bin/python -m pip install -q ${EXTRA[@]+"${EXTRA[@]}"} -r requirements.txt \
  || fail "의존성 설치 실패. 위 오류를 확인하세요. 'No matching distribution' 이면 Python 버전을 3.11~3.12 로 바꿔 './setup.sh --recreate' 를 실행하세요."

# 4) 모델 미리 받기 (~465MB, 처음 한 번) + 자가 점검
say "임베딩 모델 확인·다운로드 (처음 한 번 ~465MB)"
.venv/bin/python - <<'PY'
import json, numpy as np
from sentence_transformers import SentenceTransformer
import sentence_transformers, torch
m = SentenceTransformer("dragonkue/multilingual-e5-small-ko-v2", device="cpu")
row = json.loads(open("data/qa_embeddings.jsonl", encoding="utf-8").readline())
v = m.encode(["passage: " + row["embed_text"]], normalize_embeddings=True)[0]
sim = float(np.dot(v, np.array(row["embedding"], dtype=np.float32)))
print(f"  sentence-transformers {sentence_transformers.__version__}, torch {torch.__version__}, numpy {np.__version__}")
print(f"  저장된 문서 벡터와의 일치도(코사인): {sim:.3f}  (0.95 이상이면 정상)")
if sim < 0.95:
    raise SystemExit("  ❌ 일치도가 낮습니다. 모델이나 라이브러리 버전이 크게 다를 수 있습니다.")
PY
say "✅ 준비 완료 → 실행: .venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001"
