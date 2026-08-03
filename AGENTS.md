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

## 문서 규칙

- 오래 유지할 프로젝트 문서는 저장소 루트의 `/docs`에 Markdown 파일로 둔다.
- 각 디렉터리의 README는 해당 문서를 찾는 짧은 라우팅·시작 안내만 둔다. 상세 운영 절차나
  설계 설명을 README에 누적하지 않는다.
- 새 문서에는 목적을 드러내는 kebab-case 파일명을 쓰고, 관련 README에서 링크한다.

## main 브랜치 보호 (필수)

- main에 직접 push하거나 merge하지 않는다. 예외 없음.
- 모든 변경은 별도 브랜치에서 작업한 뒤 PR(Pull Request)을 통해서만 main에 반영한다.
- 작업 중인 도구가 Codex든 Claude Code든 이 규칙은 동일하게 지킨다.
