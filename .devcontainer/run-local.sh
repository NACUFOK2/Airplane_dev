#!/usr/bin/env bash
# 로컬 실행: 임베딩(8001) + 백엔드(8080) + 프론트엔드(3000) 동시 기동
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "$ROOT/.env" ] && set -a && . "$ROOT/.env" && set +a

port_busy() { (echo > /dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1; }
for p in 8001 8080 3000; do
  if port_busy "$p"; then echo "⚠️  포트 $p 가 이미 사용 중입니다. 이미 실행 중인 서버를 끄거나(lsof -iTCP:$p -sTCP:LISTEN) 그대로 쓰세요."; fi
done

# 1) 임베딩: 가상환경이 없으면 먼저 설치 (개발 컨테이너 밖에서 처음 실행하는 경우)
if [ ! -x "$ROOT/embedding-service/.venv/bin/uvicorn" ]; then
  echo "▶ 임베딩 서비스 가상환경이 없어 설치합니다 (embedding-service/setup.sh)"
  "$ROOT/embedding-service/setup.sh" || echo "⚠️  임베딩 설치 실패 → 백엔드는 키워드 검색(BM25)만으로 동작합니다."
fi
EMB=""
if [ -x "$ROOT/embedding-service/.venv/bin/uvicorn" ]; then
  (cd "$ROOT/embedding-service" && .venv/bin/uvicorn app:app --host 127.0.0.1 --port 8001) &
  EMB=$!
fi

(cd "$ROOT/backend" && mvn -q spring-boot:run) &
BACK=$!
(cd "$ROOT/frontend" && npm run dev) &
FRONT=$!
trap 'kill $EMB $BACK $FRONT 2>/dev/null' INT TERM EXIT

# 2) 임베딩 준비 대기 (모델 로딩 후 포트가 열림. 처음이면 다운로드로 수 분)
if [ -n "$EMB" ]; then
  echo "⏳ 임베딩 서비스 준비 중..."
  for i in $(seq 1 300); do
    if curl -sf http://127.0.0.1:8001/health >/dev/null 2>&1; then echo "✅ Embedding → http://127.0.0.1:8001 ($(curl -s http://127.0.0.1:8001/health))"; break; fi
    if ! kill -0 "$EMB" 2>/dev/null; then echo "❌ 임베딩 서비스가 종료됐습니다. 위 오류를 확인하고 embedding-service/setup.sh --recreate 를 실행해 보세요."; break; fi
    sleep 1
  done
fi
echo "Backend   → http://localhost:8080"
echo "Frontend  → http://localhost:3000"
echo "상태 확인 → for u in http://127.0.0.1:8001/health http://localhost:8080/api/health http://localhost:3000; do curl -s -o /dev/null -w \"\$u %{http_code}\\n\" \$u; done"
wait
