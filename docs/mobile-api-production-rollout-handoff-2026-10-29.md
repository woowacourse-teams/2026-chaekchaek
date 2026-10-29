# 모바일 API production 승격 호환성 조사 핸드오프

## 목적

development 서버의 책장·도서 계약을 production에 반영할 때 심사 또는 내부 테스트 빌드
`1.3.2 (12)`를 정상화하면서 이미 설치된 구버전 앱을 깨뜨리지 않는 배포 범위를 확정한다.
이 문서는 조사 시작점이며 서버 배포 승인이나 완료 기록이 아니다.

확인일은 2026-10-29이다. 서버 브랜치, OpenAPI, 스토어와 심사 상태는 바뀔 수 있으므로
다음 세션은 아래 명령과 원본을 다시 조회한 뒤 판단한다.

## 사용자 요청과 현재 상황

- 사용자는 dev와 prod의 싱크 불일치 때문에 앱이 종료되거나 주요 기능이 실패하며,
  심사에 올린 앱도 같은 영향을 받는다고 보고했다.
- Release 빌드는 Android와 iOS 모두 production API만 사용하도록 빌드 단계에서 제한한다.
  앱 바이너리를 dev 서버로 돌리는 방식은 해결책으로 사용하지 않는다.
- `1.3.2 (12)` Android 내부 테스트와 iOS TestFlight용 산출물은 준비됐지만, 이 문서 작성
  시점에 스토어 심사 상태를 콘솔에서 다시 조회하지 않았다. 사용자의 최신 설명을 존중하되
  제출, 처리, 심사 중, 승인, 게시 상태는 다음 세션에서 각각 확인한다.
- 2026-10-28 Android 물리기기 `SM-S931N`에는 `1.3.2 (12)` Release APK를 USB로
  설치했다. 내부 테스트 업로드와 같은 커밋 `5082264f`의 빌드이지만 Play 서명과 Play 설치
  경로의 검증 근거는 아니다. 기존 Debug 서명 앱은 사용자 승인 후 삭제되어 로컬 앱 데이터와
  로그인 상태가 초기화됐다.

## 현재 확인된 원본과 기준

### 서버와 OpenAPI

2026-10-29에 원격 브랜치를 fetch하고 확인한 값이다.

| 대상 | 기준 |
| --- | --- |
| production 서버 브랜치 | `origin/be@979bef7d17e63547f317d53cd8b2779f6bc9ab32` |
| development 서버 브랜치 | `origin/be-dev@af64d127d6bcb7d765cde767d8afc3c0fb65269f` |
| be 대비 be-dev 커밋 | 29개 앞섬 |
| backend diff | 108개 파일, 12,579줄 추가, 297줄 삭제 |
| production OpenAPI | `https://api.chaekchaek.com/docs/openapi3.yaml` |
| development OpenAPI | `https://dev-api.chaekchaek.com/docs/openapi3.yaml` |
| production OpenAPI SHA-256 | `658848d849f3e56d7a926d88810f2c6a00b22a1974bd3db8371809d861d86c73` |
| development OpenAPI SHA-256 | `c30c5bd6cdb31ce362562d683e54d2bf8ec3fcb4a717bd06952aa4a5d0da753a` |

OpenAPI 경로는 production 33개, development 40개다. development에만 있는 경로는
다음 7개다.

- `GET/POST /api/v1/libraries`
- `/api/v1/libraries/order`
- `/api/v1/libraries/{libraryId}`
- `/api/v1/libraries/{libraryId}/books/order`
- `/api/v1/libraries/{libraryId}/books/{bookId}`
- `/api/v1/books/by-isbn/{isbn13}/rating`
- `/api/v2/books/by-isbn/{isbn13}`

`1.3.2 (12)`의 `BookshelfRemoteRepository`와 `PublicLibraryRemoteRepository`는
`/api/v1/libraries` 계열을 호출한다. production에 이 경로가 없으므로 새 책장 흐름은
production에서 정상 완료할 수 없다.

### 앱 기준

- `android-v1.3.1-code10`: 구버전 호출과 DTO를 확인할 수 있는 태그다.
- `android-v1.3.2-code12`: 심사 또는 내부 테스트 후보의 현재 태그다.
- 실제 production으로 관찰된 `1.2.1 (7)`의 정확한 Git 태그는 없다. 과거 문서에는
  `1.2.2 (8)`이 스토어 사용 버전이었다는 사용자 확인도 있어 현재 활성 버전과 소스 기준은
  Play와 App Store에서 다시 확인해야 한다.
- Release 환경 제한은 `android/config/api-environment.gradle.kts`, 주소는
  `android/config/production.properties`와 `android/config/development.properties`를 확인한다.

## 구버전 호환성에서 확인된 위험

새 경로 추가만 보면 하위 호환이지만, 공통 경로의 계약도 달라졌다. 다음 항목은 OpenAPI와
`android-v1.3.1-code10` 소스를 함께 대조한 결과다.

### 감상 정렬 `PAGE` 제거

- production의 `GET /api/v1/books/{bookId}/reviews`는 `sort=PAGE`를 허용한다.
- development 계약은 `LATEST`, `OLDEST`, `POPULAR`만 허용한다.
- `1.3.1 (10)`의 `ReviewSort`와 상세 화면은 실제로 `PAGE`를 전송할 수 있다.
- development 구현을 그대로 승격해 `PAGE`를 거부하면 구버전에서 해당 정렬이 400으로
  실패할 수 있다. 구버전 사용 종료가 확인될 때까지 `PAGE`를 별칭으로 수용하거나 같은 의미로
  변환하는 방안을 검토한다.

### 별점 저장 응답의 nullability 변경

`PUT /api/v1/library/{bookId}/rating`의 development 응답은 production과 비교해 다음 필드의
nullability가 달라졌다.

- development: `rating`은 필수 non-null
- development: `status`, `currentPage`, `readingUpdatedAt`은 nullable
- `1.3.1 (10)`의 `LibraryRecordDto`는 `status: String`, `currentPage: Int`를 non-null로
  역직렬화한다.

서버가 실제로 null을 반환하면 구버전에서 역직렬화 실패가 발생한다. 화면 오류와 프로세스
종료 중 어느 결과가 되는지는 호출부 예외 처리와 실제 Crashlytics 또는 logcat으로 확인해야
한다. OpenAPI 차이만으로 사용자가 본 프로세스 종료의 단일 원인이라고 확정하지 않는다.

### 추가 응답 필드

development의 `GET /api/v1/books/by-isbn/{isbn13}`는 nullable
`isRegisteredInMyLibrary`를 추가한다. `1.3.1 (10)`의 Android와 iOS HTTP 클라이언트는
`ignoreUnknownKeys = true`를 사용하므로 이 추가 필드 자체는 안전한 방향이다. 실제
`1.2.1 (7)`에도 같은 설정이 있는지는 별도 확인한다.

### 닉네임 길이와 403 응답

- 닉네임 계약의 최대 길이는 production 100자, development 10자다.
- `1.3.1 (10)` UI는 이미 10자로 제한해 일반 앱 입력 영향은 작지만, 기존 저장 데이터와
  다른 클라이언트의 요청은 확인해야 한다.
- 여러 쓰기 API에 development의 403 응답이 추가됐다. 응답 코드 추가만으로 파싱이
  깨지지는 않지만, 권한 판정 변경이 구버전의 재로그인 반복이나 범용 오류 안내로 이어지는지
  실제 토큰과 계정 상태별로 검증한다.

## DB 이전과 배포 순서 위험

be-dev 승격은 컨트롤러 추가만이 아니다.

- `V8__add_library_book_placement_storage.sql`은 `library`에 기본 책장과 표시 순서를 추가하고,
  `library_book`을 만들며, `library_item`의 독서 상태 필드를 nullable로 변경한다.
- V8은 중복 회원·도서, 불완전한 독서 또는 별점 상태, 책장 소유자 불일치를 먼저 검사하고
  위반이 있으면 스키마 변경 전에 중단하도록 작성돼 있다.
- `library_shelves_after_v8.sql`은 Flyway V8 이후 별도로 실행하는 production/development
  backfill이다. 모든 애플리케이션 쓰기를 중단하고 한 MySQL 연결의 단일 batch로 실행하라고
  명시한다.
- production 모드는 회원 1번 37권, 회원 20번 36권이라는 승인된 개수를 하드코딩한다.
  실제 운영 데이터의 개수가 달라졌거나 승인하지 않은 30권 초과 회원이 있으면 backfill이
  중단된다. 실행 직전 운영 데이터 재집계가 필요하다.
- backfill 전에 애플리케이션을 새 코드로 열면 별점 전용 기록이나 새 배치가 섞여 이전이
  거부될 수 있다. 구 코드와 신 코드의 쓰기 중단 시점, V8, backfill, 서버 기동 순서를
  runbook으로 고정해야 한다.
- `LegacyLibraryServiceIntegrationTest`에는 기존 조회, 추가, 삭제, 일괄 삭제, 상태 수정과
  새 책장 배치를 함께 쓰는 회귀 시나리오가 있다. 테스트 존재는 production 데이터와
  무중단 배포 안전성을 증명하지 않으므로 운영 스냅샷 기반 사전 검증을 별도로 수행한다.

## 다음 세션의 우선 조사 순서

### 1. 실제 장애 증거 확보

1. 심사 또는 내부 테스트의 정확한 플랫폼, 버전, build, 설치 경로와 재현 동선을 기록한다.
2. Android는 Crashlytics와 logcat, iOS는 Organizer crash log 또는 Crashlytics에서 같은
   시각의 stack trace를 확보한다.
3. HTTP 404, 400, 401, 403, JSON 역직렬화 예외와 실제 프로세스 종료를 구분한다.
4. R8 Android라면 `1.3.2 (12)` mapping으로 난독화 stack trace를 복원한다.

### 2. 실제 활성 구버전 복원

1. Play production, internal, alpha, beta와 App Store/TestFlight의 현재 버전을 다시 조회한다.
2. production 사용자가 실제로 받을 Android AAB와 iOS build를 확인한다.
3. 태그가 없는 Android `1.2.1 (7)`은 Play artifact, 당시 커밋 기록, AAB 내부 클래스 중
   가능한 근거를 사용해 호출 경로와 DTO를 복원한다. 추정 소스를 확정본으로 취급하지 않는다.
4. 버전별 활성 사용자 수가 확인되지 않으면 구버전 종료를 가정하지 않는다.

### 3. 서버 변경을 호환 변경과 파괴 변경으로 분리

1. `origin/be..origin/be-dev` 29개 커밋을 기능별로 나눈다.
2. 새 `/libraries`, V8, backfill에 꼭 필요한 커밋과 캐시, ISBN 별점, 도서 상세 v2 등
   독립 변경을 분리한다.
3. 공통 API의 요청 enum, 필수값, nullability, 성공 응답과 오류 상태를 버전별 DTO와 대조한다.
4. `PAGE` 수용, legacy rating 응답의 non-null 보장, `/api/v1/library` 유지 기간을 명시한다.
5. 변경된 인증·인가 정책이 기존 access token, refresh token, guest token과 호환되는지 확인한다.

### 4. 마이그레이션 사전 검증

1. 읽기 전용 운영 스냅샷 또는 익명화 복제본에서 V8 guard 조회를 먼저 실행한다.
2. backfill의 승인 회원과 권수를 현재 데이터로 재확인한다. 하드코딩 값이 달라졌으면 원인을
   조사하고 임의로 숫자만 갱신하지 않는다.
3. V8과 backfill의 실행 시간, lock 범위, 쓰기 중단 시간과 rollback 방법을 측정한다.
4. migration 성공 후 legacy API와 새 API가 같은 기록을 어떻게 공유하는지 검증한다.
5. Flyway 완료 뒤 backfill 실패 시 앱 기동을 막는 운영 절차가 있는지 확인한다.

### 5. 버전 호환 매트릭스 실행

최소 다음 조합을 staging 또는 production 복제 환경에서 검증한다.

| 앱 | 필수 흐름 |
| --- | --- |
| 실제 production 구버전 | 로그인·토큰 재발급, 홈, 검색, 책 상세, 기존 서재 조회·담기·삭제, 별점, 감상 `PAGE` 정렬 |
| `1.3.1 (10)` | 위 흐름과 닉네임·익명 설정, 감상 작성·수정·답글 |
| `1.3.2 (12)` | 위 흐름과 책장 목록·상세·생성·이름 변경·삭제, 담기·꺼내기, 책장·책 순서 변경, 공개 서재 |

Android와 iOS를 별도로 실행한다. Debug의 development 성공, mock 테스트, USB 설치 성공을
production 서버와 스토어 서명 앱의 성공으로 대신하지 않는다.

## 권장 배포 형태

현 단계의 기본안은 be-dev 전체를 한 번에 production에 올리는 방식이 아니라, 다음 조건을
충족하는 하위 호환 서버 릴리스다.

1. `/api/v1/library`와 기존 성공 응답을 유지한다.
2. 구버전의 `PAGE` 정렬을 계속 수용한다.
3. legacy 별점 저장 응답의 `status`와 `currentPage`를 null로 반환하지 않는다.
4. V8과 backfill 완료 뒤 `/api/v1/libraries` 계열을 연다.
5. 새 앱의 production smoke와 구버전 호환 smoke가 모두 통과한 뒤 심사를 계속한다.
6. 호환 API 제거는 버전별 사용자 수와 스토어 업데이트 가능성을 확인한 뒤 별도 배포로 한다.

서버를 먼저 호환 상태로 만들 수 없다면 심사 바이너리를 유지하지 말고 더 높은 build의
클라이언트 hotfix를 다시 제출한다. Remote Config 강제 업데이트는 정책 지원 앱의 UI 차단일
뿐이며 서버 계약을 복구하지 않는다. 이미 배포된 정책 미지원 앱에도 소급 적용되지 않는다.

## 완료 기준

- 실제 crash stack trace 또는 비크래시 오류 증거로 장애 원인이 분류돼 있다.
- 활성 production 앱 버전과 각 버전의 소스 또는 바이너리 계약이 식별돼 있다.
- 공통 API의 파괴 변경이 제거되거나 버전별 호환 계층으로 격리돼 있다.
- 운영 데이터 복제본에서 V8, backfill, 재실행, 실패 rollback이 검증돼 있다.
- 구버전, `1.3.1 (10)`, `1.3.2 (12)`의 Android와 iOS 호환 매트릭스가 통과한다.
- production OpenAPI와 실제 응답이 일치하고, 배포 후 rollback 조건과 담당자가 기록돼 있다.
- 스토어 심사 상태와 production 게시 상태를 따로 확인했다.

## 재확인 명령

비밀값을 출력하거나 저장하지 않는 조회 명령만 예시로 둔다.

```sh
git fetch origin be be-dev an-develop --prune
git rev-parse origin/be origin/be-dev
git rev-list --count origin/be..origin/be-dev
git log --oneline --no-merges origin/be..origin/be-dev

curl --fail --silent --show-error \
  https://api.chaekchaek.com/docs/openapi3.yaml \
  -o /tmp/chaekchaek-prod-openapi.yaml
curl --fail --silent --show-error \
  https://dev-api.chaekchaek.com/docs/openapi3.yaml \
  -o /tmp/chaekchaek-dev-openapi.yaml
shasum -a 256 /tmp/chaekchaek-prod-openapi.yaml /tmp/chaekchaek-dev-openapi.yaml
```

## 관련 문서와 코드

- [API 환경 설정](android-api-environments.md)
- [모바일 원격 설정과 업데이트 정책](mobile-remote-config-updates.md#운영과-하위-호환)
- [모바일 QA 피드백 수정 계획](mobile-qa-feedback-plan-2026-10-28.md#서버-계약-재확인-결과)
- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/data/remote/BookshelfRemoteRepository.kt`
- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/data/remote/PublicLibraryRemoteRepository.kt`
- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/data/remote/BookDetailRemoteRepository.kt`
- `backend/src/main/resources/db/migration/V8__add_library_book_placement_storage.sql`
- `backend/src/main/resources/db/backfill/library_shelves_after_v8.sql`
- `backend/src/test/java/com/chaekchaek/library/service/LegacyLibraryServiceIntegrationTest.java`

