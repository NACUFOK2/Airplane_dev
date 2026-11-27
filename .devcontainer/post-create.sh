#!/usr/bin/env bash
set -e
git config core.hooksPath .githooks
./embedding-service/setup.sh            # 리눅스: CPU 전용 torch, 모델 미리 다운로드
(cd backend && mvn -q -DskipTests dependency:go-offline || true)
(cd frontend && npm install)
echo "✅ 개발 환경 준비 완료: ./.devcontainer/run-local.sh 로 실행하세요."
