#!/usr/bin/env bash
# 컨테이너 최초 생성 시 1회 실행. Python 의존성·임베딩 모델은 이미지(/opt/venv)에 포함돼 있음.
set -e
# Windows 폴더를 마운트하면 소유자가 달라 git 이 "dubious ownership" 으로 거부하므로 예외 등록
git config --global --add safe.directory "$(pwd)"
git config core.hooksPath .githooks
(cd frontend && npm install)
(cd backend && mvn -q -DskipTests dependency:go-offline || true)
echo "✅ 개발 환경 준비 완료: bash .devcontainer/run-local.sh 로 실행하세요."
