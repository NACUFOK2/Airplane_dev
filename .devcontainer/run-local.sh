#!/usr/bin/env bash
# 로컬 실행: 임베딩(8001) + 백엔드(8080) + 프론트엔드(3000) 동시 기동
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "$ROOT/.env" ] && set -a && . "$ROOT/.env" && set +a

(cd "$ROOT/embedding-service" && .venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001) &
EMB=$!
(cd "$ROOT/backend" && mvn -q spring-boot:run) &
BACK=$!
(cd "$ROOT/frontend" && npm run dev) &
FRONT=$!
trap 'kill $EMB $BACK $FRONT 2>/dev/null' INT TERM EXIT
echo "Embedding → http://127.0.0.1:8001 (첫 기동 시 모델 다운로드 ~470MB)"
echo "Backend   → http://localhost:8080"
echo "Frontend  → http://localhost:3000"
wait
