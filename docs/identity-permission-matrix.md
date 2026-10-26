# 열람자 인증, 작성자 유형과 익명 표시

2026-10-26 후속 정책: [서재·책장 확정 사항](bookshelf-decisions.md)의 작성자별 서재 이동 표를 함께 읽는다. 비익명 회원만 서재로 이동하며 익명 회원·비회원·탈퇴 회원은 이동을 막고 사유를 설명한다. 비로그인 사용자의 책 담기에는 로그인 필요를 안내한다. 아래 정적 분석 기록은 이 요구사항의 구현 완료 증거가 아니다.

인증·권한, 게스트 공개 상호작용, 작성자 표시 또는 로그인 유도를 바꿀 때 읽는다. 세 축을 분리해 현재 코드와 계약을 확인하기 위한 문서이며 새 정책을 정하거나 앱 구현을 변경한 기록은 아니다.

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
