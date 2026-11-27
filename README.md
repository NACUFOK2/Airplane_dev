# ✈️ Airplane QnA — 항공사 RAG 챗봇

여러 항공사의 규정(수하물, 예약/변경, 체크인 등)을 검색해 답변하는 로컬 RAG 챗봇 서비스입니다.

- **Backend**: Java 21 · Spring Boot (MVC 레이어드 아키텍처) · `backend/`
- **Frontend**: Next.js (App Router) · 토스 스타일 UI · `frontend/`
- **Dev 환경**: `.devcontainer/` (JDK 21 + Maven + Node 20 일괄 구성)

> 배포하지 않으며 로컬에서만 실행합니다. 자세한 실행 방법은 아래 섹션 참고.

## 브랜치 / 커밋 규칙
[docs/COMMIT_CONVENTION.md](docs/COMMIT_CONVENTION.md) 참고.

## 보안
AI 키·토큰·개인정보·결제 관련 파일은 커밋 금지입니다. 클론 후 아래를 실행하세요.

```bash
git config core.hooksPath .githooks
```
