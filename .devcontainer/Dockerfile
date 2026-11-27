# Java 21 + Maven + Python(임베딩 서비스) 통합 개발 환경 (Node 20 은 devcontainer feature 로 설치)
# 빌드 컨텍스트는 프로젝트 루트(..) — devcontainer.json 의 build.context 참고
FROM mcr.microsoft.com/devcontainers/java:1-21-bookworm

# 베이스 이미지의 yarn apt 저장소 GPG 키 만료로 apt-get update 가 실패(exit 100)하는 문제 회피
# Debian bookworm 기본 python3 = 3.11 (torch 2.8 지원 범위 3.10~3.13)
RUN rm -f /etc/apt/sources.list.d/yarn.list \
    && apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get install -y --no-install-recommends maven python3 python3-venv python3-pip \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Seoul \
    VENV_PATH=/opt/venv \
    HF_HOME=/opt/hf-cache \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# ---- 임베딩 서비스 Python 의존성을 이미지에 포함 ---------------------------
# requirements.txt 만 먼저 복사 → 소스가 바뀌어도 이 레이어는 캐시 재사용
COPY embedding-service/requirements.txt /tmp/requirements.txt
# CPU 전용 torch 를 먼저 설치 (기본 CUDA 휠은 수 GB, 이 서비스는 device="cpu")
RUN python3 -m venv $VENV_PATH \
    && $VENV_PATH/bin/pip install --no-cache-dir --upgrade pip \
    && $VENV_PATH/bin/pip install --no-cache-dir torch==2.8.0 --index-url https://download.pytorch.org/whl/cpu \
    && $VENV_PATH/bin/pip install --no-cache-dir -r /tmp/requirements.txt \
    && rm /tmp/requirements.txt

# 임베딩 모델(~470MB)을 미리 받아 첫 기동 대기 제거 (네트워크 실패 시 런타임에 받도록 빌드는 계속)
RUN mkdir -p $HF_HOME \
    && ($VENV_PATH/bin/python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('dragonkue/multilingual-e5-small-ko-v2')" \
        || echo "WARN: 모델 사전 다운로드 실패 - 첫 기동 시 다운로드됩니다") \
    && chown -R vscode:vscode $VENV_PATH $HF_HOME

ENV PATH=$VENV_PATH/bin:$PATH
