# 비로그인 사용자 공개 상호작용 허용 계획 (Codex 실행용)

- 출처: `ooo interview` 세션 `interview_20260827_085537` (2026-08-27, ambiguity 0.13, seed-ready)
- 상위 결정: 「비로그인 사용자의 권한 범위를 결정한다」 (2026-08-26)
- 실행 주체: Codex
- 작업 범위: `android/` 스코프 내부로 한정. **백엔드 변경 없음.**

## 1. 한 줄 목표

비로그인 사용자가 서버 발급 게스트 토큰으로 감상·답글을 작성·수정·삭제하고 좋아요를 등록·취소할
수 있게 하고, 로그인 시 그 기록을 계정으로 계승하도록 Android 클라이언트를 바꾸되, 서재·별점·
독서상태·쪽수는 로그인 전용으로 유지한다.

## 2. 이 작업의 실제 크기

인터뷰 중 코드를 조사한 결과, 이 작업은 "게이트 해제"보다 크다. 두 덩어리다.

**A. 게이트 해제 (기존 기능의 권한만 바꿈)** - 감상 작성, 답글 작성, 좋아요 등록/취소.
UI가 이미 있고 로그인 시트만 걷어내면 된다.

**B. 신규 기능 (없던 것을 만듦)** - 감상·답글 **수정/삭제**. 서버 API는 있지만
**Android 클라이언트에 저장소 메서드도 UI도 전혀 없다. 로그인 사용자에게도 없다.**
`BookDetailRemoteRepository`의 `delete`는 좋아요 취소용뿐이다(`:81`, `:97`).

B가 이 계획의 절반 이상을 차지한다. 6.7절이 그 부분이다.

## 3. 시작 전 확인 - 서버는 이미 다 되어 있다

`android/openapi3.yaml`을 원격 원본(`https://api.chaekchaek.com/docs/openapi3.yaml`)과 diff한
결과 **동일**했다 (2026-08-27 확인).

| 서버가 제공하는 것 | 위치 |
|---|---|
| `POST /api/v1/auth/guest-token` → `{guestToken, nickname, expiresAt}` | openapi3.yaml:295 |
| `PATCH` / `DELETE /api/v1/reviews/{reviewId}` (감상 수정·삭제) | openapi3.yaml:761 |
| `PATCH` / `DELETE /api/v1/replies/{replyId}` (답글 수정·삭제) | openapi3.yaml:640 |
| 위 쓰기 엔드포인트의 `X-Guest-Token` 헤더 지원 (`guestToken` security scheme) | openapi3.yaml:689, 2399 |
| 소셜 로그인 시 `X-Guest-Token`으로 게스트 Actor·닉네임·공개 상호작용 계승 | openapi3.yaml:939, 984 |
| 응답 `author.mine` (내가 작성했는지), `author.actorType` (`MEMBER`/`GUEST`) | openapi3.yaml:1958-1961, 1999-2002 |

계약서에 명시된 서버 규칙을 클라이언트 설계의 전제로 삼는다.

- "게스트는 자신이 생성한 데이터만 수정·삭제 또는 좋아요 취소할 수 있습니다." (openapi3.yaml:647)
- "토큰이 유실되거나 만료되면 기존 게스트 콘텐츠를 수정하거나 삭제할 수 없습니다." (openapi3.yaml:29)
- 게스트는 감상 작성 시 `currentPage`/`totalPages`를 입력할 수 없다. (openapi3.yaml:1243)
- 삭제는 soft delete이며 응답에 `deleted` 필드가 있다. (openapi3.yaml:761, 640)

**이 작업은 순수 Android 클라이언트 작업이다. 새 API를 만들거나 백엔드를 건드리지 않는다.**

## 4. 허용/차단 경계

| 기능 | 비로그인 | 근거 |
|---|---|---|
| 감상 작성·수정·삭제 | **허용** | 공개 데이터 |
| 답글 작성·수정·삭제 | **허용** | 공개 데이터 |
| 감상·답글 좋아요 등록·취소 | **허용** | 공개 데이터 |
| 서재 추가 | 차단(로그인 유지) | 개인 서재 |
| 별점 남기기 | 차단(로그인 유지) | 개인화(이전 별점 비교)에 사용 |
| 독서 상태 변경 | 차단(로그인 유지) | 개인 독서 기록 |
| 쪽수 입력 | 차단(로그인 유지) | 장기 누적 기록, 토큰 유실 시 복구 불가 |

## 5. 결정 사항

| 항목 | 결정 |
|---|---|
| 저장 위치 | 기존 `RefreshTokenStore` 확장. 새 클래스·새 의존성 없음 |
| 발급 시점 | **lazy** - 쓰기를 실제로 시도하는 순간 토큰이 없으면 발급 |
| 401 처리 | **신규 작성만** 재발급 후 1회 재시도. 수정·삭제·좋아요취소는 재시도 없이 안내 |
| 로그인 후 토큰 | 삭제. 계승은 로그인 요청 시점에 이미 끝났다 |
| 계승 | 로그인 요청에 `X-Guest-Token` 헤더 부착 (이번 범위 포함) |
| 게스트 닉네임 | 발급 응답의 `nickname`을 저장해 감상 작성 시트에 표시 |
| 게스트 익명 토글 | 숨김 (대응 API 없음) |
| 수정/삭제 진입점 | 내 감상·답글 카드의 케밥 `⋯` → 하단 시트 `[수정] [삭제]` |
| `GuestQuota` / `Viewer` | **삭제** |
| 검증 | 수동 QA 시나리오 + 단위 테스트 + gradle 그린 |

### 왜 401에서 종류를 나누는가

게스트 토큰을 재발급하면 서버 입장에서 **다른 Actor**가 된다. 그래서 일괄 재시도를 적용하면:

- 신규 작성(감상/답글 작성, 좋아요 등록) → 재발급 후 재시도가 **성공한다**
- 수정·삭제·좋아요 취소 → 새 Actor에겐 그 데이터에 대한 권한이 없어 **확실히 또 실패한다**

확실히 실패할 요청을 한 번 더 보내는 것은 낭비이므로, 후자는 재시도 없이
"이 기기에서는 더 이상 수정할 수 없습니다" 취지의 스낵바만 노출한다.

### 왜 `GuestQuota`를 삭제하는가

`GuestQuota`(비로그인 감상 전문 열람 3회)는 **강제되는 규칙이 아니라 장식용 더미다.**

`HomeViewModel.kt:72`에 `GuestQuota(viewed = 2)`가 하드코딩되어 있고, 바로 위 주석이
`// ponytail: 홈 피드가 더미인 동안 2/3 고정. 인증 연결 시 Viewer에서 매핑한다.` 이다.
이 값은 홈 배너의 `지금 2 / 3` 문구를 그리는 데만 쓰이며, **열람 횟수를 세거나 차단하는 코드는
어디에도 없다.** `Viewer` sealed interface는 프로덕션 코드에서 전혀 쓰이지 않는다.

여기에 "감상을 쓸 수 있는 사용자가 남의 감상은 3회만 본다"는 이번 결정과 정면 충돌한다.
미사용 코드 삭제이므로 회귀 리스크가 없다.

## 6. 설계

### 6.0 객체지향 관점의 근본 문제

지금 코드는 인증 수단을 **`accessToken: String`이라는 원시값으로** 저장소 12개 메서드에 그대로
꿰어 놓았다(`BookDetailRemoteRepository.kt:41-99`). 게스트 토큰을 추가할 때 `guestToken: String?`
파라미터를 하나 더 붙이는 방식으로 대응하면 다음 문제가 생긴다.

- **원시값 집착(primitive obsession)**: "인증 수단"이라는 개념이 타입으로 존재하지 않아
  `String` 두 개의 조합 규칙(어느 쪽이 우선인가, 둘 다 null이면?)이 호출부마다 흩어진다.
- **산탄총 수술(shotgun surgery)**: 인증 방식이 하나 늘 때마다 메서드 시그니처 12개가 바뀐다.
- **DIP 위반**: 저장소가 "인증 수단"이라는 역할이 아니라 "Bearer 토큰 문자열"이라는 구체적
  표현에 의존한다.

같은 문제가 UI 레이어에도 있다. `BookDetailViewModel.requestAuthentication()`
(`BookDetailViewModel.kt:52`)이 **모든** 액션에 대해 `accessToken != null` 하나로 판정한다.
액션마다 필요한 권한이 다르다는 사실이 타입에 없고 ViewModel의 조건문 안에 숨는다.

세 번째로, 서버가 **필수 필드로 내려주는 `author.mine`과 `author.actorType`을 클라이언트
`ReviewAuthorDto`(`BookDetailRemoteRepository.kt:266`)가 통째로 버리고 있다.** "이 감상이 내
것인가"라는 정보가 서버에 있는데 클라이언트 모델에 없어서, 수정/삭제 UI를 붙일 근거가 없다.
`deleted` 필드도 마찬가지로 버려진다.

### 6.1 인증 수단을 값 타입으로

```kotlin
// shared/src/commonMain/kotlin/com/chaekchaek/app/data/remote/WriteCredential.kt (신규)

/** 쓰기 요청에 붙일 인증 수단. 회원은 Bearer, 게스트는 X-Guest-Token. */
sealed interface WriteCredential {
    val headerName: String
    val headerValue: String

    data class Member(val accessToken: String) : WriteCredential {
        override val headerName get() = HttpHeaders.Authorization
        override val headerValue get() = "Bearer $accessToken"
    }

    data class Guest(val guestToken: String) : WriteCredential {
        override val headerName get() = GUEST_TOKEN_HEADER
        override val headerValue get() = guestToken
    }

    companion object {
        const val GUEST_TOKEN_HEADER = "X-Guest-Token"
    }
}
```

저장소는 헤더 이름을 분기하지 않고 `header(credential.headerName, credential.headerValue)`
한 줄만 쓴다. 인증 수단이 하나 더 늘어도 저장소는 열리지 않는다(OCP).

### 6.2 액션이 자기 권한 요구사항을 안다

`BookDetailAuthenticatedAction`(`BookDetailModels.kt:46`)에 프로퍼티를 하나 추가한다.

```kotlin
sealed interface BookDetailAuthenticatedAction {
    /** 회원 로그인이 반드시 필요한가. false면 게스트 토큰으로 수행 가능. */
    val requiresMember: Boolean
    ...
}
```

- `requiresMember = true`: `AddToLibrary`, `OpenPageInput`, `OpenRating`, `SavePage`,
  `ChangeStatus`, `OpenMineFeed`
- `requiresMember = false`: `OpenReview`, `LikeReview`, `CreateReply`, `LikeReply`,
  그리고 6.7에서 추가되는 수정/삭제 액션

그러면 `requestAuthentication()`은 액션 타입을 `when`으로 열거하지 않고 액션에게 물어본다
(Tell, Don't Ask). 액션이 하나 추가되어도 ViewModel을 고치지 않는다.

**참고(이번에 고치지 않아도 되는 것)**: 이 변경 후 `BookDetailAuthenticatedAction`이라는
이름은 "인증이 필요한 액션"이 아니라 "권한 확인이 필요한 액션"을 뜻하게 되어 이름과 의미가
어긋난다. 이름 변경은 호출부 20여 곳을 건드리므로 이번 범위에 넣지 않는다. 다만 이 어긋남을
알고 넘어간다.

## 7. 작업 단계

각 단계가 끝날 때마다 커밋한다.

### 7.1 GuestQuota / Viewer 삭제

| 파일 | 처리 |
|---|---|
| `shared/.../domain/reader/GuestQuota.kt` | 파일 삭제 (`GuestQuota`, `Viewer` 모두) |
| `shared/.../presentation/home/HomeViewModel.kt` | import + `GuestQuota(viewed = 2)`(:72) + `guestBanner`(:30, :75-78) 제거 |
| `shared/.../presentation/home/HomeUiState.kt:12` | `Content.guestBanner` 필드 제거 |
| `shared/.../presentation/home/HomeUiModel.kt:53` | `GuestBannerUiModel` 제거 |
| `shared/.../presentation/home/HomeUiModel.kt:84` | `HomeLabels.guestProgress(...)` 제거 |
| `shared/.../ui/home/HomeScreen.kt` | 게스트 배너 렌더링 제거 |
| `shared/src/commonTest/.../domain/reader/ReaderTest.kt:57` | `GuestQuotaTest` 클래스 제거 |
| `shared/src/commonTest/.../presentation/home/HomeViewModelTest.kt:115` | 배너 단언 제거 |

배너 자리를 다른 가입 유도 문구로 **교체하지 않는다.** 그냥 없앤다. 대체 문구는 UI 결정이
필요하므로 이번 범위 밖이다.

검증: `./gradlew :shared:allTests` 그린.

### 7.2 게스트 토큰 저장소 확장

`app/src/main/java/com/chamsae/chaekchaek/auth/RefreshTokenStore.kt`에 게스트 토큰과 닉네임을
추가한다. 기존 AndroidKeyStore AES/GCM + SharedPreferences(`auth_session`) 경로를 그대로 쓴다.

- 기존 `read()` / `write()` / `clear()`는 refresh token 용도 그대로 유지
- `readGuest()` / `writeGuest(token, nickname)` / `clearGuest()` 추가
- SharedPreferences 키: `guest_token`, `guest_nickname`. KeyStore alias는 기존
  `chaekchaek_refresh_token`을 재사용한다(같은 앱, 같은 신뢰 경계).

`shared/.../auth/AuthPlatformCallbacks.kt`에 콜백 3개를 추가하고,
`app/src/main/java/com/chamsae/chaekchaek/MainActivity.kt:56` 부근에서 연결한다.

### 7.3 게스트 토큰 발급·보관 (lazy)

`shared/.../data/remote/`에 `POST /api/v1/auth/guest-token` 호출을 추가한다.
응답 `{guestToken, nickname, expiresAt}`.

발급 규칙:

1. 쓰기 액션 시도 시점에 저장된 게스트 토큰이 있으면 그대로 쓴다.
2. 없으면 발급받아 저장한 뒤, 원래 요청을 이어서 보낸다.
3. **앱 시작 시 선발급하지 않는다.** 조회만 하는 사용자에게 서버 Actor를 만들지 않는다.

`expiresAt`은 저장하되 클라이언트가 선제적으로 만료를 계산해 재발급하지 않는다. 실제로 401이
왔을 때만 7.5의 규칙으로 처리한다(시계 오차로 인한 불필요한 재발급을 피한다).

**로딩 인디케이터**: 첫 쓰기는 발급 + 쓰기로 왕복이 2회가 된다. `android/CLAUDE.md`의 규칙
(500ms 이내면 인디케이터 미표시, 초과 시 표시, 응답 즉시 닫기)을 발급 + 쓰기를 합친 전체
구간 기준으로 적용한다. 발급과 쓰기에 인디케이터를 따로 두어 두 번 깜빡이게 하지 않는다.

### 7.4 DTO에서 버려지는 필드 복구

`shared/.../data/remote/BookDetailRemoteRepository.kt:266` `ReviewAuthorDto`에
서버가 필수로 내려주는 필드를 추가한다.

```kotlin
internal data class ReviewAuthorDto(
    val displayName: String,
    val anonymous: Boolean,
    val mine: Boolean,          // 추가 - 내가 작성했는지 (openapi3.yaml:2002)
    val actorType: String,      // 추가 - MEMBER / GUEST (openapi3.yaml:1962)
    val profileImageUrl: String? = null,
)
```

도메인 모델 `BookReview`(:149)와 `ReviewReply`(:166)에 `writtenByMe: Boolean`을 추가하고
매핑한다. 감상·답글 응답의 `deleted: Boolean`도 같이 살려 soft delete 상태를 표현한다.

이 필드가 7.7 케밥 노출 조건의 유일한 근거다. 클라이언트가 닉네임 문자열을 비교해 "내 것"을
추측하지 않는다.

### 7.5 WriteCredential 도입, 저장소 수정, 401 처리

6.1의 `WriteCredential`을 추가하고, **게스트 허용 쓰기 메서드만** 시그니처를 바꾼다.

| 메서드 | 변경 |
|---|---|
| `createReview` (:61) | `accessToken: String` → `credential: WriteCredential` |
| `createReply` (:87) | 동일 |
| `likeReview` (:76) / `unlikeReview` (:81) | 동일 |
| `likeReply` (:92) / `unlikeReply` (:97) | 동일 |
| `updateReview` / `deleteReview` / `updateReply` / `deleteReply` | **신규 추가** (7.7) |
| `addToLibrary` (:41), `updateReadingStatus` (:46), `updateCurrentPage` (:51), `rate` (:56) | **바꾸지 않는다.** 회원 전용이므로 `accessToken: String` 유지 |
| `detail`, `reviews`, `replies` (조회) | 7.8 참고 |

게스트로 감상을 작성할 때 `currentPage`/`totalPages`를 요청에 넣지 않는다(서버가 거부한다).

**401 처리** - 게스트 자격(`WriteCredential.Guest`)으로 보낸 요청에 한해:

- **신규 작성**(감상 작성, 답글 작성, 좋아요 등록): 게스트 토큰을 재발급해 저장하고 그 요청을
  **1회만** 재시도한다. 재시도도 401이면 그대로 실패로 노출한다.
- **기존 데이터 변경**(감상·답글 수정/삭제, 좋아요 취소): 재시도하지 않는다. 스낵바로
  "이 기기에서는 더 이상 수정할 수 없습니다" 취지의 안내만 노출한다. 다이얼로그나 새 화면을
  만들지 않는다.

회원 자격의 401은 기존 `AuthSession` 동작(`AuthSession.kt`의
`PERMANENT_AUTH_FAILURE_STATUS = 401` → `clear()`)을 그대로 둔다.

### 7.6 게이트 해제

`BookDetailModels.kt:46`의 `BookDetailAuthenticatedAction`에 `requiresMember`를 추가하고(6.2),
`BookDetailViewModel.requestAuthentication()`(`BookDetailViewModel.kt:52`)이 그 값을 보고
판정하게 바꾼다.

- `requiresMember = true` + 미로그인 → 기존대로 `LoginRequiredSheet` 노출
- `requiresMember = false` → 로그인 시트 없이 게스트 자격으로 바로 수행

로그인 시트가 계속 떠야 하는 나머지 두 지점은 **건드리지 않는다**:

- `RootScreen.kt:164` - 검색에서 책 등록(`pendingRegistration`)
- `RootScreen.kt:185` - 서재 편집 모드 진입(`showArchiveLoginSheet`)

### 7.7 감상·답글 수정/삭제 (신규 기능)

이 절이 이 계획에서 새로 만드는 부분이다. 회원·게스트 모두에게 적용된다.

**저장소** (`BookDetailRemoteRepository.kt`)

| 메서드 | 엔드포인트 |
|---|---|
| `updateReview(reviewId, request, credential)` | `PATCH /api/v1/reviews/{reviewId}` |
| `deleteReview(reviewId, credential)` | `DELETE /api/v1/reviews/{reviewId}` |
| `updateReply(replyId, content, credential)` | `PATCH /api/v1/replies/{replyId}` |
| `deleteReply(replyId, credential)` | `DELETE /api/v1/replies/{replyId}` |

요청 본문 스키마는 openapi3.yaml의 `review-update`(:761 블록), `reply-update`(:686 블록)
예시를 그대로 따른다.

**액션**: `BookDetailAuthenticatedAction`에 `EditReview`, `DeleteReview`, `EditReply`,
`DeleteReply`를 추가한다. 모두 `requiresMember = false`.

**UI** (`BookDetailScreen.kt` / `BookDetailSheets.kt`)

기존 컴포넌트만 쓴다. 새 디자인 토큰이나 새 색상을 만들지 않는다.

```
감상 카드 (writtenByMe == true 일 때만)
┌──────────────────────────────────┐
│ 닉네임          2일 전      ⋯    │ ← 케밥 아이콘
│ 감상 본문 ...                     │
│ ♡ 12   답글 3                     │
└──────────────────────────────────┘
       ⋯ 탭 → 하단 시트 [수정] [삭제]
```

- 케밥 `⋯`은 `writtenByMe == true`인 카드에만 렌더링한다. 남의 감상에는 나타나지 않는다.
- 하단 시트는 `LoginRequiredSheet.kt`가 쓰는 것과 같은 `ModalBottomSheet` 패턴을 따른다.
- **수정**: 기존 감상 작성 시트를 재사용하되 기존 내용(본문·인용·챕터·스포일러 여부)을 채워
  연다. 새 화면을 만들지 않는다.
- **삭제**: 확인 단계를 한 번 거친 뒤 실행한다. 서버가 soft delete이므로 성공 시 목록에서
  해당 항목을 제거하고 재조회하지 않는다(낙관적 갱신).
- 답글도 같은 패턴을 쓴다. 답글 수정은 답글 입력 필드를 내용이 채워진 상태로 연다.

**디자인 시스템 가드 훅 주의**: `~/.codex/hooks/chaekchaek-design-system-guard.py`가
`PreToolUse`에서 `/ui/` 경로 `.kt` 패치를 검사하고, 최근 스크린샷 검증이 없으면 **패치를
deny 한다.** 이 절의 모든 파일이 `/ui/` 경로이므로, 착수 전에
`/Users/ujeonghyeon/Downloads/designs.pen`에서 대상 노드 하나를 지정해 `TakeScreenshot`을
성공시켜 둘 것. `document`나 `SxMn5`는 게이트를 통과시키지 않는다.

### 7.8 조회 요청의 게스트 토큰 (미확인 영역 - 아래 10절 참고)

`mine`과 `likedByMe`는 서버가 요청에 실린 토큰으로 계산한다. 게스트가 쓴 감상에 케밥이
뜨려면 조회 응답의 `mine`이 `true`여야 한다.

`detail`(:22), `reviews`(:27), `replies`(:66)에 게스트 토큰이 있으면 `X-Guest-Token` 헤더를
같이 붙인다. 회원 토큰이 있으면 회원 토큰을 우선한다.

**단, `GET /api/v1/books/{bookId}/reviews`(openapi3.yaml:1106)에는 `security` 블록이 없어
게스트 토큰으로 조회했을 때 서버가 `mine`을 계산해 주는지 계약서에 명시가 없다.**
7.8은 "붙여서 보내고, QA 9번에서 실제로 `mine`이 오는지 확인"하는 단계다. 오지 않으면 백엔드에
요청해야 하며, 그때까지 게스트는 앱을 재시작하면 자기 감상의 케밥을 볼 수 없다.

### 7.9 게스트 닉네임 표시

`shared/.../ui/bookdetail/BookDetailSheets.kt:246`의
`if (anonymous) "이름을 숨겨서 표시돼요" else "'${nickname.ifBlank { "닉네임 없음" }}'으로 표시돼요"`
에서, `nickname`이 지금은 `Navigation.kt:142`의 `archiveState.nickname`(회원 프로필
`/api/v1/members/me` 기반)에서만 온다. 게스트는 이 값이 비어 있어
**"'닉네임 없음'으로 표시돼요"** 가 뜬다.

- 게스트일 때는 저장된 게스트 닉네임을 이 자리에 넣는다.
- 게스트일 때 익명 토글을 **숨긴다**(회원 설정 API에 붙은 기능이라 게스트에겐 대응 API가 없다).
- **시트 레이아웃과 문구 형식은 그대로 둔다.** 데이터 출처만 바꾼다.

### 7.10 로그인 시 계승

`shared/.../data/remote/MobileAuthRemoteRepository.kt:14` `loginWithGoogle(idToken)`이
`POST /api/v1/auth/mobile/{path}`를 호출한다. 저장된 게스트 토큰이 있으면 이 요청에
`X-Guest-Token` 헤더를 붙인다.

로그인 성공 직후 저장된 게스트 토큰과 닉네임을 **삭제한다**(`clearGuest()`). 계승은 로그인
요청 시점에 이미 끝났으므로 남겨둘 이유가 없고, 남겨두면 로그아웃 후 상태가 모호해진다.
로그아웃 후 다시 쓰기를 시도하면 7.3의 lazy 규칙에 따라 새 토큰을 받는다.

## 8. 완료 검증 기준

### 8.1 자동

새 테스트 프레임워크를 도입하지 않는다. 기존 `shared/src/commonTest`의 `kotlin.test` 위에서
작성한다.

- `WriteCredential.Member` → `Authorization: Bearer ...`, `WriteCredential.Guest` →
  `X-Guest-Token: ...` 헤더가 나가는지
- `requestAuthentication()`이 `requiresMember = true` 액션에만 `pendingAction`을 세우는지
- 게스트 신규 작성이 401을 받으면 재발급 후 1회 재시도하고, 재시도도 401이면 실패로 끝나는지
- 게스트 수정/삭제/좋아요취소가 401을 받으면 재시도하지 **않는지**
- `ReviewAuthorDto`가 `mine` / `actorType`을 파싱해 `writtenByMe`로 매핑하는지
- `updateReview` / `deleteReview` / `updateReply` / `deleteReply`가 올바른 메서드와 경로로
  나가는지
- 로그인 요청에 저장된 게스트 토큰이 `X-Guest-Token`으로 붙는지
- 로그인 성공 후 게스트 토큰·닉네임이 삭제되는지
- `./gradlew :shared:allTests :app:assembleDebug` 그린

### 8.2 수동 QA 시나리오

로그아웃 상태(앱 데이터 삭제 후 시작)에서 순서대로 확인한다.

1. 책 상세에서 감상을 작성한다 → 로그인 시트가 뜨지 않고 작성된다.
2. 감상 작성 시트에 서버가 준 게스트 닉네임이 표시된다. 익명 토글은 보이지 않는다.
3. 방금 쓴 감상 카드에 케밥 `⋯`이 보인다. **남의 감상 카드에는 보이지 않는다.**
4. `⋯` → [수정] → 기존 내용이 채워진 시트가 열리고, 고쳐서 저장하면 반영된다.
5. `⋯` → [삭제] → 확인 후 목록에서 사라진다.
6. 다른 감상에 답글을 단다 / 좋아요를 누른다 → 로그인 시트 없이 동작한다.
7. 내 답글에도 케밥이 뜨고 수정·삭제가 된다. 좋아요를 취소한다 → 취소된다.
8. **게스트 상태에서 서재 담기 / 별점 / 독서 상태 변경 / 쪽수 입력을 시도한다 →
   여전히 로그인 시트가 뜬다.**
9. **앱을 완전히 종료했다 재실행한다 → 내가 쓴 감상에 케밥이 그대로 보인다.**
   (7.8의 미확인 영역을 여기서 판정한다. 안 보이면 백엔드 이슈로 올린다.)
10. 홈 화면에 `지금 2 / 3` 게스트 배너가 더 이상 보이지 않는다.
11. 감상을 하나 더 남긴 뒤 구글 로그인한다 → **로그인 후에도 그 감상이 내 것으로 남아 있고,
    닉네임이 유지되며, 케밥으로 수정·삭제가 가능하다.**
12. 로그아웃 후 다시 감상을 쓴다 → 새 게스트 신분으로 작성된다.

## 9. 범위 밖 (이번에 하지 않는다)

- 게스트 플로의 UI 자동화 테스트(Compose UI test / Espresso)
- 홈 게스트 배너를 대체할 새 가입 유도 문구 (UI 결정 필요)
- `BookDetailAuthenticatedAction` 이름 변경 (6.2 참고)
- 백엔드 변경 일체
- 게스트 토큰 만료 시각을 이용한 선제적 재발급

## 10. 알려진 미결

- **`GET /api/v1/books/{bookId}/reviews`가 `X-Guest-Token`으로 `mine`을 계산해 주는지 계약서에
  명시가 없다.** (openapi3.yaml:1106에 `security` 블록 없음.) 7.8에서 헤더를 붙여 보내고
  QA 9번에서 실제 동작을 판정한다. 계산해 주지 않으면 백엔드 요청이 필요하다.
- 감상·답글 수정/삭제 진입점 UI는 `designs.pen`의 재사용 컴포넌트 목록에 없다. 이 계획의
  케밥 + 하단 시트 안은 기존 컴포넌트 조합으로 구성한 것이며, 디자인 확정본이 나오면
  그쪽을 따른다.
- 앱 재설치·기기 변경·게스트 토큰 유실 시 기존 게스트 콘텐츠를 수정·삭제할 수 없다. 상위
  결정 문서에서 감수하기로 한 사항이다.
- 계승 후 서버에서 기존 게스트 토큰이 계속 유효한지는 계약서에 명시가 없다. 로그인 직후
  삭제하는 결정으로 이 미확인 영역에 의존하지 않게 했다.
