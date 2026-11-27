#!/usr/bin/env bash
set -e
git config core.hooksPath .githooks
(cd backend && mvn -q -DskipTests dependency:go-offline || true)
(cd frontend && npm install)
echo "✅ 개발 환경 준비 완료: ./.devcontainer/run-local.sh 로 실행하세요."
