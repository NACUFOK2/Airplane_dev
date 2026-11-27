# Java 21 + Maven 통합 개발 환경 (Node 20 은 devcontainer feature 로 설치)
FROM mcr.microsoft.com/devcontainers/java:1-21-bookworm

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get install -y --no-install-recommends maven \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Seoul
