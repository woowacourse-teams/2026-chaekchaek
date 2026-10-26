# 열람자 인증, 작성자 유형과 익명 표시

2026-10-26 후속 정책: [서재·책장 확정 사항](bookshelf-decisions.md)의 작성자별 서재 이동 표를 함께 읽는다. 비익명 회원만 서재로 이동하며 익명 회원·비회원·탈퇴 회원은 이동을 막고 사유를 설명한다. 비로그인 사용자의 책 담기에는 로그인 필요를 안내한다. 아래 정적 분석 기록은 이 요구사항의 구현 완료 증거가 아니다.

인증·권한, 게스트 공개 상호작용, 작성자 표시 또는 로그인 유도를 바꿀 때 읽는다. 초기 정적 조사와 이후 확정된 정책을 구분한다. 정책 확정은 앱 구현이나 실제 실행 검증의 완료를 뜻하지 않는다.

## 기준과 확인 범위

- 확인일: 2026-10-21. 코드 기준: `origin/an-develop`의 `192ab0cc`. Android와 iOS가 공유하는 `android/shared/src/commonMain`을 정적으로 확인했다.
- [운영 OpenAPI](https://api.chaekchaek.com/docs/openapi3.yaml)를 TLS 검증을 유지한 curl로 받아 로컬 [openapi3.yaml](../android/openapi3.yaml)과 바이트 비교했다. 두 파일의 SHA-256은 `19fd60bd763c9eaa992963465825cc3622cf8f10cccde7eec39d1f41c9690bfd`로 같았다.
- API 계약 확인은 실제 인증 요청이나 기기 흐름 성공을 뜻하지 않는다. Android/iOS 실기기, 운영 게스트 토큰, 로그인 후 계승과 토큰 만료 흐름은 이 작업에서 실행하지 않았다.

## 독립된 세 축

| 축 | 값과 의미 | 다른 축으로 추정하면 안 되는 것 |
| --- | --- | --- |
| 열람자 인증 | 회원 access token, 게스트 token, 인증 정보 없음 | 지금 읽는 사람이 비로그인이라고 작성자도 게스트인 것은 아니다 |
| 작성자 유형 | 응답 `author.actorType`: MEMBER / GUEST | MEMBER도 익명 표시일 수 있다. 표시 이름으로 유형을 추정하지 않는다 |
| 익명 표시 | 응답 `author.anonymous` | 익명은 작성자 표시 속성이다. 열람자 로그인 여부나 읽기 권한을 뜻하지 않는다 |
| 소유권 판단 | 응답 `author.mine` → 공통 모델 `writtenByMe` | 닉네임 일치, 익명 여부, MEMBER/GUEST만으로 수정·삭제 권한을 계산하지 않는다 |

관련 코드: [FeedViewModel](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/feed/FeedViewModel.kt)의 `currentViewer`, [ReviewFeedRemoteRepository](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/data/remote/ReviewFeedRemoteRepository.kt)의 인증 헤더, [BookDetailRemoteRepository](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/data/remote/BookDetailRemoteRepository.kt)의 `ReviewAuthorDto`, `toBookReview`, `toReviewReply`.

2026-10-21 정적 조사 당시 DTO의 `actorType`은 `BookReview` 매핑에 전달되지 않았다. 2026-10-23 공개 서재 구현에서는 `AuthorProfile`에 작성자 ID·유형·익명·프로필 상태를 보존한다. `mine`은 기존 소유권 판단인 `writtenByMe`로 유지하며, 공개 조회 권한과 분리한다.

## 행동별 권한과 현재 구현

| 행동 | 정책·계약 | 현재 공통 코드 근거 | 별도 확인 |
| --- | --- | --- | --- |
| 감상 조회 | 도서 감상 ALL과 MINE을 구분한다. 계약상 MINE은 인증 필요 | `OpenMineFeed`는 회원 요구, 화면에서 비회원 MINE 선택 시 로그인 요청. 전체 피드는 `FeedViewer.Anonymous/Guest/Member`로 조회 | 인증 상태별 서버 응답과 실제 본문 표시를 각각 검증. 익명 표시를 본문 차단 조건으로 추가하지 않는다 |
| 감상·답글 작성, 좋아요 | 회원 또는 게스트 인증 허용 | `BookDetailAuthenticatedAction`의 공개 쓰기는 `requiresMember=false`, `BookDetailViewModel.publicWrite`가 게스트 자격을 발급·사용 | 화면마다 같은 정책이 적용되는지는 호출 경로와 기기로 확인 |
| 내 감상·답글 수정·삭제, 좋아요 취소 | 계약상 자신이 생성한 데이터만 허용 | 응답 mine 매핑과 기존 자격으로 요청. 게스트 401의 소유권 상실은 신규 작성 재시도와 분리 | 목록 조회의 게스트 mine, 재실행 후 소유권은 실제 API/기기 미검증 |
| 서재·별점·독서 상태·쪽수 | 회원 전용 | `AddToLibrary`, `OpenRating`, `ChangeStatus`, `OpenPageInput`, `SavePage`는 회원 요구. 게스트 요청에서 쪽수 필드 제거 | 화면 진입과 저장 결과는 플랫폼별 미검증 |
| 작성 화면의 익명 표시 | 열람자 인증과 구분 | `BookDetailScreen`의 신규 입력은 `signedIn && anonymousReviews`, `BookDetailSheets`의 편집 표시는 기존 감상의 anonymous 우선 | 표시 인수가 서버의 익명 상태를 새로 정한다는 뜻은 아님. 실제 저장 계약은 해당 API를 확인 |
| 로그인 시 게스트 기록 | 최신 계약과 과거 계획을 구분 | `AuthViewModel`은 로그인에 게스트 토큰을 넘기고 성공 시 로컬 게스트 정보를 정리 | 최신 계약의 웹 계승 설명은 감상·답글과 좋아요 이전을 구분한다. 모바일에서도 같은 결과라고 추정하지 않는다 |

코드 진입점: [BookDetailModels](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/bookdetail/BookDetailModels.kt), [BookDetailScreen](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/bookdetail/BookDetailScreen.kt), [BookDetailViewModel](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/bookdetail/BookDetailViewModel.kt), [BookDetailSheets](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/bookdetail/BookDetailSheets.kt), [AuthViewModel](../android/shared/src/commonMain/kotlin/com/chaekchaek/app/auth/AuthViewModel.kt).

## 과거 계획과 변경 시 확인

[게스트 쓰기 권한 계획](../android/docs/plans/guest-write-permissions.md)은 2026-08-27의 결정·구현 계획이다. 그 문서의 미구현 설명과 API 행 번호, GuestQuota 상태, 계승·만료 설명을 현재 구현으로 간주하지 않는다. 최신 계약은 게스트 토큰 갱신 경로도 설명하므로 과거 문서의 재발급 계획과 구분한다.

동작 변경은 `flow-first`로 현재/목표 행동과 보존 상태를 먼저 정리한다. 작성자의 익명성을 바꾸는 요구와 열람자에게 로그인을 요구하는 변경을 별개로 다룬다. 미확정 권한은 이 표를 근거로 임의 결정하지 않는다.

## 타인 공개 서재 조회 확정 정책 (2026-10-23)

[이슈 #607](https://github.com/woowacourse-teams/2026-chaekchaek/issues/607)의 구현 기준이다. 공통 앱 코드에 공개 조회와 프로필 이동을 추가했으며, 빌드·실행 결과는 아래 검증 기록으로 구분한다.

### 사용자 행동과 조회 범위

로그인 여부와 관계없이 사용자가 공개된 회원 프로필을 누르면 해당 회원의 서재에서 책장 목록과 책장별 책·독서 상태를 보고 기존 책 상세 화면으로 이동한다.

| 작성자 상태 | 프로필을 눌렀을 때 | 표시 정책 |
| --- | --- | --- |
| 비익명 회원 | 해당 회원의 서재 열기 | 서버의 작성자 이름과 프로필 이미지 사용 |
| 익명 회원 | 이동하지 않고 “익명으로 작성한 사용자의 서재는 볼 수 없어요.” 안내 | 익명 이름과 기본 이미지 사용 |
| 비회원 | 이동하지 않고 “비회원으로 작성한 사용자는 서재가 없어요.” 안내 | 비회원 작성자 표시 유지 |
| 탈퇴 회원 | 이동하지 않고 “탈퇴한 사용자의 서재는 볼 수 없어요.” 안내 | 탈퇴 사용자 표시 유지 |

- 홈, 피드, 책 상세와 답글을 포함한 실제 작성자 프로필 진입점에 동일하게 적용한다.
- 책장은 모두 공개하며 서버가 반환한 책장 순서와 책 순서를 유지한다. 같은 책이 여러 책장에 있으면 각 배치를 유지한다.
- 책과 독서 상태를 보여주며 별점·감상 조회 UI를 추가하지 않는다. 이 화면의 담기 기능은 추가하지 않고 기존 책 상세를 사용한다.
- 빈 서재는 “아직 담은 책이 없어요” 안내를 제공한다. 빈 책장과 조회 실패를 구분한다.
- 실제 조회 오류는 [모바일 공통 오류 정책](mobile-error-policy.md)의 공통 다이얼로그로 안내하며 재시도와 닫기에서 현재 화면과 기존 내용을 유지한다. 뒤로가기로 이전 화면에 돌아갈 수 있어야 한다.

### 화면 왕복과 상태 보존

- 서재 → 책장 → 책 상세에서 뒤로가면 직전 화면으로 돌아간다.
- 각 화면의 필터, 선택과 스크롤을 보존하고 연속 탭으로 같은 화면을 중복해서 열지 않는다.
- A 서재 → 책 상세 → A 프로필처럼 이미 열린 서재를 다시 선택하면 기존 A 서재로 돌아간다. 새 A 서재를 쌓지 않는다.
- Android와 iOS에서 비로그인 열람, 접근 불가 작성자 안내, 책 상세 왕복, 기존 서재 복귀, 목록 상태 보존, 빈 서재와 오류 재시도를 각각 실제 실행으로 확인한다.

### API 기준과 구현 경계

- 사용자는 운영 API의 평면 책 목록 대신 전달받은 새 API 명세에 맞춘 여러 책장 구조 구현을 선택했다. 비교한 로컬 자료는 `/Users/ujeonghyeon/Downloads/openapi3 (1).yaml`이다. 운영 배포 여부는 별도 확인 대상이다.
- `GET /api/v1/libraries?memberId={memberId}`로 책장 목록과 도서 요약을, `GET /api/v1/libraries/{libraryId}`로 책장 소유자의 독서 상태를 포함한 도서를 조회한다. 둘 다 로그인 없이 조회하는 계약이다.
- 작성자 상태는 API의 `actorType`, `anonymous`, `profileStatus`, `memberId` 계약으로 판단한다. 표시 이름 문자열로 작성자 유형을 추정하지 않는다. `origin/main`의 `ReviewService.authorOf`와 `HomeService`에서 익명은 ID·이미지를 숨기며, 비익명 탈퇴 회원은 MEMBER·UNAVAILABLE과 탈퇴 표시를 반환하는 경로를 확인했다. 필드가 빠진 과거 응답은 이름으로 추측하지 않고 접근을 차단한다.
- 가입·닉네임 검증·기본 책장 생성, 내 서재 전환, 담기, 책장과 책의 편집·순서 저장, 이미지 공유, 질문 이모지 변경과 서버 수정은 이 이슈의 구현 범위가 아니다.
- 다른 작업과 공유하는 최신 이름 정책은 닉네임 1~10자, 한글·영문·숫자·`-`·`_` 허용이다. 가입 시 기본 서재의 이름은 접미어 없이 닉네임과 같다. 조회 화면은 서버가 반환한 이름에 `의 책장`이나 `의 서재`를 붙이지 않는다. 기존 이름을 조회할 때 새 이름 입력 제약으로 자르거나 정규화하지 않는다.

### 앱 구현과 디자인

- [타인 서재 책장 목록](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1588-6560), [읽기 전용 책장 상세](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1588-6716), [빈 책장](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1588-7008), [빈 서재](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1588-7099), [로딩](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1588-7145), [조회 실패](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1588-7193), [열람 불가](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1588-7253)를 기존 A안 섹션에 추가했다. 공통 Avatar·FilterChip·ErrorDialog·Loading 인스턴스를 재사용한다. 팀 라이브러리 게시는 수행하지 않았다.
- `AuthorProfile`에서 작성자 접근을 판단한다. 홈 최신 감상, 피드 감상, 발견 감상, 책 상세 감상과 답글의 이름·아바타를 해당 entry에 묶인 이동 콜백으로 연결한다. 상단 내 프로필 버튼은 기존 마이페이지 이동을 유지한다.
- `PublicLibraryRemoteRepository`는 공개 GET만 제공하고 쓰기 토큰을 전송하지 않는다. 책장 순서와 책 순서, 여러 책장에 담긴 같은 책과 과거 30권 초과 데이터를 유지한다. 서버가 반환한 다른 소유자의 자료는 화면에 표시하지 않는다.
- `PublicLibraryViewModel`은 entry 수명 동안 조회 응답과 진행 중 요청을 소유하며 오류·재시도는 공통 정책에 위임한다. 필터와 세로·가로 목록 위치는 Navigation 3의 saveable entry 상태에 둔다. 로딩 중 임시 빈 목록으로 복원 위치를 덮어쓰지 않는다.
- 객체지향 관점에서 기존 `AppNavigation`은 의존 객체 생성, 인증 동기화와 이동을 함께 담당해 단일 책임 원칙의 책임 집중이 남아 있다. 이 작업은 접근 정책과 공개 조회 책임을 별도 타입으로 분리했으며 전체 내비게이션 구조 개편은 포함하지 않는다.

### 다른 세션에 남기는 합의

이번 작업은 위의 타인 공개 조회만 가져온다. 다음은 별도 구현 범위다.

- 내 서재의 여러 책장 전환, 책장 만들기·이름 변경·삭제, 책장·책 순서 편집과 저장.
- 담기 시 최근 책장 자동 선택, 다른 책장 선택과 확정, 온보딩의 기본 책장 저장, 여러 책장 중복 배치, 같은 책장 중복 방지와 책장당 30권 저장 제한. 비회원 담기 안내는 “책을 담으려면 로그인이 필요해요.”다.
- 회원가입 기본 책장 생성과 기존 사용자 마이그레이션. 기본 이름은 닉네임 그대로이며 닉네임은 1~10자, 한글·영문·숫자·`-`·`_`만 허용한다.
- 이미지 공유와 감상 없는 책의 플레이스홀더. 링크 공개 범위·수명·취소·수정 반영 논의는 제외한다.
- 책 상세 질문의 별 삭제와 `good`, `heart`, `think`, `calm`, `sad` 유지.
- 저장 실패의 로컬 처리와 복구. 이번 공개 조회 작업에서 쓰기 실패 정책을 새로 정하지 않는다.

책장 이름 최대 길이·책장 수 제한, 질문 이모지별 표시 이름·의미, 구체적인 저장 실패 복구는 별도 논의가 남아 있다. 조회 화면에서 이 제한을 임의로 적용하지 않는다.

### 초기 검증 기록

- 운영 OpenAPI를 2026-10-23 다시 조회했으며 `/api/v1/libraries` 경로가 아직 없다. 사용자 지정 새 명세를 구현한 것으로, 운영 배포·실연동 완료를 뜻하지 않는다.
- 공통 테스트 430개가 실패·생략 없이 통과했다. 접근 행렬, 익명 이미지 차단, 작성자 DTO 매핑, 공개 GET의 인증 헤더 부재, 순서·빈 책장·여러 배치·31권 응답, 오류 전파, 중복 이동·기존 서재 복귀와 지연 로딩·명시적 재시도를 포함한다.
- `483844f8`의 [CI 빌드·테스트·Lint](https://github.com/woowacourse-teams/2026-chaekchaek/actions/runs/39945270719/job/120386287889)가 통과했다. Android 앱·계측 APK 빌드와 로컬 Lint도 통과했다.
- `android/scripts/public-library-fixture.py`의 같은 로컬 HTTP 계약으로 아래 실제 앱 테스트를 실행했다. 운영 데이터에는 쓰지 않았다.

| 플랫폼 | 실행 코드와 기기 | 결과와 범위 |
| --- | --- | --- |
| Android | `80e31823`, 전용 `chaekchaek_public_607` AVD, Android 16 / API 36, `emulator-5594` | `PublicLibraryNavigationTest` 2개 통과, 10.082초 |
| iOS | `5fd379f3`, iPhone 17 Pro / iOS 26.2 Simulator, `74464F96-99AB-4F02-BFC2-A8D7C082ACE1` | `PublicLibraryUITests` 2개 통과, 63.334초 |

- 두 플랫폼에서 비로그인 열람, 책장 → 책 상세 → 책장 왕복, 읽는 중 필터와 스크롤 위치 보존, 기존 A 서재 복귀, 빈 서재, 조회 실패 후 재시도와 익명·비회원·탈퇴 작성자별 차단을 확인했다. iOS 검증 후에는 Android 계측 APK 버전과 테스트 캡처, 검증 문서만 보완했으며 공통 앱 동작은 같다.
- Android 원격 플리파 연결이 접수 전에 실패해 공용 대기열과 기기 임대 안에서 로컬 검증으로 전환했다. 앱·계측·Portal APK의 패키지·버전·서명을 비교한 뒤 데이터 보존 설치했다. SDK 37 서명 출력과 기존 검사기의 형식 차이는 SDK 36 도구 선택으로 해결했으며 검사기를 우회하지 않았다.
- Jev 준비 스크립트가 추상 Activity를 지정해 중단된 뒤 실제 매니페스트의 `MainActivity`로 바로잡았다. 이후 데이터 보존 업데이트·전면 실행·트리 조회까지 성공했다. 앱 APK는 `1.3.1 (10)`, SHA-256 `b30d2f76426a75150cb99c66e70fd3af35bbfa4e5fadd2632fe7066cfa062b67`이며 설치본과 같은 서명을 확인했다. 최종 계측 APK는 `1.0 (1)`, SHA-256 `3d876580967cf9bc7efdb5c45d2513e1d2a1fa1a9503d0f15931ee56894306dc`다.
- Jev 고정 설치본은 소스 `a43a0e917e1aed0eef3d1b46dcbf4e100055ae60`, 패키지 `0.2.0`, Droidrun과 Portal `0.7.27`을 사용했다. 실제 트리에서 선택자를 확정하고 형식 검사 후 실행했다. 피드 탭 진입과 비로그인 작성자 → 공개 서재 조회는 각각 1단계로 통과했다.
- Jev 시스템 뒤로가기 실행은 요청·응답 후 즉시 트리에 서재가 남아 `blocked`로 끝났다. 낮은 확신도나 단계 제한을 완화하지 않았다. 별도 후속 관찰에서 선택된 피드와 독자A 프로필이 다시 표시되는 것을 확인했으며, 해당 자동 실행을 통과로 바꾸지는 않는다. 기기 행동 성공과 자동 판정의 한계를 구분한다.
- Jev 3회 실행의 총 API 호출은 4회, 입력 34,915토큰·출력 187토큰이다. 근거는 현재 호스트의 `/tmp/607-jev-feed-run-1`, `/tmp/607-jev-library-run-1`, `/tmp/607-jev-back-run-1`의 `result.json`과 `events.jsonl`, 후속 `/tmp/607-android-tree-after-back.log`다. 원문 트리와 비밀 환경파일은 커밋하지 않는다.
- 검증 후 키보드·접근성 설정을 복원하고 전용 Android 에뮬레이터 종료·임대 반환을 확인했다. iOS 전용 Simulator도 종료했다. 기본 글자 크기의 새 목록·상세 화면을 양 플랫폼에서 캡처로 확인했으며 확대 글자·전체 접근성 감사나 운영 서버 저장 검증을 대신하지 않는다.
- 로컬 근거: Android `/tmp/607-android-capture-ui.log`, iOS `/tmp/607-ios-public-library-v6.xcresult`. Android 목록·상세 캡처는 `/tmp/607-android-public-library-final.png`, `/tmp/607-android-public-bookshelf-final.png`이며 1080×1920이다. iOS 캡처는 `/tmp/607-ios-library-final.png`, `/tmp/607-ios-bookshelf-final.png`이며 1206×2622다. 임시 경로는 현재 검증 호스트에만 존재한다.

### 추가 UI 검증 (2026-10-24)

홈·발견·답글 진입과 백스택의 미검증 항목을 양 플랫폼의 기존 UI 테스트에 추가했다. 앱 동작 코드는 변경하지 않았다. 같은 로컬 HTTP fixture를 사용했으며 아래 결과는 운영 API 배포나 실연동의 근거가 아니다.

| 플랫폼 | 실행 소스와 기기 | 최종 결과 |
| --- | --- | --- |
| Android | `909dff1e5df7eb2781107c35a734942cd2f23b97`, 전용 `chaekchaek_public_607`, Android 16 / API 36, `emulator-5594` | `PublicLibraryNavigationTest` 6개 통과, 27.552초. 별도 실제 `MainActivity` 종료·재실행 확인도 통과 |
| iOS | `c24b08a17fc6e9ef899f10343d53b84a72d362de`, iPhone 17 Pro / iOS 26.2, `74464F96-99AB-4F02-BFC2-A8D7C082ACE1` | `PublicLibraryUITests` 6개 통과, 206.765초, 실패·생략 0개 |

- 홈·발견의 프로필에서 서재를 열고 뒤로 가면 원래 탭과 작성자 위치가 유지된다.
- 책 상세의 답글 작성자를 연속 탭하면 기존 A 서재로 돌아가고, 그 뒤로 가기는 원래 피드로 복귀한다. 연속 진입·뒤로가기와 시스템 뒤로가기를 섞어도 루트 탭을 사용할 수 있다.
- 두 플랫폼에서 화면 가장자리 뒤로가기 제스처를 취소하면 현재 책장에 남는다. iOS는 이어지는 긴 제스처로 실제 이전 화면 복귀도 확인했다. Android의 실제 Activity 종료·재실행과 iOS 앱 종료·재실행 후 루트 탭·프로필 재진입도 확인했다.
- 서재 책장 카드와 책장 상세의 가로 책등 위치, 상세의 독서 상태 필터·세로 위치가 왕복 후 유지된다. 가로 위치는 17번째 책등의 X 좌표로도 확인했다.
- 빈 책장 안내를 확인했다. 조회 오류를 닫으면 현재 소유자 화면을 유지하며 추가 요청하지 않고, 나갔다 다시 들어갈 때 새로 조회한다. 명시적 재시도와 빈 서재, 익명·비회원·탈퇴 작성자 차단도 재검증했다.
- A 책장 → 책 X 상세(페이지순) → B 서재·책장 → 같은 X 상세(최신순) → 원래 상세(페이지순)에서 진입별 상태가 독립적으로 유지된다.
- 초기 실패는 Android 홈 바깥 목록 스크롤 선택, iOS의 취소 의도와 달리 완료된 제스처, Swift 6 테스트 함수의 액터 격리 문제였다. 테스트를 보완한 위 최종 실행은 모두 통과했다. `909dff1e` 이후 소스 차이는 iOS 테스트 보조 함수의 `@MainActor` 한 줄이며 Android 소스·공통 앱 코드는 동일하다.
- 플리파는 연결됐지만 해당 브랜치와 UI 계측을 지원하지 않아 원격 작업을 접수하지 않았다. 사용자에게 로컬 Android UI 검증을 명시적으로 승인받고 공용 대기열·전용 AVD 임대 안에서 수행했다. 같은 버전·서명을 확인한 `adb install -r`로 데이터를 보존했으며 Android 앱 APK SHA-256은 `a9d670743c21f88a9fefd6b01404901463a7c0f048e364827523fb9198c6c70d`, 계측 APK는 `c768dd8da7623820e30bfa9a5b9ff5251fa2cb5ac0be79a89817febf5031c385`다.
- 로컬 근거는 Android `/tmp/607-remaining-final-android-ui.log`, `/tmp/607-remaining-final-android-root-relaunch.txt`, iOS `/tmp/607-ios-public-library-final.xcresult`다. Android 캡처 `/tmp/607-remaining-final-android-public-bookshelf.png`, `/tmp/607-remaining-final-android-reply-mixed-back-root.png`와 iOS 첨부 `/tmp/607-ios-final-attachments`에서 기본 크기의 화면을 직접 확인했다. 임시 경로는 현재 호스트에서만 유효하다.
- 검증 뒤 전용 Android AVD 종료·임대 반환과 iOS 글자 크기 복원·Simulator 종료, 공용 대기열 반환을 확인했다. 확대 글자·전체 접근성 감사·운영 API 실연동은 이번 결과에 포함하지 않는다. 기존 `AppNavigation`의 단일 책임 원칙상 책임 집중은 앞서 기록한 상태와 같다.
