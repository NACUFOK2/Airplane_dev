#!/usr/bin/env bash
# 로컬 실행: 백엔드(8080) + 프론트엔드(3000) 동시 기동
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "$ROOT/.env" ] && set -a && . "$ROOT/.env" && set +a

(cd "$ROOT/backend" && mvn -q spring-boot:run) &
BACK=$!
(cd "$ROOT/frontend" && npm run dev) &
FRONT=$!
trap 'kill $BACK $FRONT 2>/dev/null' INT TERM EXIT
echo "Backend  → http://localhost:8080"
echo "Frontend → http://localhost:3000"
wait
