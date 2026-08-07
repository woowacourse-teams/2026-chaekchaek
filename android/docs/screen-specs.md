# 첵췍 화면 명세

Figma [node 36:3](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr/%EC%B1%85%EC%B7%8D?node-id=36-3)의
12개 화면별 상태·액션·이동을 정리한다. 도메인 규칙은 [도메인 모델](domain-model.md), 상태 클래스
작성 규칙은 [아키텍처](app-architecture.md#6-화면-레이어)에 있다.

각 화면의 「신규 UI」 항목은 **Figma에 시안이 없어 새로 만들어야 하는 부분**이다. 구현 전에 시안
확정이 필요하다.

## 화면 목록

| # | 화면 | Figma 노드 | 탭바 |
| --- | --- | --- | --- |
| 1 | 탭 컨테이너 | 36:246 | - |
| 2 | 홈 피드 | 36:1206 | 있음 |
| 3 | 검색 (발견) | 36:427 | **추가 필요** |
| 4 | 내 서재 | 36:574 | 있음 |
| 5 | 서재 편집 | 36:1060 | 없음 |
| 6 | 닉네임 설정 | 36:900 | 다이얼로그 |
| 7 | 책 상세 | 36:4, 36:1391 | 없음 |
| 8 | 감상 목록 | 36:1391 내부 | 상세의 일부 |
| 9 | 스포일러 가드 | 36:712 | 다이얼로그 |
| 10 | 별점 매기기 | 36:1391 내부 | 다이얼로그 |
| 11 | 감상 작성 | 36:1337 | 바텀시트 |
| 12 | 답글 입력 | 36:264 | 감상 카드 내부 |

---

## 1. 탭 컨테이너 (`feature/root`)

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

**신규 UI**: 현재 탭 아이콘이 텍스트 기호(`⌕`, `▤`)다. Figma의 아이콘으로 교체해야 한다.

---

## 2. 홈 피드 (`feature/home`)

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

---

## 3. 검색 / 발견 (`feature/search`)

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
| 검색어 입력 후 실행 | `BookRepository.search(query)` |
| 결과 항목 탭 | 책 상세로 이동 |
| 「읽는 중 시작」 탭 | 서재에 `READING`으로 추가, 버튼이 상태 표시로 바뀜 |
| 정렬 변경 | 결과만 다시 로드 |

**신규 UI**

- **하단 탭바 추가** (Figma에 없음). 기존 우하단 원형 버튼과 겹치지 않게 위치 조정
- 검색 전 빈 화면 (`Idle`)
- 결과 0건 화면 (`Empty`)
- 로딩·오류 표시

**미결정**: 검색을 서버 경유로 바꿀지, 현재처럼 알라딘 API를 앱이 직접 호출할지.
[API 계약 5절](../../docs/api-contract.md#5-책)에 남겼다.

---

## 4. 내 서재 (`feature/shelf`)

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
| 프로필 아이콘 탭 | 미정 |

**신규 UI**: 빈 서재 화면, 로딩·오류 표시.

---

## 5. 서재 편집 (`feature/shelf`)

```
취소            2권 선택            완료
☑ 익명으로 감상 공개
  해제하면 닉네임을 설정해야 합니다
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
    val anonymousToggle: AnonymousToggleUiModel,
    val canApply: Boolean,              // 선택이 1개 이상
)

data class AnonymousToggleUiModel(
    val checked: Boolean,
    val description: String,
    // checked  : "해제하면 닉네임을 설정해야 합니다"
    // unchecked: "닉네임이 감상에 표시됩니다"
)
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 항목 체크 | `selectedIds` 갱신, 제목이 「N권 선택」으로 바뀜 |
| 「상태 변경」 탭 | **신규 UI**: 상태 3택 선택 수단이 시안에 없음 |
| 「서재에서 삭제」 탭 | `Shelf.remove(selectedIds)` |
| 개별 휴지통 탭 | 그 한 권 삭제 |
| 익명 토글 해제 | 닉네임 있으면 즉시 반영, 없으면 닉네임 다이얼로그 |
| 「취소」 | 변경 버리고 목록으로 |
| 「완료」 | 변경 반영하고 목록으로 |

**신규 UI**

- 「상태 변경」을 누른 뒤 무엇을 고르는지 (바텀시트? 다이얼로그?)
- 여러 권을 「읽고 싶어요」로 되돌릴 때 **진행 쪽수가 사라진다는 경고** 여부
- 삭제 확인 다이얼로그 여부

**미결정**: 「취소」가 이미 적용된 상태 변경까지 되돌리는지, 아니면 선택만 버리는지.

---

## 6. 닉네임 설정 (`feature/shelf`)

```
┌─────────────────────────────┐
│ 닉네임 설정                  │
│ 지금부터 감상과 답글에 이     │
│ 닉네임이 표시됩니다.          │
│ [ 닉네임을 입력하세요  0/10 ]│
│ 2~10자                       │
│      [취소]      [확인]      │
└─────────────────────────────┘
```

### 상태

```kotlin
data class NicknameDialogUiModel(
    val input: String,
    val counterLabel: String,       // "0/10"
    val helperLabel: String,        // "2~10자"
    val confirmEnabled: Boolean,    // 2~10자일 때만
    val errorLabel: String?,
)
```

입력 중에는 `String`으로 들고 있다가 「확인」에서 `Nickname`으로 만든다. 길이 검사는
`Nickname.MIN_LENGTH`/`MAX_LENGTH` 상수를 쓴다.

**미결정**: 닉네임 중복 검사 여부. 서버 결정 사항.

---

## 7. 책 상세 (`feature/bookdetail`)

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
| 북마크 아이콘 | 미정 |

**주의**: 상태를 「다 읽음」으로 바꾸면 쪽수 입력값도 총 쪽수로 바뀌어야 한다. 두 UI가 같은
데이터를 보고 있다.

**신규 UI**: 로딩·오류 표시, 북마크 동작, 표지 위의 「READ TODAY」 배지 조건.

---

## 8. 감상 목록 (`feature/bookdetail`)

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

[아키텍처 6.2](app-architecture.md#62-uimodel)의 `NoteUiModel`을 쓴다. 가려진 감상은
`NoteUiModel.Hidden`이고 **본문 문자열을 담지 않는다.**

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
| 가려진 카드 탭 | 스포일러 다이얼로그 |
| 좋아요 탭 | `NoteRepository.like()`. 낙관적 갱신 후 실패 시 되돌림 |
| 「답글」 탭 | 답글 입력창 열기 |
| ⋯ 탭 | 미정 (신고? 삭제?) |

**신규 UI**

- **가려진 감상 카드의 모양** (Figma에 잠금 카드 시안이 없음). 작성자·날짜는 보이고 본문 자리에
  「N쪽 이후 내용을 포함해요」와 자물쇠를 두는 안을 제안한다
- 감상 0건 화면
- 페이징 (30개를 한 번에 받는지 나눠 받는지)

---

## 9. 스포일러 가드 (`feature/bookdetail`)

```
┌─────────────────────────────────────┐
│ 어디까지 읽으셨나요?              ✕ │
│ 이 감상은 160쪽 이후 내용을 포함해요.│
│ 내가 읽은 쪽수를 입력하면 읽은      │
│ 범위까지 안전하게 볼 수 있어요.     │
│ 내가 읽은 쪽수  [ 160 ] 쪽 / 412쪽  │
│ [      입력한 쪽수까지 보기       ] │
│ [     스포일러 감수하고 보기      ] │
└─────────────────────────────────────┘
```

### 상태

```kotlin
data class SpoilerDialogUiModel(
    val requiredPageLabel: String,      // "160쪽 이후 내용을 포함해요"
    val pageInput: String,
    val totalPageLabel: String,         // "/ 412쪽"
    val confirmEnabled: Boolean,
)
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 「입력한 쪽수까지 보기」 | `ShelfBook.recordPage(입력값)` → **독서 기록이 갱신된다**. 서재에 없으면 `READING`으로 추가 |
| 「스포일러 감수하고 보기」 | `SpoilerBoundary.reveal(bookId)` → 그 책의 감상 전부 열림. 메모리에만 보관 |
| ✕ | 닫기, 아무 것도 안 함 |

입력값이 총 쪽수와 같으면 상태가 `FINISHED`가 된다는 점에 주의한다.

**감수한 손실**: 감상을 보려고 대충 넣은 숫자가 독서 기록을 덮어쓴다.

---

## 10. 별점 매기기 (`feature/bookdetail`)

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

**문구 수정 필요**: Figma의 「마션에 남겼던 별점을 확인하고」는 데이터(다른 책 3권)와 어긋난다.
「최근 남긴 별점을 확인하고」로 고친다.

### 액션

| 액션 | 처리 |
| --- | --- |
| 별 탭 | 0.5 단위 선택. 라벨이 함께 바뀜 |
| 「별점 저장」 | `RatingRepository.rate(bookId, rating)` |
| 「취소」 | 닫기 |

**미결정**: 0.5 단위를 어떻게 입력하는지 (별 반쪽 탭? 드래그?). 라벨 문구 매핑
(`4.0 → 좋았어요`)도 전체 표가 없다.

---

## 11. 감상 작성 (`feature/bookdetail`)

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
    val readingPageInput: String,       // 어디까지 읽었나 (스포일러 기준)
    val quotePageInput: String,         // 인용 출처 쪽 (신규)
    val chapter: String,
    val identityLabel: String,          // "익명 · '골똘한 참새'로 표시돼요"
    val submitEnabled: Boolean,         // 느낀점이 비어있지 않을 때
)
```

### 액션

| 액션 | 처리 |
| --- | --- |
| 「감상 남기기」 | `NoteRepository.write()`. 성공 시 닫고 목록 갱신 |
| ✕ | 작성 중이면 확인 후 닫기 |

**신규 UI (중요)**

Figma 폼에는 쪽수 입력칸이 **하나뿐**이다. 「읽은 지점」과 「인용 출처 쪽」을 분리하기로 했으므로
**입력칸을 하나 더 놓아야 한다.** 두 숫자가 비슷해 혼동되기 쉬우니 배치와 라벨을 신중히 정해야
한다. 구현 전에 시안을 확정한다.

제안하는 방향은 두 가지다.

```
안 A) 세로 배치
  어디까지 읽었나요   [ 200 ] 쪽 / 412쪽
  인용한 문장의 쪽수  [  50 ] 쪽        (인용문을 적었을 때만 표시)

안 B) 인용 블록에 붙이기
  인상 깊은 문구
  [ 기억하고 싶은 문장을...        ]
  └ 이 문장은 [ 50 ] 쪽에 있어요
  ─────────────────────────────
  여기까지 읽었어요 [ 200 ] 쪽 / 412쪽
```

안 B는 인용 쪽수를 인용문 바로 아래 두어 두 숫자의 역할이 구분된다. 다만 시안 구조에서 더 멀다.

**미결정**: 느낀점 최대 길이, 인용문 최대 길이, 임시 저장 여부.

---

## 12. 답글 입력 (`feature/bookdetail`)

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
| 「답글」 탭 | `NoteRepository.reply()`. 성공 시 입력창 닫고 목록에 추가 |
| 「취소」 | 입력 버리고 닫기 |

한 번에 하나의 감상에만 답글 입력창이 열린다.

---

## 신규 UI 정리

구현 전에 시안이 필요한 항목을 모았다.

| 우선순위 | 항목 | 관련 화면 |
| --- | --- | --- |
| 높음 | 가려진 감상 카드의 모양 | 감상 목록 |
| 높음 | 감상 작성 폼의 쪽수 입력칸 2개 배치 | 감상 작성 |
| 높음 | 검색 화면 하단 탭바 | 검색 |
| 높음 | 로딩·오류 표시 (전 화면 공통) | 전체 |
| 중간 | 「상태 변경」의 상태 선택 수단 | 서재 편집 |
| 중간 | 빈 상태 화면 (서재 0권, 검색 0건, 감상 0건) | 서재·검색·감상 |
| 중간 | 게스트 쿼터 소진 상태 | 홈 |
| 낮음 | 「모두 보기」·「목록」이 여는 화면 | 홈 |
| 낮음 | 알림 화면 | 홈 |
| 낮음 | 진행 기록 소실 경고 | 서재 편집 |
