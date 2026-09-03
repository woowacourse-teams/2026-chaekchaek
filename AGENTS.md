# 첵췍 모노레포 작업 스코프

Codex와 Claude Code를 함께 사용한다. 두 도구 모두 이 파일(AGENTS.md, CLAUDE.md는 심볼릭
링크)을 프로젝트 메모리로 읽으므로, 아래 규칙은 어느 도구로 작업하든 동일하게 적용한다.

## 디렉터리별 작업 범위

- `/android` - Android 앱 작업 루트. Android 관련 작업은 이 디렉터리 안에서만 진행한다.
  Android 전용 규칙은 `android/AGENTS.md`(= `android/CLAUDE.md`) 참고.
- `/backend` - 백엔드 작업 범위.
- `/frontend` - 프론트엔드 작업 범위.

## 원칙

- 한 작업이 특정 스코프(android/backend/frontend)에 속하면, 명시적 요청 없이 그 스코프
  밖의 디렉터리를 수정하지 않는다.
- 작업 중인 도구가 Codex든 Claude Code든 이 스코프 규칙은 동일하게 지킨다.
- 설치, 조회, 빌드, 테스트, 디바이스 실행처럼 저장소 파일을 변경하지 않는 작업에는 새
  브랜치를 만들지 않는다.

## AI 작업 설정 예외

- `AGENTS.md`, `CLAUDE.md` 등 AI 에이전트 작업 설정 변경은 별도 GitHub 이슈, 작업 브랜치,
  PR 없이 `an-develop`에 직접 커밋하고 push할 수 있다.
- 이 예외는 `main` 브랜치 보호 규칙에는 적용하지 않는다.

## GitHub 작업 흐름

- 현재 프로젝트에서는 Notion 칸반 단계를 사용하지 않는다.
- 새 작업은 GitHub 이슈 생성, 작업 브랜치 생성, Draft PR 생성, 구현 및 검증 순서로 진행한다.
- GitHub 이슈에는 저장소에 이미 존재하는 범위 및 작업 유형 라벨을 적극적으로 적용한다.
- `/android` 스코프의 GitHub 이슈와 `an-develop` 대상 PR 제목은 반드시 `[AN] `으로 시작한다.
- 이슈와 PR 생성 승인용 제목 초안에 `[AN] `을 포함하고, 생성 직후 실제 제목에도 접두사가
  적용됐는지 GitHub CLI로 확인한다.
- GitHub 이슈 생성 승인을 요청하기 전에 이슈 제목, 본문, 적용할 라벨 초안을 먼저 작성해
  사용자에게 제시한다. 사용자의 초안 검토가 끝난 뒤에만 `슛` 승인을 요청한다.
- 사용자가 이슈 초안 수정을 요청하면 수정안을 다시 제시하며, 초안에 대한 확인이나 수정 요청을
  이슈 생성 승인으로 간주하지 않는다.
- Draft PR 생성 승인을 요청하기 전에 PR 제목과 본문 초안을 먼저 작성해 사용자에게 제시한다.
  사용자의 초안 검토가 끝난 뒤에만 PR 생성을 위한 별도의 `슛` 승인을 요청한다.
- 사용자가 PR 초안 수정을 요청하면 수정안을 다시 제시하며, 초안에 대한 확인이나 수정 요청을
  PR 생성 승인으로 간주하지 않는다.
- 이슈와 PR 생성 승인은 전역 `AGENTS.md`의 `슛` 규칙을 따른다.

## Pencil 디자인 파일 SSOT

- Pencil 디자인의 단일 원본은 `/Users/ujeonghyeon/Downloads/designs.pen`이다.
- Pencil 조회와 편집은 항상 이 파일을 대상으로 한다.
- 저장소 루트나 다른 경로에 `designs.pen` 복사본을 만들거나 동기화하지 않는다.
- `designs.pen`은 Git으로 추적하거나 PR에 포함하지 않는다.

## 문서 규칙

- 오래 유지할 프로젝트 문서는 저장소 루트의 `/docs`에 Markdown 파일로 둔다.
- 각 디렉터리의 README는 해당 문서를 찾는 짧은 라우팅·시작 안내만 둔다. 상세 운영 절차나
  설계 설명을 README에 누적하지 않는다.
- 새 문서에는 목적을 드러내는 kebab-case 파일명을 쓰고, 관련 README에서 링크한다.
- Markdown 문서만 변경하는 독립 작업은 GitHub 이슈, 작업 브랜치, PR 없이 `an-develop`에
  직접 커밋하고 push한다.
- 기능이나 코드 변경에 수반되는 문서 변경은 해당 작업 브랜치와 PR에 함께 포함한다.

## an-develop 브랜치 운영

- `an-develop`은 현재 사용자 한 명이 관리하는 통합 브랜치이므로 PR 없이 직접 push해도 된다.
- `origin/an-develop`을 원격의 최신 기준으로 유지한다.
- `an-develop-local`은 로컬 작업·검증 전용이며 원격 브랜치로 push하지 않는다. 작업 완료 후
  `an-develop-local`의 HEAD를 `origin/an-develop`에 fast-forward로 반영한다.

## main 브랜치 보호 (필수)

- main에 직접 push하거나 merge하지 않는다. 예외 없음.
- 모든 변경은 별도 브랜치에서 작업한 뒤 PR(Pull Request)을 통해서만 main에 반영한다.
- 작업 중인 도구가 Codex든 Claude Code든 이 규칙은 동일하게 지킨다.
