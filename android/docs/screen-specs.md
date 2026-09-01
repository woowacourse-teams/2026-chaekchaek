# 첵췍 화면 명세

최종 갱신: 2026-09-01

현재 디자인 단일 원본은 `/Users/ujeonghyeon/Downloads/designs.pen`이다. 아래 Figma 노드와 초기
UiState 설계는 제품 의도와 이력 확인용이며, 실제 Android 동작은 Pencil과 현재 코드가 우선한다.

## 2026-08-25 Android 구현 기준

| 화면 또는 규칙 | Pencil 노드 | 현재 동작 |
| --- | --- | --- |
| 디자인 시스템 | `SxMn5` | 기존 색상, 서체, 간격 토큰만 사용 |
| 검색 정렬 | `jI61d` | `LATEST`와 `COMMENT`를 서버에 전달하고 선택 즉시 재조회 |
| 책 상세 | `QgUZE` | 서재 추가와 해제를 현재 등록 상태에 따라 전환 |
| 감상 잠금 | `DEquR`, `d6grPa` | 스포일러 체크된 감상의 본문, 발췌, 답글을 원문 길이만큼 `짹`으로 표시 |
| 감상 작성 | `ZozGQ`, `rU9vK` | 1000자 수, 작성 취소 확인, 실제 익명 설정과 공개 닉네임 표시 |
| 답글 입력 | `NS3v7` | 1자부터 200자까지 제출, 글자 수 표시 |
| 로그인 시트 | `mGHMD` | Google 로그인과 개인정보처리방침 링크 표시, 이용약관 링크 없음 |
| 내 서재 프로필 | `F0qdM9` | 실제 사용자 프로필 이미지를 표시하고 마이페이지로 이동 |
| 마이페이지 | `f2vla`, `th4SN` | 익명 공개 설정과 회원 탈퇴 확인 흐름 제공 |

홈, 검색, 상세 API 로딩은 요청이 500ms 안에 끝나면 표시하지 않는다. 500ms가 지나도 진행 중일
때만 표시하고 응답 즉시 닫는다.

Figma [node 36:3](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr/%EC%B1%85%EC%B7%8D?node-id=36-3)의
12개 화면별 상태·액션·이동을 정리한다. 도메인 규칙은 [도메인 모델](domain-model.md), 상태 클래스
작성 규칙은 [아키텍처](app-architecture.md#7-presentation-레이어)에 있다.

아래 UiState·UiModel은 초기 공유 계약이다. 현재 홈 상태는 `shared/commonMain/presentation`, 검색과
상세 화면 상태는 Android `app`에 있다. 화면 그림은 Android 기준이다.

각 화면의 「신규 UI」 항목은 **Figma에 시안이 없어 새로 만들어야 하는 부분**이다. 구현 전에 시안
확정이 필요하다.

라벨 문자열(`"80쪽 / 308쪽"` 등)은 UiModel 매핑에서 직접 조립하지 않고
[아키텍처 7.3의 포맷터](app-architecture.md#73-표시-문자열)를 거친다.

## 화면 목록

| # | 화면 | Figma 노드 | 탭바 |
| --- | --- | --- | --- |
| 1 | 탭 컨테이너 | 36:246 | - |
| 2 | 홈 피드 | 36:1206 | 있음 |
| 3 | 검색 (발견) | 36:427 | 있음 |
| 4 | 내 서재 | 36:574 | 있음 |
| 5 | 서재 편집 | 36:1060 | 없음 |
| 6 | 닉네임 설정 | 36:900 | 다이얼로그 |
| 7 | 책 상세 | 36:4, 36:1391 | 없음 |
| 8 | 감상 목록 | 36:1391 내부 | 상세의 일부 |
| 9 | 스포일러 가드 | `fKIr2` | 책 상세 내부 상태 |
| 10 | 별점 매기기 | 36:1391 내부 | 다이얼로그 |
| 11 | 감상 작성 | 36:1337 | 바텀시트 |
| 12 | 답글 입력 | 36:264 | 감상 카드 내부 |
| 13 | 마이페이지 | `f2vla`, `th4SN` | 없음 |

---

## 1. 탭 컨테이너 (`presentation/home` + `androidApp/ui/root`)

하단 탭 3개를 담고 선택된 탭의 화면을 보여준다. 백스택에는 `RootKey` 하나로 존재한다.

```kotlin
enum class RootTab(val label: String) {
    HOME("홈"),
    DISCOVER("발견"),
    SHELF("내 서재"),
}
```

- 선택된 탭은 `rememberSaveable`로 보관한다
- 상세 화면이 `push`되면 탭바는 보이지 않는다 (`RootKey`가 최상단일 때만 그린다)
- 탭 전환은 백스택에 쌓지 않는다. 뒤로가기는 앱 종료로 간다

하단 탭은 홈, 발견, 내 서재 명칭과 리소스 아이콘을 사용한다.

---

## 2. 홈 피드 (`presentation/home`)

```
오늘, 어땠어요?                    🔔
┌───────────────────────────────┐
│ (표지 콜라주)                  │
│ 지금 인기 책들 +12             │
│ 감상 128 · 댓글 46             │
│ 보이지 않는 도시            ↗ │
└───────────────────────────────┘
방금 남겨진 문장          모두 보기 ↗
┌───────────────────────────────┐
│ [표지] 보이지 않는 도시        │
│        김여름의 서재 · 4분 전  │
│        "도시는 기억으로..."    │
│        감상 전문 읽기 · 댓글 12│
└───────────────────────────────┘
밑줄이 겹친 책               목록 ↗
┌───────────────────────────────┐
│ 역병                    감상 96│
│ 윤서의 서재 · 오늘             │
│ 무너지는 세계에서...           │
│ 전문 읽기 · 댓글 28 ↗   [표지] │
└───────────────────────────────┘
┌───────────────────────────────┐
│ GUEST READING LIMIT            │
│ 둘러볼 수 있는 감상은 3개예요  │
│ ... 지금 2 / 3                 │
│ [로그인하고 계속 읽기      → ] │
└───────────────────────────────┘
```

### 상태

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Failure(val error: AppError) : HomeUiState
    data class Content(
        val sections: List<FeedSectionUiModel>,
        val guestBanner: GuestBannerUiModel?,   // 회원이면 null
    ) : HomeUiState
}

sealed interface FeedSectionUiModel {
    data class TrendingBooks(
        val books: List<TrendingBookUiModel>,
        val moreLabel: String,          // "지금 인기 책들 +12"
    ) : FeedSectionUiModel

    data class RecentQuotes(
        val title: String,              // "방금 남겨진 문장"
        val cards: List<QuoteCardUiModel>,
    ) : FeedSectionUiModel

    data class OverlappedBooks(
        val title: String,              // "밑줄이 겹친 책"
        val cards: List<OverlappedCardUiModel>,
    ) : FeedSectionUiModel
}

data class GuestBannerUiModel(
    val progressLabel: String,          // "지금 2 / 3"
    val exhausted: Boolean,
)
```

서버가 모르는 섹션 타입을 내려주면 `FeedSectionUiModel`로 매핑하지 않고 건너뛴다.

### 액션

| 액션 | 처리 |
| --- | --- |
| 인기 책 콜라주 탭 | 책 상세로 이동 |
| 「감상 전문 읽기」 탭 | 게스트면 쿼터 1 소비 → 책 상세(해당 감상 위치)로 이동 |
| 「모두 보기」/「목록」 탭 | **신규 화면 필요** |
| 「로그인하고 계속 읽기」 탭 | 지금은 빈 콜백 |
| 알림 아이콘 탭 | 미정 |

**신규 UI**

- 「모두 보기」·「목록」이 여는 화면 (시안 없음)
- 알림 화면 (시안 없음)
- 게스트 쿼터 소진 시의 표시 (Figma는 2/3 상태만 있음)
- 로딩·오류 표시

### 더미 데이터 (서버 준비 전)

이슈 #7 범위에서 홈이 보여줄 내용이다. `images/cover-01.png` ~ `cover-12.png`를 쓴다.

| 섹션 | 더미 구성 |
| --- | --- |
| 인기 책 콜라주 | 표지 12장을 흩뿌림. 대표 1권의 감상·댓글 수 표시 |
| 방금 남겨진 문장 | 감상 카드 3개 (12분 전, 38분 전, 1시간 전) |
| 밑줄이 겹친 책 | 책 1권 + 인용 발췌 |
| 게스트 배너 | `2 / 3` 상태 |

**상대 시각은 저장하지 않는다.** 「4분 전」 같은 문자열을 더미에 박아두면 시간이 지나도 그대로다.
`Instant`를 담고 표시 시점에 계산한다. Fake DataSource는 `Clock`을 주입받아 테스트에서 시각을
고정할 수 있게 한다.

「24시간 동안 감상 18개」 같은 문장도 통째로 저장하지 않는다. 숫자만 담고 라벨 포맷터가 조립한다.

**연결되지 않은 버튼은 만들지 않는다.** 목적지가 아직 없는 액션(「모두 보기」, 알림)은 눌러도
아무 일이 없는 가짜 버튼으로 두지 않고, 그 화면이 구현될 때 콜백과 함께 추가한다. 이슈 #7에서
실제로 동작하는 것은 하단 탭 전환과 서재 바로가기뿐이다.

---

## 3. 검색 / 발견 (`presentation/search`)

```
🔍 [ 마션                    ]
ARCHIVE SEARCH · 검색 결과 10건    최신순 ▾
┌───────────────────────────────┐
│ [표지] 마션                    │
│        앤디 위어 · 박아람 옮김  │
│        알에이치코리아 · SF ·   │
│        2026 · 308쪽            │
│        💬 댓글 46  [+ 읽는 중 시작]│
└───────────────────────────────┘
```

### 상태

```kotlin
sealed interface SearchUiState {
    data object Idle : SearchUiState                    // 검색 전
    data object Loading : SearchUiState
    data class Failure(val error: AppError) : SearchUiState
    data class Content(
        val query: String,
        val resultLabel: String,                        // "검색 결과 10건"
        val sort: SearchSortUiModel,
        val books: List<SearchResultUiModel>,
    ) : SearchUiState
    data object Empty : SearchUiState                   // 결과 0건
}

data class SearchResultUiModel(
    val bookId: BookId,
    val title: String,
    val authorLabel: String,        // "앤디 위어 · 박아람 옮김"
    val metaLabel: String,          // "알에이치코리아 · SF · 2026 · 308쪽"
    val coverUrl: String,
    val noteCountLabel: String,     // "댓글 46"
    val shelfAction: ShelfActionUiModel,
)

sealed interface ShelfActionUiModel {
    data object StartReading : ShelfActionUiModel        // "+ 읽는 중 시작"
    data class InShelf(val statusLabel: String) : ShelfActionUiModel
}
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 검색어 입력 후 실행 | `BookSearchRepository.search(query, sort)` |
| 결과 항목 탭 | 책 상세로 이동 |
| 「읽는 중 시작」 탭 | 서재에 `READING`으로 추가, 버튼이 상태 표시로 바뀜 |
| 정렬 변경 | `LATEST` 또는 `COMMENT`로 현재 검색어를 다시 로드 |

검색 전 빈 화면, 결과 0건, 오류, 500ms 지연 로딩과 하단 탭을 구현했다.

검색은 `BookSearchRepository.search(query, sort)`를 통해 Chaekchaek API `GET /api/v1/books`를 호출한다.
`sort` 값은 `LATEST`, `COMMENT`만 사용한다.

---

## 4. 내 서재 (`presentation/shelf`)

```
내 서재                              👤
[전체] [읽고 싶어요] [읽는 중] [다 읽음]
전체 12권            최근 기록순 ▾   편집
┌───────────────────────────────┐
│ [표지] [읽는 중]               │
│        마션                  > │
│        앤디 위어 · SF          │
│        80쪽 / 308쪽            │
└───────────────────────────────┘
```

### 상태

```kotlin
sealed interface ShelfUiState {
    data object Loading : ShelfUiState
    data class Failure(val error: AppError) : ShelfUiState
    data class Content(
        val filter: ReadingStatus?,             // null = 전체
        val countLabel: String,                 // "전체 12권"
        val sortLabel: String,                  // "최근 기록순"
        val books: List<ShelfBookUiModel>,
    ) : ShelfUiState
    data object Empty : ShelfUiState
}

data class ShelfBookUiModel(
    val bookId: BookId,
    val statusLabel: String,        // "읽는 중"
    val title: String,
    val metaLabel: String,          // "앤디 위어 · SF"
    val progressLabel: String,      // "80쪽 / 308쪽"
    val progressRatio: Float,
    val coverUrl: String,
)
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 상태 필터 탭 | `Shelf.filterBy(status)` |
| 정렬 변경 | `Shelf.sortedByRecent()` |
| 항목 탭 | 책 상세로 이동 |
| 「편집」 탭 | 편집 모드로 전환 |
| 프로필 아이콘 탭 | 로그인 상태면 마이페이지로 이동, 게스트면 로그인 시트 표시 |

**신규 UI**: 빈 서재 화면, 로딩·오류 표시.

---

## 5. 서재 편집 (`presentation/shelf`)

```
취소            2권 선택            완료
[전체] [읽고 싶어요] [읽는 중] [다 읽음]
전체 12권                  최근 기록순 ▾
☑ [표지] [읽는 중] 마션            🗑
☐ [표지] [다 읽음] 보이지 않는 도시 🗑
[ 상태 변경 ]            [ 서재에서 삭제 ]
```

### 상태

```kotlin
data class ShelfEditUiModel(
    val selectedIds: Set<BookId>,
    val titleLabel: String,             // "2권 선택"
    val canApply: Boolean,              // 선택이 1개 이상
)
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 항목 체크 | `selectedIds` 갱신, 제목이 「N권 선택」으로 바뀜 |
| 「상태 변경」 탭 | **신규 UI**: 상태 3택 선택 수단이 시안에 없음 |
| 「서재에서 삭제」 탭 | `Shelf.remove(selectedIds)` |
| 개별 휴지통 탭 | 그 한 권 삭제 |
| 「취소」 | 변경 버리고 목록으로 |
| 「완료」 | 변경 반영하고 목록으로 |

**신규 UI**

- 「상태 변경」을 누른 뒤 무엇을 고르는지 (바텀시트? 다이얼로그?)
- 여러 권을 「읽고 싶어요」로 되돌릴 때 **진행 쪽수가 사라진다는 경고** 여부
- 삭제 확인 다이얼로그 여부

**미결정**: 「취소」가 이미 적용된 상태 변경까지 되돌리는지, 아니면 선택만 버리는지.

---

## 6. 닉네임 설정 (`presentation/shelf`)

```
┌─────────────────────────────┐
│ 닉네임 설정                  │
│ 지금부터 감상과 답글에 이     │
│ 닉네임이 표시됩니다.          │
│ [ 닉네임을 입력하세요  0/10 ]│
│ 최대 10자                    │
│      [취소]      [확인]      │
└─────────────────────────────┘
```

Android 구현은 공백이 아닌 최대 10자를 받는다.

### 상태

```kotlin
data class NicknameDialogUiModel(
    val input: String,
    val counterLabel: String,       // "0/10"
    val helperLabel: String,        // "최대 10자"
    val confirmEnabled: Boolean,    // Nickname.isValid(input)
    val errorLabel: String?,
)
```

입력 중에는 `String`으로 들고 최대 10자로 자른다. 공백이 아니면 확인할 수 있다.

**미결정**: 닉네임 중복 검사 여부. 서버 결정 사항.

## 6.1. 마이페이지 (`ui/archive`)

### 상태

```kotlin
data class MemberSettingsUiState(
    val signedIn: Boolean,
    val anonymousReviews: Boolean,
    val nickname: String,
    val anonymousNickname: String,
    val profileImageUrl: String?,
    val withdrawing: Boolean,
    val withdrawalErrorMessage: String?,
)
```

- 프로필 이미지는 `profileImageUrl`을 표시하고 값이 없으면 기본 참새 이미지를 사용한다.
- 공개 닉네임은 익명 공개 중이면 `anonymousNickname`, 아니면 `nickname`을 표시한다.
- 회원 탈퇴 실패 시 로그인 상태를 유지하고 오류와 「다시 시도」 액션을 표시한다.

### 액션

| 액션 | 처리 |
| --- | --- |
| 뒤로가기 | 내 서재로 복귀 |
| 익명 공개 설정 탭 | `PATCH /api/v1/members/me/anonymity`, 닉네임이 없으면 닉네임 다이얼로그 표시 |
| 회원 탈퇴 탭 | 되돌릴 수 없음을 알리는 확인 다이얼로그 표시 |
| 「탈퇴하기」 탭 | `DELETE /api/v1/members/me`, 성공 시 로컬 인증을 지우고 비로그인 상태로 복귀 |
| 「다시 시도」 탭 | 회원 탈퇴 API 재요청 |

---

## 7. 책 상세 (`presentation/bookdetail`)

```
←                                    🔖
        [표지]        [READ TODAY]
        마션
        앤디 위어 · 알에이치코리아
   SF  2026 초판  308쪽  ★★★★☆ 4.2
        평점 100명 · 감상 30명
내 독서 기록                  ☆ 별점 주기
[읽고 싶어요] [읽는 중] [다 읽음]
지금 읽는 쪽 [ 80 ] / 308쪽   ✎ 쪽수 입력
─────────────────────────────────
감상 30
[최신순 ▾] [인기순]   [전체 피드] [내 피드]
(감상 카드 목록)
─────────────────────────────────
✎ 이 순간의 감상 남기기
```

### 상태

```kotlin
sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data class Failure(val error: AppError) : BookDetailUiState
    data class Content(
        val book: BookUiModel,
        val myRecord: ReadingRecordUiModel,
        val notes: NoteListUiState,
        val dialog: BookDetailDialog?,
    ) : BookDetailUiState
}

data class BookUiModel(
    val title: String,
    val authorLabel: String,        // "앤디 위어 · 알에이치코리아"
    val metaLabels: List<String>,   // ["SF", "2026 초판", "308쪽"]
    val ratingLabel: String,        // "4.2"
    val ratingStars: Float,
    val ratingCountLabel: String,   // "평점 100명 · 감상 30명"
    val coverUrl: String,
)

data class ReadingRecordUiModel(
    val status: ReadingStatus?,     // 서재에 없으면 null
    val currentPageInput: String,   // "80"
    val totalPageLabel: String,     // "/ 308쪽"
    val myRatingLabel: String?,     // "별점 주기" 또는 "4.0"
)

sealed interface BookDetailDialog {
    data class Spoiler(val model: SpoilerDialogUiModel) : BookDetailDialog
    data class Rating(val model: RatingDialogUiModel) : BookDetailDialog
}
```

`notes`를 중첩한 이유는 정렬·범위를 바꿀 때 감상 목록만 다시 로드하고 표지·별점은 유지해야
하기 때문이다.

### 액션

| 액션 | 처리 |
| --- | --- |
| 상태 3택 탭 | `ShelfBook.changeStatus()`. 서재에 없으면 새로 만든다 |
| 쪽수 입력 후 확정 | `ShelfBook.recordPage()`. 서재에 없으면 `READING`으로 추가 |
| 「별점 주기」 탭 | 별점 다이얼로그 |
| 정렬·범위 변경 | `notes`만 다시 로드 |
| 「감상 남기기」 탭 | 감상 작성 바텀시트 |
| 뒤로 | `popBackStack` |
| 서재 아이콘 | 현재 등록 상태에 따라 서재 추가 또는 해제 |

**주의**: 상태를 「다 읽음」으로 바꾸면 쪽수 입력값도 총 쪽수로 바뀌어야 한다. 두 UI가 같은
데이터를 보고 있다.

**남은 UI**: 상세 오류 표시와 표지 위의 「READ TODAY」 배지 조건.

---

## 8. 감상 목록 (`presentation/bookdetail`)

```
감상 30
[최신순 ▾] [인기순]      [전체 피드] [내 피드]
┌─────────────────────────────────────┐
│ 🐦 참새 1204 (익명) [완독] 2026.08.05│
│                          p.80까지  ⋯│
│ 혼자 남겨진 사람이 절망 대신...      │
│ 인용 위치 · p.80                    │
│ "나는 이 행성에서 과학으로..."       │
│ ♡ 좋아요 12    💬 답글 2            │
│   🐦 참새 0330 (익명)               │
│      감자 파트에서 진짜 웃었어요.  ♡3│
└─────────────────────────────────────┘
```

### 상태

Android 구현은 서버 원문을 유지하고 표시 단계에서만 잠근다. 잠긴 감상, 발췌, 답글은 공백과
문장부호를 유지하고 나머지 문자를 원문 길이만큼 `짹`으로 치환한다.

```kotlin
sealed interface NoteListUiState {
    data object Loading : NoteListUiState
    data class Failure(val error: AppError) : NoteListUiState
    data class Loaded(
        val countLabel: String,     // "감상 30"
        val sort: NoteSortOrder,
        val scope: NoteScope,
        val notes: List<NoteUiModel>,
    ) : NoteListUiState
    data object Empty : NoteListUiState
}
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 가려진 카드 탭 | 해당 감상의 본문, 인용문, 답글만 즉시 공개 |
| 좋아요 탭 | `likedByMe`에 따라 감상 반응 등록 또는 취소 후 다시 로드 |
| 「답글」 탭 | 답글 입력창 열기 |
| 「답글 N개 모두 보기」 탭 | 마지막 페이지까지 조회해 중복 없이 펼침 |
| 답글 좋아요 탭 | `likedByMe`에 따라 답글 반응 등록 또는 취소 |
| ⋯ 탭 | 미정 (신고? 삭제?) |

감상 0건 화면과 감상 페이지 추가 로딩은 구현되어 있다.

---

## 9. 스포일러 가드 (`presentation/bookdetail`)

```
┌─────────────────────────────────────┐
│ 참새 0912 (익명)       p.160까지    │
│ 짹짹짹 짹짹 짹짹짹...             │
└─────────────────────────────────────┘
```

잠금 상태는 `designs.pen`의 `QgUZE`와 같이 별도 안내 행을 표시하지 않는다. 짹짹으로 가린
본문, 인용문, 답글을 포함한 감상 카드 전체가 공개 탭 대상이다.

### 상태

```kotlin
val locked = review.isSpoiler && review.reviewId !in revealedReviewIds
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 가려진 감상 탭 | 해당 `reviewId`를 공개 목록에 추가하고 본문, 인용문, 답글을 함께 표시 |

공개 상태는 현재 책 상세 화면의 메모리에만 유지한다. 다른 스포일러 감상은 계속 가린다. 감상
쪽수는 선택적으로 입력하고 표시하지만 가림 판정에는 사용하지 않는다.

---

## 10. 별점 매기기 (`presentation/bookdetail`)

```
┌─────────────────────────────────────┐
│ 이 책에 별점 매기기               ✕ │
│ 최근 남긴 별점을 확인하고 새 별점을 │
│ 선택하세요.                         │
│ 내 평점 기록                    3회 │
│  3.5          4.0          4.0      │
│  보이지 않는  역병         마션      │
│  도시                               │
│  2026.05.12  2026.06.21   2026.08.05│
│ 새 별점   ★ ★ ★ ★ ☆                │
│           4.0 · 좋았어요            │
│        [취소]    [별점 저장]        │
└─────────────────────────────────────┘
```

### 상태

```kotlin
data class RatingDialogUiModel(
    val historyLabel: String,               // "3회"
    val history: List<RatedBookUiModel>,    // 다른 책 포함, 내 최근 별점
    val selected: Rating?,
    val selectedLabel: String?,             // "4.0 · 좋았어요"
    val saveEnabled: Boolean,
)
```

다이얼로그는 최근 별점 3개를 표시하고 저장 직후 이력을 갱신한다.

### 액션

| 액션 | 처리 |
| --- | --- |
| 별 탭 | 0.5 단위 선택. 라벨이 함께 바뀜 |
| 「별점 저장」 | `RatingRepository.rate(bookId, rating)` |
| 「취소」 | 닫기 |

별점은 0.5 단위로 선택한다.

---

## 11. 감상 작성 (`presentation/bookdetail`)

```
┌─────────────────────────────────────┐
│ 감상 남기기                       ✕ │
│ 느낀점 [필수]                       │
│ [ 이 구간을 읽으며 든 생각을        │
│   남겨보세요                      ] │
│ 인상 깊은 문구                      │
│ [ 기억하고 싶은 문장을 옮겨 적어    │
│   보세요                          ] │
│ 쪽수 [ 80 ] 쪽   목차/챕터 [Chapter 1]│
│ 🐦 익명 · '골똘한 참새'로 표시돼요  │
│ [        감상 남기기              ] │
└─────────────────────────────────────┘
```

### 상태

```kotlin
data class NoteComposeUiModel(
    val impression: String,
    val quoteText: String,
    val readingPageInput: String,       // 감상과 함께 남길 선택적 쪽수
    val chapter: String,
    val identityLabel: String,          // 실제 익명 설정 또는 LibraryRepository.nickname
    val counterLabel: String,           // "0 / 1000"
    val submitEnabled: Boolean,         // 느낀점이 비어있지 않을 때
)
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 「감상 남기기」 | `NoteRepository.write()`. 성공 시 닫고 목록 갱신 |
| ✕ | 작성 중이면 확인 후 닫기 |

느낀점은 최대 1000자, 인용문은 최대 500자, 챕터는 최대 255자다. 작성값이 하나라도 있으면 닫기
전에 폐기 확인 대화상자를 표시한다. 임시 저장은 하지 않는다.

---

## 12. 답글 입력 (`presentation/bookdetail`)

```
│ ♡ 좋아요 12    💬 답글 2            │
│ ┌─────────────────────────────────┐ │
│ │ 🐦 [                          ] │ │
│ │              0 / 200            │ │
│ │              [취소]   [답글]    │ │
│ └─────────────────────────────────┘ │
│   🐦 참새 0330 (익명)               │
```

### 상태

```kotlin
data class ReplyComposeUiModel(
    val noteId: NoteId,
    val input: String,
    val counterLabel: String,       // "0 / 200"
    val submitEnabled: Boolean,     // 1~200자
)
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 「답글」 탭 | 1자부터 200자까지 서버에 등록하고 성공 시 목록을 다시 로드 |
| 「취소」 | 입력 버리고 닫기 |

한 번에 하나의 감상에만 답글 입력창이 열린다.

---

## 신규 UI 정리

구현 전에 시안이 필요한 항목을 모았다.

| 우선순위 | 항목 | 관련 화면 |
| --- | --- | --- |
| 완료 | 가려진 감상 표시와 해제 흐름 | 감상 목록 |
| 완료 | 감상 작성 글자 수와 폐기 확인 | 감상 작성 |
| 완료 | 검색 화면 하단 탭바 | 검색 |
| 완료 | 500ms 지연 로딩 | 홈·검색·상세 |
| 중간 | 「상태 변경」의 상태 선택 수단 | 서재 편집 |
| 중간 | 빈 상태 화면 (서재 0권, 검색 0건, 감상 0건) | 서재·검색·감상 |
| 중간 | 게스트 쿼터 소진 상태 | 홈 |
| 낮음 | 「모두 보기」·「목록」이 여는 화면 | 홈 |
| 낮음 | 알림 화면 | 홈 |
| 낮음 | 진행 기록 소실 경고 | 서재 편집 |
