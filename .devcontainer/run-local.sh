#!/usr/bin/env bash
# 로컬 실행: 임베딩(8001) + 백엔드(8080) + 프론트엔드(3000) 동시 기동
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "$ROOT/.env" ] && set -a && . "$ROOT/.env" && set +a

# Python: 이미지에 포함된 /opt/venv 우선, 없으면 embedding-service/.venv (컨테이너 밖 실행용)
VENV="${VENV_PATH:-/opt/venv}"
[ -x "$VENV/bin/uvicorn" ] || VENV="$ROOT/embedding-service/.venv"
[ -d "$ROOT/frontend/node_modules" ] || (cd "$ROOT/frontend" && npm install)

# 각 서비스 출력은 터미널 + .run-logs/<서비스>.log 에 함께 기록 (문제 발생 시 로그 파일 확인)
LOG="$ROOT/.run-logs"; mkdir -p "$LOG"
(cd "$ROOT/embedding-service" && "$VENV/bin/uvicorn" app:app --host 127.0.0.1 --port 8001 2>&1 | sed -u 's/^/[emb] /' | tee "$LOG/embedding.log") &
EMB=$!
(cd "$ROOT/backend" && mvn -q spring-boot:run 2>&1 | sed -u 's/^/[back] /' | tee "$LOG/backend.log") &
BACK=$!
(cd "$ROOT/frontend" && npm run dev 2>&1 | sed -u 's/^/[front] /' | tee "$LOG/frontend.log") &
FRONT=$!
trap 'pkill -P $EMB; pkill -P $BACK; pkill -P $FRONT; pkill -f "uvicorn app:app"; pkill -f spring-boot:run; pkill -f "next dev"; kill $EMB $BACK $FRONT 2>/dev/null' INT TERM EXIT
echo "Embedding → http://127.0.0.1:8001 (모델은 이미지에 포함, 로딩 수 초)"
echo "Backend   → http://localhost:8080"
echo "Frontend  → http://localhost:3000"
wait
