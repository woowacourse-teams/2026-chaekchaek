# Android 빌드·서명 환경 팀 소유 요구 사항 (필수)

"제 컴퓨터에서는 빌드되는데요" 문제와 keystore가 한 사람 로컬에만 있어 그 사람이 빠지면
배포가 멈추는 문제를 막기 위한 요구 사항. 반드시 지킬 것.

## 도착 조건 (완료 기준)
- 팀원 누구나 문서를 따라 릴리스 서명이 붙은 빌드 결과물을 만들 수 있다.
- minSdk / targetSdk 를 몇으로 정했는지와 그 이유가 문서화되어 있다.
- keystore와 비밀번호가 특정 팀원 1인의 로컬에만 있지 않고, 2인 이상이 접근 가능한 곳에
  보관되어 있다.

## 학습 조건 (팀원이 설명할 수 있어야 함)
- Gradle 빌드 구성(빌드 타입, 서명 설정, 의존성 관리)을 직접 작성하고 각 설정이 무엇을
  하는지 설명할 수 있다.
- minSdk 결정이 지원 기기 범위와 사용 가능한 API를 어떻게 바꾸는지 설명할 수 있고,
  targetSdk 가 스토어 정책과 연결된다는 것을 안다.
- 업로드 키와 앱 서명 키의 차이를 이해하고, 키를 잃었을 때 무엇이 불가능해지는지
설명할 수 있다.

## 관련 문서

- 이 요구 사항의 팀 운영 절차와 설정 설명은 [`../docs/android-build-signing.md`](../docs/android-build-signing.md)에 둔다.
- `android/README.md`에는 이 문서로 가는 짧은 링크만 유지한다.

## SDK 버전 결정 (2026-08-03)

- **minSdk 26** (Android 8.0). 근거: apilevels.com(2026-05-28 갱신, 2026년 4월 Statcounter
  데이터) 기준 누적 기기 커버리지 96.1%. 첵췍은 일반 대중을 폭넓게 타겟하므로 최신 API
  활용보다 기기 커버리지를 우선.
- **targetSdk 36** (Android 16). 근거: Google Play 정책상 2026-08-31부터 신규 앱은
  targetSdk 36 이상이어야 제출 가능(연장 시 2026-11-01까지). 선택의 여지가 없어 정책
  최소치로 고정.
- 출처: [Target API level requirements - Play Console Help](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en),
  [API Levels - apilevels.com](https://apilevels.com/)

## 샘플앱: 검색·등록·아카이브 (2026-08-03)

리뷰 심사 기간 단축 검증용 최소 샘플앱. 도서 검색은 공개된 Chaekchaek API
`GET /api/v1/books`를 KMP 공유 모듈에서 호출한다. 등록/아카이브는 로그인 없이 기기
로컬(SharedPreferences)에만 저장한다. 서버 서재 API는 인증이 필요하므로 이번 샘플 범위에서
사용하지 않는다.

## 안드로이드 명령 실행 전 설명 (2026-08-03)

`android` CLI/`adb` 등 안드로이드 관련 명령을 실행하기 전에는 그 명령이 무엇을 하는지
먼저 한 줄로 설명한다. 훅으로는 강제 불가(PreToolUse 훅은 도구 호출 직전 명령어만 보고
그 앞에 설명이 있었는지는 검증할 방법이 없음 - 승인 프롬프트를 강제할 수는 있지만 그건
"설명"이 아니라 "승인"이라 요청과 다름). 그래서 훅 없이 에이전트(Codex/Claude Code
공통) 스스로 지키는 약속으로 둔다.

## API 로딩 인디케이터 규칙 (2026-08-19)

- API 요청이 500ms 이내에 끝나면 로딩 인디케이터를 표시하지 않는다.
- 500ms가 지나도 요청 중이면 로딩 인디케이터를 표시한다.
- 표시 후에는 최소 노출 시간을 두지 않고 API 응답 즉시 닫는다.

## Android 앱 버전 및 릴리스 커밋 (2026-08-21)

- 앱 버전의 단일 출처는 `app/build.gradle.kts`의 `defaultConfig`에 있는 `versionCode`와
  `versionName`이다. 다른 파일에 같은 값을 중복 선언하지 않는다.
- Play에 빌드를 올릴 때마다 `versionCode`를 Play Console에서 사용한 최댓값보다 크게 올린다.
  기본 증가 폭은 1이다.
- `versionName`은 SemVer(`MAJOR.MINOR.PATCH`)를 따른다. 변경 성격에 따라 모델이 증가 단위를
  판단한다. 호환되지 않는 변경은 major, 호환되는 기능 추가는 minor, 호환되는 버그 수정은
  patch다. 기존 심사 버전 `1.0`은 유지하고 다음 버전부터 세 자리 형식을 적용한다.
- 버전과 배포 상태, 변경 내역, 복구 기준은
  [`../docs/android-release-management.md`](../docs/android-release-management.md)에 함께 기록한다.

릴리스 절차는 `android` 디렉터리에서 아래 순서로 진행한다. 각 Android 명령을 실행하기 전에
명령의 목적을 한 줄로 먼저 설명한다.

1. `app/build.gradle.kts`의 두 버전과 릴리스 관리 문서를 갱신한다.
2. `./gradlew :app:assembleDebug`로 기본 컴파일과 패키징을 확인한다.
3. `./gradlew :app:verifyReleaseSigning`으로 릴리스 서명 설정을 확인한다.
4. `./gradlew :app:bundleRelease`로 서명된 AAB를 만든다.
5. `jarsigner -verify app/build/outputs/bundle/release/app-release.aab`로 서명을 검증한다.
6. AAB의 SHA-256을 릴리스 관리 문서에 기록하고 Play Console의 대상 트랙에 업로드한다.
7. 배포 후 release 커밋을 남긴다. 포맷과 변경 목록 추출은 `release-commit` 스킬을 따른다.

release 커밋 제목은 항상 `chore(release): vX.Y.Z 배포`로 쓴다. 본문 첫 줄에는 아래 형식으로
배포 대상, `versionName` 증감과 SemVer 증가 단위, `versionCode` 증감을 적는다.

```text
대상: Google Play <트랙> / A.B.C -> X.Y.Z (major|minor|patch), versionCode M -> N
```

본문 변경 목록은 직전 release 커밋 이후의 `git log`에서 추출하고 임의로 누락하지 않는다. 본문
맨 끝에는 실제 배포 트랙과 AAB 해시를 아래 형식으로 남긴다.

```text
Play track: <internal|closed|open|production>
AAB SHA-256: <hash>
```
