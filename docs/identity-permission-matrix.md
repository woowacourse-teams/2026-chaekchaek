# 열람자 인증, 작성자 유형과 익명 표시

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

DTO의 `actorType`은 현재 `BookReview` 매핑에 전달되지 않는다. `anonymous`와 `mine`은 각각 표시 상태와 `writtenByMe`로 전달된다. 후속 기능이 작성자 유형을 필요로 하면 현재 모델에 정보가 남아 있는지 먼저 확인한다. 이 문서 추가로 모델을 바꾸지는 않는다.

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

[이슈 #607](https://github.com/woowacourse-teams/2026-chaekchaek/issues/607)의 구현 기준이다. 이 절을 추가한 시점에는 앱 기능을 구현하지 않았으며 Android·iOS 빌드, 기기 실행과 새 백엔드 실연동을 검증하지 않았다.

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
- 작성자 상태는 API의 `actorType`, `anonymous`, `profileStatus`, `memberId` 계약으로 판단한다. 표시 이름 문자열로 작성자 유형을 추정하지 않는다. 탈퇴 구분의 실제 응답은 구현 시 확인한다.
- 가입·닉네임 검증·기본 책장 생성, 내 서재 전환, 담기, 책장과 책의 편집·순서 저장, 이미지 공유, 질문 이모지 변경과 서버 수정은 이 이슈의 구현 범위가 아니다.
- 다른 작업과 공유하는 최신 이름 정책은 닉네임 1~10자, 한글·영문·숫자·`-`·`_` 허용이다. 가입 시 기본 서재의 이름은 접미어 없이 닉네임과 같다. 조회 화면은 서버가 반환한 이름에 `의 책장`이나 `의 서재`를 붙이지 않는다. 기존 이름을 조회할 때 새 이름 입력 제약으로 자르거나 정규화하지 않는다.
