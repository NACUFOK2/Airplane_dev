# 커밋 & 브랜치 컨벤션

## 브랜치 전략

```
main      ← 최종 릴리스본만 (dev 에서 --no-ff 머지)
 └─ dev   ← 통합 개발 브랜치
     ├─ feature/devcontainer-setup
     ├─ feature/backend-rag-api
     └─ feature/frontend-chat-ui
```

1. 모든 작업은 `dev` 에서 `feature/<기능명>` 브랜치를 생성해 진행한다.
2. 기능 완료 시 `feature/*` → `dev` 로 `--no-ff` 머지한다.
3. `dev` 에서 통합 검증이 끝난 최종본만 `main` 으로 `--no-ff` 머지한다.

## 커밋 메시지 (네이버 커밋 메시지 컨벤션)

```
<type>: <subject>          ← 제목 50자 이내, 마침표 X, 명령문

<body>                     ← 72자 줄바꿈, "무엇을/왜" 중심

<footer>                   ← 이슈 참조 (선택)
```

| type | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 포맷팅, 세미콜론 누락 등 (로직 변경 없음) |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드 설정, 패키지 매니저, 환경 설정 |
| `design` | UI 디자인(CSS 등) 변경 |
| `rename` | 파일/폴더명 변경 |
| `remove` | 파일 삭제 |

footer 예시: `Resolves: #12`, `Ref: #34`, `Related to: #56`

### 예시

```
feat: 항공사별 BM25 검색기 추가

- 한글 바이그램 토크나이저로 형태소 분석기 없이 검색
- 항공사 코드로 문서 범위를 제한해 답변 혼선 방지

Ref: #3
```

## 보안 규칙 (커밋 금지)

- AI 식별값(API 키, 모델 키), 토큰, 개인정보, 결제 관련 파일은 **절대 커밋하지 않는다.**
- 시크릿은 `.env`(gitignore 대상) 또는 OS 환경변수로만 관리하고, 저장소에는 `.env.example` 만 둔다.
- `.githooks/pre-commit` 이 파일명/내용 패턴을 검사해 유출을 차단한다.
  클론 후 반드시 `git config core.hooksPath .githooks` 를 실행한다.
