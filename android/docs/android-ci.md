# Android CI 운영 및 학습 기록

## 자율 요구사항 기록

### 문제

Android 빌드와 테스트가 로컬에서만 실행되어, 깨진 코드가 PR에 포함돼도 머지 전에 자동으로
발견되지 않는다.

### 근거

- `app/src/test`에 단위 테스트가 있지만 실행 여부는 작업자에게 달려 있었다.
- CI 도입 전에는 Android 변경을 검증하는 GitHub Actions 워크플로가 없었다.

### 가설

Android 변경 PR마다 디버그 빌드, 단위 테스트, Android Lint를 실행하면 컴파일 오류, 회귀,
정적 분석 오류를 머지 전에 발견할 수 있다.

### 대안

| 대안 | 선택하지 않은 이유 |
| --- | --- |
| 로컬 체크리스트 | 실행을 잊거나 결과를 공유하지 않아도 막을 수 없다. |
| Git hook | 개인 환경에서 우회할 수 있고 GitHub의 머지 조건으로 사용할 수 없다. |
| GitHub Actions | 저장소가 이미 GitHub를 사용하고 PR 결과와 실패 로그를 한곳에서 확인할 수 있어 선택했다. |

### 실행

`.github/workflows/android-ci.yml`에 다음 검증을 추가했다.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

- Android 파일이나 워크플로가 바뀐 PR에서 실행한다.
- `main`이나 `an-develop`에 같은 변경이 push되면 다시 실행한다.
- JDK 21과 프로젝트의 Gradle Wrapper를 사용한다.
- [`gradle/actions/setup-gradle`](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md)의
  기본 캐시를 사용한다.
- 디버그 빌드만 검증하므로 keystore와 API 키를 CI에 저장하지 않는다.

### 관찰

로컬에서 CI와 같은 명령을 실행한 결과는 다음과 같다.

| 항목 | 결과 |
| --- | --- |
| 실행 시간 | 37초 |
| Gradle 태스크 | 53개 중 50개 실행, 3개 캐시 사용 |
| 빌드·테스트·Lint | 모두 통과 |
| GitHub Actions 첫 실행 | [PR #29](https://github.com/woowacourse-teams/2026-chaekchaek/actions/runs/31771357098)에서 성공 |
| 원격 실행 시간 | 전체 작업 3분 26초, Gradle 3분 15초 |
| 원격 Gradle 태스크 | 53개 중 52개 실행, 1개 캐시 사용 |
| 원격 Gradle 캐시 | PR은 읽기 전용이며 첫 실행이라 0개 복원, 0개 저장 |

### 학습과 다음 행동

- 기존 Gradle 태스크만 조합해 새 분석 도구 없이 첫 CI를 구성할 수 있었다.
- 첫 원격 실행에서 성공 여부, 실패 태스크명, 실행 시간을 GitHub에서 확인할 수 있었다.
- `an-develop`의 Android PR에는 실제 체크 이름인 `Build, test, lint`를 머지 필수 검사로
  지정한다. FE/BE PR에서는 Android CI를 실행하지 않으므로 `main`의 전역 필수 검사로는
  지정하지 않는다.
- 기본 브랜치인 `main`의 첫 실행에서 캐시가 저장된 뒤 후속 PR 실행 시간을 다시 비교한다.
- `shared` 모듈이 통합 브랜치에 들어오면 `:shared:testAndroidHostTest`를 검증 대상에 추가한다.
- 계측 테스트는 중요한 UI 회귀를 JVM 테스트로 잡을 수 없을 때 추가한다.

## 실행과 실패 대응

### 로컬 재현

저장소의 `android` 디렉터리에서 CI와 같은 명령을 실행한다.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

### 실패 원인 찾기

GitHub Actions의 `Build, test, lint` 작업에서 실패한 Gradle 태스크를 먼저 확인한다.

| 실패 태스크 | 먼저 확인할 것 | 로컬 결과 |
| --- | --- | --- |
| `:app:assembleDebug` | 컴파일 오류, 누락된 의존성·리소스 | `app/build/outputs/apk/debug/` |
| `:app:testDebugUnitTest` | 실패한 테스트명과 assertion | `app/build/reports/tests/testDebugUnitTest/` |
| `:app:lintDebug` | Lint 오류 위치와 설명 | `app/build/reports/lint-results-debug.html` |

로컬에서 수정하고 같은 명령이 통과하는지 확인한 뒤 다시 push한다. CI 환경에만 필요한 비밀값을
추가해서 통과시키지 않는다.

## 현재 범위

에뮬레이터가 필요한 계측 테스트, 릴리스 서명, 별도 정적 분석 도구는 포함하지 않는다. 현재 문제를
검증하는 데 기존 단위 테스트와 Android Lint로 충분하며, 추가 비용이 필요한 사례가 생기면 범위를
확장한다.
