# main 반영 가이드

## 목적과 작업 범위

최신 main을 기준으로 Android와 iOS KMP 앱 결과물을 반영한다. 코드, 문서, AI 규칙, 스킬, 설정과 핸드오프는 `android/`에 둔다. GitHub Actions가 읽는 `.github/workflows/android-ci.yml`만 루트 경로의 예외다.

- 이슈: [#524](https://github.com/woowacourse-teams/2026-chaekchaek/issues/524)
- main 기준 커밋: `63bc1a04b6d6259fc5899c022cf5ccf945f5c18d`
- 가져온 an-develop 원본: `ad6280a20886d4758274c2db61de56d8dec48a5a`
- 작업 브랜치: `codex/524-android-scope`

이 브랜치는 main에서 분기한다. an-develop의 커밋을 병합하거나 cherry-pick하지 않고 필요한 파일의 결과물만 새 커밋에 넣는다. 따라서 an-develop의 frontend 구현과 커밋 이력은 추가되지 않는다. main에 이미 있는 frontend와 backend, 공통 설정 및 기존 이력은 유지한다.

## 자산 위치

아래 이전 경로는 저장소 루트를 기준으로 한다.

| 이전 위치 | 현재 위치 |
| --- | --- |
| 루트 `AGENTS.md`, `CLAUDE.md` | 기존 `android/AGENTS.md`와 규칙을 합치고 `android/CLAUDE.md`로 연결 |
| 루트 `docs/` | 기존 `android/docs/`와 통합 |
| 루트 `.agents/skills/` | `android/.agents/skills/` |
| 루트 `.codex/hooks.json` | 실행하지 않는 보관 자료 `android/docs/legacy/codex-hooks.json` |
| 루트 `handoff.md`, `handoff/` | `android/handoff.md`, `android/handoff/` |
| an-develop의 루트 PR 템플릿 | 참고용 `android/.github/pull_request_template.md` |
| an-develop의 루트 README 연결 | `android/README.md`와 문서 인덱스로 통합 |
| an-develop의 루트 ignore 규칙 | `android/.gitignore`의 해당 Android 로컬 경로에 적용 |

`android/.github/pull_request_template.md`는 Android 작업자가 참고하는 본문 양식이다. 저장소 공통 PR 템플릿을 교체하거나 GitHub가 이 하위 경로를 자동 적용한다고 가정하지 않는다. main의 루트 PR 템플릿은 그대로 둔다.

과거 로컬 훅은 [보관 안내](legacy/README.md)를 따른다. 개인 절대경로를 참조하는 설정을 활성화하거나 전역 훅을 설치하지 않는다. 웹 실험 핸드오프는 과거 기록으로 보관하며 frontend 개발 지시로 사용하지 않는다.

## 후속 반영 절차

1. `git status --untracked-files=no`로 기존 추적 변경을 확인하고 원격 main과 an-develop을 갱신한다.
2. main에서 분기한 동일 목적의 작업 브랜치를 재사용하거나, 없으면 최신 main에서 새 작업 브랜치를 만든다. main 직접 push와 직접 merge는 금지한다.
3. 가져올 an-develop의 SHA를 고정한다. 이번 변경 이후의 후속 작업에서는 이전 이동 위치와 수동 수정도 보존한다.
4. 변경 파일을 `android/**`와 `.github/workflows/android-ci.yml`로 제한한다. `frontend/**`, `backend/**`, 루트 AI 규칙, 루트 README, 공통 PR 템플릿과 루트 `.gitignore`는 포함하지 않는다.
5. 문서와 규칙의 상대 링크, CLAUDE 심볼릭 링크, 개인 비밀값의 제외 여부, CI 작업 디렉터리와 커밋 이력을 확인한다.
6. GitHub 이슈와 PR 생성은 각각 초안을 제시하고 독립된 `슛` 승인을 받는다. 이슈 승인으로 PR 생성을 승인한 것으로 해석하지 않는다.

## Firebase iOS 로컬 설정

<a id="firebase-ios"></a>

원본 iOS Firebase 설정의 API 키를 새 커밋에 추가하지 않는다. `iosApp/Chaekchaek/GoogleService-Info.plist.example`에는 API 키를 비워 두고, 실제 `GoogleService-Info.plist`와 `.env`는 `android/.gitignore`로 제외한다. 다른 Firebase 식별자는 원본 값과 동일하게 유지한다.

iOS를 처음 빌드하는 작업자는 키를 로컬 `.env` 또는 환경변수로 관리하고, `FIREBASE_IOS_API_KEY`가 환경에 로드된 상태에서 `android/` 안에서 다음 명령을 실행한다. 스크립트는 `.env` 파일을 직접 읽지 않는다.

```sh
python3 scripts/prepare-ios-firebase-config.py
```

스크립트는 기존 로컬 설정을 덮어쓰지 않고 API 키를 출력하지 않는다. 생성한 설정은 기존 Xcode 프로젝트가 참조하는 리소스 위치에 놓인다. 설정 생성은 실제 iOS 빌드, 로그인이나 Firebase 이벤트 수신을 검증했다는 의미가 아니다.

## 검증 범위

파일 경로와 문서 연결, main 기존 파일의 보존, an-develop 이력의 제외를 Git 차이로 확인한다. Android 빌드와 테스트, 기기 실행, iOS 빌드, 서버 연동과 Firebase 이벤트 수신은 서로 별도의 검증 결과로 보고한다.
