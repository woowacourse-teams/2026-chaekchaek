# 첵췍 도메인 모델

Figma 시안 [node 36:3](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr/%EC%B1%85%EC%B7%8D?node-id=36-3)
의 12개 화면에서 읽어낸 도메인 개념을 코드 객체로 옮긴 설계다. Android와 iOS가 공유하는
`shared/commonMain`에 놓인다. 레이어 구조·DI·화면 상태 규칙은
[앱 아키텍처](app-architecture.md)에, 모듈 구성은 [KMP 셋업](kmp-setup.md)에, 서버와의 약속은
[API 계약](../../docs/api-contract.md)에 있다.

이 문서에 적힌 규칙은 모두 Figma에서 근거를 찾을 수 있거나, 근거가 없어 결정한 것이면 그렇다고
표시했다. 결론만 필요하면 [10. 규칙 목록](#10-규칙-목록-테스트-대상)과
[11. 미결정 사항](#11-미결정-사항)을 보면 된다.

## 1. 설계 원칙

1. **도메인은 순수 Kotlin이다.** `androidx.*`, Ktor, `kotlinx.serialization` 어노테이션을
   import 하지 않는다. `java.*`도 쓸 수 없다(Kotlin/Native에 없다). 시각은 kotlinx-datetime의
   `Instant`·`LocalDate`를 쓴다.
2. **규칙이 붙는 값은 전용 타입으로 감싼다.** 쪽수·별점·닉네임·식별자가 대상이다. 감싼 값을
   원시값으로 도로 꺼내는 메서드는 만들지 않는다.
3. **불가능한 상태는 표현할 수 없게 만든다.** 생성자 `require`와 `sealed interface`로 막는다.
   "이런 조합은 없을 것"이라는 주석 대신 컴파일러나 예외가 막게 한다.
4. **규칙은 데이터를 가진 객체가 판정한다.** ViewModel이 도메인 객체 내부를 순회해서 판단하지
   않는다.
5. **컬렉션은 감춘다.** `List`를 그대로 노출하지 않고 질문에 답하는 메서드를 준다.

원칙 2·5는 참고 프로젝트(`wtc/android-shopping-order`)에서 지켜지지 않은 부분이라 특히 적어둔다.
자세한 내용은 [부록 A](#부록-a-참고-프로젝트에서-바꾼-것)에 있다.

### 1.1 value class와 Swift 경계 (중요)

Kotlin/Native의 Objective-C export는 `value class`를 **underlying 타입이나 `id`(Swift의 `Any`)로
매핑**한다. 즉 `PageNumber`가 Swift에서 `Int32`나 `Any`로 보여 **타입 안전성이 사라진다.**

그래서 **도메인 타입은 Swift 경계를 넘지 않는다.** ViewModel과 UiModel까지 `commonMain`에서
공유하고, UI에는 완성된 `String`만 넘긴다. `value class`가 주는 안전성은 Kotlin 코드 안에서만
유효하다는 것을 전제로 설계했다.

- Kotlin 코드(도메인·데이터·presentation): `value class`가 실수를 컴파일 단계에서 막는다
- Swift 코드: 도메인 타입을 아예 보지 않으므로 실수할 여지가 없다

식별자(`BookId` 등)는 예외적으로 UiModel에 남는다. UI가 값을 읽지 않고 콜백으로 되돌려주기만
하는 불투명한 토큰이라 Swift에서 `Any`로 보여도 문제가 없다.

## 2. 용어 사전

Figma는 한글, 코드는 영문이라 대응을 먼저 고정한다. 화면 문구가 바뀌어도 코드 이름은 이 표를
따른다.

| Figma 문구 | 코드 이름 | 비고 |
| --- | --- | --- |
| 책 | `Book` | |
| 내 서재 | `Shelf` | 탭 이름이자 컬렉션 |
| 서재의 책 한 권 | `ShelfBook` | 책 + 내 독서 기록 |
| 읽고 싶어요 / 읽는 중 / 다 읽음 | `ReadingStatus.WANT_TO_READ / READING / FINISHED` | |
| 80쪽 / 308쪽 | `ReadingProgress` | |
| 감상 | `Note` | "감상 30", "감상 남기기" |
| 느낀점 | `Note.impression` | 필수 입력 |
| 인상 깊은 문구 | `Quote` | 선택 입력 |
| p.80까지 | `Note.readingPoint` | 스포일러 기준 |
| 인용 위치 · p.80 | `Quote.page` | |
| 답글 | `Reply` | |
| 좋아요 | `Reaction` | |
| 별점 | `Rating` | |
| 닉네임 | `Nickname` | 2~10자 |
| 익명 | `ReaderProfile.publishesAnonymously` | |
| 참새 1204 (익명) | `AuthorName.Anonymous` | |
| 방금 남겨진 문장 | `FeedSection.RecentQuotes` | |
| 밑줄이 겹친 책 | `FeedSection.OverlappedBooks` | |
| 지금 인기 책들 | `FeedSection.TrendingBooks` | |
| 둘러볼 수 있는 감상은 3개 | `GuestQuota` | |

## 3. 값 객체

### 3.1 쪽수

쪽수는 이 앱에서 가장 많이 쓰이는 값이다. 책 상세의 "지금 읽는 쪽", 서재의 진행도, 감상의
"p.80까지", 인용 위치, 그리고 **스포일러 판정 기준**이 전부 쪽수다. `Int`로 두면 좋아요 수나
감상 수 같은 다른 `Int`와 섞여도 컴파일이 통과한다.

```kotlin
@JvmInline
value class PageNumber(val value: Int) : Comparable<PageNumber> {
    init {
        require(value >= 0) { "쪽수는 0 이상이어야 합니다: $value" }
    }

    override fun compareTo(other: PageNumber): Int = value.compareTo(other.value)
}

@JvmInline
value class PageCount(val value: Int) {
    init {
        require(value > 0) { "총 쪽수는 1 이상이어야 합니다: $value" }
    }

    fun contains(page: PageNumber): Boolean = page.value <= value

    fun lastPage(): PageNumber = PageNumber(value)
}
```

`PageNumber`(어느 지점)와 `PageCount`(전체 분량)를 나눈 이유는 0의 의미가 다르기 때문이다.
0쪽은 유효한 지점이지만 0쪽짜리 책은 없다.

### 3.2 별점

Figma에 별점이 두 종류로 나온다. 평균 별점은 `★★★★☆ 4.2`처럼 임의 소수이고, 내가 매기는
별점은 `4.0 · 좋았어요`, 이력은 `3.5 / 4.0 / 4.0`으로 **0.5 단위**다. 둘을 같은 `Float`로 두면
섞이고, 부동소수점 동등 비교도 위험하다. 그래서 내 별점은 반개 단위 정수로 들고 있는다.

```kotlin
@JvmInline
value class Rating private constructor(val halfStars: Int) {
    init {
        require(halfStars in MIN_HALF_STARS..MAX_HALF_STARS) {
            "별점은 0.5부터 5.0까지 0.5 단위입니다: ${halfStars / 2f}"
        }
    }

    val score: Float get() = halfStars / 2f

    companion object {
        private const val MIN_HALF_STARS = 1
        private const val MAX_HALF_STARS = 10

        fun ofHalfStars(halfStars: Int): Rating = Rating(halfStars)

        fun ofScore(score: Float): Rating {
            val halfStars = (score * 2).toInt()
            require(halfStars / 2f == score) { "별점은 0.5 단위여야 합니다: $score" }
            return Rating(halfStars)
        }
    }
}
```

평균 별점은 별도 타입으로 둔다. 계산된 값이라 0.5 단위가 아니고, 매길 수 없는 값이다.

```kotlin
class RatingSummary(
    val average: Float,
    val raterCount: Int,
) {
    init {
        require(average in 0f..5f) { "평균 별점 범위를 벗어났습니다: $average" }
        require(raterCount >= 0) { "평점 인원은 0 이상이어야 합니다: $raterCount" }
    }

    fun hasRating(): Boolean = raterCount > 0
}
```

`4.0 · 좋았어요`의 "좋았어요" 같은 라벨은 표시용 문자열이므로 도메인이 아니라 UiModel에서
매핑한다.

### 3.3 닉네임

**2~15자, 한글·영문·숫자와 `-`, `_` 만 허용한다.** Figma 시안은 `0/10`, `2~10자`로 그려져 있으나
최대 길이를 15자로 정했다. 닉네임 설정 다이얼로그의 카운터와 안내 문구를 `0/15`, `2~15자`로
고쳐야 한다.

```kotlin
@JvmInline
value class Nickname(val value: String) {
    init {
        require(hasValidLength(value)) { ... }
        require(hasAllowedCharactersOnly(value)) { ... }
    }

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 15

        /** 한글은 완성형만 허용한다. 자음·모음 단독(ㄱ, ㅏ)은 닉네임으로 쓰지 않는다. */
        private val ALLOWED = Regex("[가-힣a-zA-Z0-9_-]+")

        fun isValid(value: String): Boolean = ...
    }
}
```

허용·차단 예시다.

| 입력 | 결과 | 이유 |
| --- | --- | --- |
| `골똘한참새`, `sparrow`, `참새1204`, `book-lover`, `book_lover` | 허용 | |
| `골똘한 참새` | 차단 | 공백 |
| `참새.`, `참새!`, `참새🐦` | 차단 | 허용하지 않는 기호 |
| `ㅋㅋㅋ`, `ㅏㅏ` | 차단 | 완성되지 않은 한글 |
| `참` | 차단 | 2자 미만 |

입력 중에는 아직 닉네임이 아니다. 화면은 `String`을 들고 있다가 확인을 누를 때 `Nickname`으로
만든다. 확인 버튼 활성화 판단에는 예외를 던지지 않는 `isValid`를 쓴다.

### 3.4 식별자

API 경로가 `/books/{bookId}/notes`와 `/notes/{noteId}/replies`처럼 중첩되어, 두 식별자를 같은
함수에서 다루는 곳이 많다. 둘 다 `String`이면 자리를 바꿔 넣어도 컴파일된다.

```kotlin
@JvmInline value class BookId(val value: String)
@JvmInline value class NoteId(val value: String)
@JvmInline value class ReplyId(val value: String)
@JvmInline value class ReaderId(val value: String)
```

이들은 규칙이 없으므로 `require`도 없다. 목적은 검증이 아니라 **자리 바꿈 방지**다.

## 4. 책

```kotlin
class Book(
    val id: BookId,
    val title: String,
    val authors: List<String>,
    val translators: List<String>,
    val publisher: String,
    val category: String,
    val publishedYear: Int,
    val totalPages: PageCount,
    val coverUrl: String,
) {
    init {
        require(title.isNotBlank()) { "책 제목은 공백일 수 없습니다." }
        require(authors.isNotEmpty()) { "저자가 최소 한 명 필요합니다." }
    }
}
```

Figma의 표기와 필드 대응은 이렇다.

```
마션                                    title
앤디 위어 · 박아람 옮김                  authors, translators
알에이치코리아 · SF · 2026 · 308쪽       publisher, category, publishedYear, totalPages
```

`authors`와 `translators`를 나눈 이유는 검색 결과(36:427)가 `앤디 위어 · 박아람 옮김`처럼
"옮김"을 붙여 구분하기 때문이다. 서재 목록(36:574)에서는 `앤디 위어 · SF`처럼 저자만 쓴다.
문자열 조합은 UiModel이 담당한다.

## 5. 내 서재

### 5.1 독서 상태

```kotlin
enum class ReadingStatus {
    WANT_TO_READ,
    READING,
    FINISHED,
}
```

### 5.2 독서 진행

```kotlin
class ReadingProgress(
    val currentPage: PageNumber,
    val totalPages: PageCount,
) {
    init {
        require(totalPages.contains(currentPage)) {
            "읽은 쪽수가 총 쪽수를 넘을 수 없습니다: ${currentPage.value} / ${totalPages.value}"
        }
    }

    fun isFinished(): Boolean = currentPage == totalPages.lastPage()

    fun isNotStarted(): Boolean = currentPage == PageNumber(0)

    fun ratio(): Float = currentPage.value.toFloat() / totalPages.value

    fun movedTo(page: PageNumber): ReadingProgress = ReadingProgress(page, totalPages)

    fun completed(): ReadingProgress = ReadingProgress(totalPages.lastPage(), totalPages)

    fun reset(): ReadingProgress = ReadingProgress(PageNumber(0), totalPages)
}
```

### 5.3 서재의 책 한 권

**상태가 쪽수를 강제한다.** Figma 서재 목록이 `읽고 싶어요 0쪽/264쪽`, `다 읽음 196쪽/196쪽`으로
정확히 맞아떨어지는 것을 규칙으로 채택했다. 상태를 바꾸면 쪽수가 따라 움직이고, 쪽수가 끝에
닿으면 상태가 따라 움직인다.

```kotlin
class ShelfBook(
    val book: Book,
    val status: ReadingStatus,
    val progress: ReadingProgress,
    val myRating: Rating?,
    val lastRecordedAt: Instant,
) {
    init {
        when (status) {
            ReadingStatus.FINISHED -> require(progress.isFinished()) {
                "다 읽음 상태는 진행 쪽수가 총 쪽수와 같아야 합니다."
            }
            ReadingStatus.WANT_TO_READ -> require(progress.isNotStarted()) {
                "읽고 싶어요 상태는 진행 쪽수가 0이어야 합니다."
            }
            ReadingStatus.READING -> Unit
        }
    }

    fun changeStatus(next: ReadingStatus, at: Instant): ShelfBook = when (next) {
        ReadingStatus.FINISHED -> copyWith(next, progress.completed(), at)
        ReadingStatus.WANT_TO_READ -> copyWith(next, progress.reset(), at)
        ReadingStatus.READING -> copyWith(next, progress, at)
    }

    fun recordPage(page: PageNumber, at: Instant): ShelfBook {
        val moved = progress.movedTo(page)
        val next = if (moved.isFinished()) ReadingStatus.FINISHED else ReadingStatus.READING
        return copyWith(next, moved, at)
    }

    fun rate(rating: Rating, at: Instant): ShelfBook =
        ShelfBook(book, status, progress, rating, at)

    fun readingPoint(): PageNumber = progress.currentPage

    private fun copyWith(
        status: ReadingStatus,
        progress: ReadingProgress,
        at: Instant,
    ): ShelfBook = ShelfBook(book, status, progress, myRating, at)
}
```

`changeStatus(READING)`이 쪽수를 그대로 두는 것에 주의한다. 「다 읽음 → 읽는 중」으로 되돌리면
쪽수가 총 쪽수에 남는다. 이건 "끝까지 읽었지만 다시 읽는 중"으로 읽히므로 그대로 둔다.

**감수한 손실**: 「읽는 중 80쪽 → 읽고 싶어요」로 되돌리면 80이 사라진다. 특히 서재 편집 모드의
일괄 상태 변경에서 여러 권의 기록이 한 번에 지워질 수 있다.

그래서 **기록이 사라지는지를 도메인이 판단해서 알려준다.** 경고를 띄울지는 화면이 정한다.

```kotlin
fun losesProgressBy(next: ReadingStatus): Boolean =
    next == ReadingStatus.WANT_TO_READ && !progress.isNotStarted()
```

화면은 일괄 변경 전에 이 값이 `true`인 책이 몇 권인지 세어 경고 여부를 결정한다. 다이얼로그
UI 는 [화면 명세](screen-specs.md)의 신규 UI 항목이다.

### 5.4 서재

편집 모드(36:900, 36:1060)에서 여러 권을 선택해 일괄 처리하므로, 그 규칙을 컬렉션 객체가 갖는다.

```kotlin
class Shelf(
    private val books: List<ShelfBook>,
) {
    fun filterBy(status: ReadingStatus?): List<ShelfBook> =
        if (status == null) books else books.filter { it.status == status }

    fun sortedByRecent(): List<ShelfBook> = books.sortedByDescending { it.lastRecordedAt }

    fun countOf(status: ReadingStatus?): Int = filterBy(status).size

    fun find(bookId: BookId): ShelfBook? = books.firstOrNull { it.book.id == bookId }

    fun contains(bookId: BookId): Boolean = find(bookId) != null

    fun changeStatusOf(
        bookIds: Set<BookId>,
        next: ReadingStatus,
        at: Instant,
    ): Shelf = Shelf(
        books.map { if (it.book.id in bookIds) it.changeStatus(next, at) else it },
    )

    fun remove(bookIds: Set<BookId>): Shelf = Shelf(books.filter { it.book.id !in bookIds })
}
```

`books`를 `private`로 둔 것이 핵심이다. 화면은 `filterBy`·`sortedByRecent`로 질문하고, 내부
리스트를 직접 순회하지 않는다. Figma의 `전체 12권`은 `countOf(null)`이다.

## 6. 별점

별점은 책당 하나이며 다시 매기면 덮어쓴다. 별점 다이얼로그의 「내 평점 기록 3회」는 **이 책의
이력이 아니라 내가 최근에 매긴 별점 목록**이다. Figma에 보이지 않는 도시·역병·마션 세 권이
그려져 있는 것을 근거로 삼았다. 「마션에 남겼던 별점을 확인하고」라는 설명 문구는 데이터와
어긋나므로 「최근 남긴 별점을 확인하고」로 고쳐야 한다.

```kotlin
class RatedBook(
    val bookId: BookId,
    val title: String,
    val rating: Rating,
    val ratedAt: LocalDate,
)
```

## 7. 감상

### 7.1 감상

```kotlin
class Note(
    val id: NoteId,
    val bookId: BookId,
    val author: NoteAuthor,
    val impression: String,
    val quote: Quote?,
    val chapter: String?,
    val readingPoint: PageNumber,
    val readCompleted: Boolean,
    val createdAt: Instant,
    val likeCount: Int,
    val likedByMe: Boolean,
    val replies: List<Reply>,
) {
    init {
        require(impression.isNotBlank()) { "느낀점은 비워둘 수 없습니다." }
        require(impression.length <= MAX_IMPRESSION_LENGTH) {
            "느낀점은 ${MAX_IMPRESSION_LENGTH}자 이하여야 합니다: ${impression.length}자"
        }
        require(likeCount >= 0) { "좋아요 수는 0 이상이어야 합니다." }
    }

    fun replyCount(): Int = replies.size

    fun containsSpoilerFor(readPage: PageNumber): Boolean = readingPoint > readPage

    companion object {
        const val MAX_IMPRESSION_LENGTH = 1000
    }
}
```

`impression`이 필수인 근거는 작성 폼(36:1337)의 「느낀점」에만 `필수` 배지가 붙어 있다는 점이다.
「인상 깊은 문구」에는 배지가 없다.

`MAX_IMPRESSION_LENGTH`는 Figma에 근거가 없다. 답글에만 `0 / 200`이 보인다. 임시로 1000자를
두었고 [미결정 사항](#11-미결정-사항)에 남겼다.

`readCompleted`는 카드 헤더의 `완독` 배지다. 감상을 남긴 시점에 그 책을 다 읽은 상태였는지를
뜻하며, 나중에 책을 다 읽어도 과거 감상의 배지는 바뀌지 않는다.

### 7.2 인용

```kotlin
class Quote(
    val text: String,
    val page: PageNumber,
) {
    init {
        require(text.isNotBlank()) { "인용문은 비워둘 수 없습니다." }
    }
}
```

`Note.readingPoint`(어디까지 읽고 남긴 감상인가)와 `Quote.page`(문장을 어디서 가져왔나)를
분리했다. 200쪽까지 읽은 사람이 50쪽 문장을 인용해도 스포일러 기준이 200으로 유지된다.

**대가**: Figma 작성 폼에는 쪽수 입력칸이 하나뿐이라 **입력칸을 하나 더 추가해야 한다.** 폼 UI
구체안은 화면 설계에서 따로 확정한다.

### 7.3 답글

```kotlin
class Reply(
    val id: ReplyId,
    val author: NoteAuthor,
    val content: String,
    val createdAt: Instant,
    val likeCount: Int,
    val likedByMe: Boolean,
) {
    init {
        require(content.isNotBlank()) { "답글은 비워둘 수 없습니다." }
        require(content.length <= MAX_LENGTH) {
            "답글은 ${MAX_LENGTH}자 이하여야 합니다: ${content.length}자"
        }
    }

    companion object {
        const val MAX_LENGTH = 200
    }
}
```

200자는 Figma 답글 입력창의 `0 / 200`에서 가져왔다.

### 7.4 작성자 표시명

```kotlin
sealed interface NoteAuthor {
    val id: ReaderId

    data class Named(
        override val id: ReaderId,
        val nickname: Nickname,
    ) : NoteAuthor

    data class Anonymous(
        override val id: ReaderId,
        val handle: String,
    ) : NoteAuthor
}
```

`handle`은 서버가 만들어 내려주는 `참새 1204`다. 화면에 붙는 `(익명)` 접미사는 표시 규칙이므로
UiModel에서 조립한다.

익명 핸들이 사용자마다 고정인지 감상마다 새로 생기는지는 Figma로 판단할 수 없다. 서버 결정
사항이며 [API 계약](../../docs/api-contract.md)에서 다룬다.

## 8. 스포일러 경계

이 앱의 시그니처 규칙이다. 판정이 화면 코드로 흩어지면 한 곳만 틀려도 본문이 샌다.

### 8.1 판정 규칙

Figma(36:712)에서 카드 헤더의 `p.160까지`와 가드 문구의 `160쪽 이후 내용을 포함해요`가 일치한다.
즉 **감상의 `readingPoint`가 내가 읽은 쪽수보다 크면 가린다.**

기준이 되는 「내가 읽은 쪽수」는 `ShelfBook.progress.currentPage`와 **같은 값**이다. 다이얼로그의
`/ 412쪽`이 그 책의 총 쪽수이고, 책 상세의 「지금 읽는 쪽 80 / 308쪽」과 같은 구조라는 점을
근거로 삼았다. 별도의 열람 전용 쪽수를 두지 않는다.

### 8.2 경계 객체

```kotlin
class SpoilerBoundary(
    private val readPage: PageNumber,
    private val revealedBooks: Set<BookId>,
) {
    fun visibilityOf(note: Note): NoteVisibility = when {
        note.bookId in revealedBooks -> NoteVisibility.Visible
        note.containsSpoilerFor(readPage) -> NoteVisibility.Hidden(note.readingPoint)
        else -> NoteVisibility.Visible
    }

    fun reveal(bookId: BookId): SpoilerBoundary =
        SpoilerBoundary(readPage, revealedBooks + bookId)

    fun movedTo(page: PageNumber): SpoilerBoundary = SpoilerBoundary(page, revealedBooks)
}

sealed interface NoteVisibility {
    data object Visible : NoteVisibility

    data class Hidden(val requiredPage: PageNumber) : NoteVisibility
}
```

### 8.3 정해진 범위

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| 「스포일러 감수하고 보기」 적용 범위 | 그 **책의 감상 전부** | 감상 30개짜리 책에서 다이얼로그가 반복되면 못 쓴다 |
| 감수 상태 유지 | **메모리에만** (ViewModel) | 앱을 다시 켜면 보호가 기본값으로 돌아온다. 저장 코드 없음 |
| 「입력한 쪽수까지 보기」의 저장 위치 | `ShelfBook.progress` | 8.1과 같은 이유 |
| 서재에 없는 책에서 쪽수를 입력하면 | `READING` 상태로 **서재에 자동 추가** | 「쪽수 기록 = 읽는 중」 규칙 하나로 통일. 검색의 「읽는 중 시작」과 같은 동작 |

**감수한 손실**: 감상을 보려고 대충 입력한 쪽수가 독서 기록을 덮어쓴다. 스포일러를 보려다
서재가 의도치 않게 늘어날 수도 있다.

### 8.4 본문이 화면에 도달하지 않게 한다

가려진 감상은 UiModel 단계에서 **본문 문자열을 담지 않는다.** 화면에 도달하지 않은 문자열은
실수로도 그릴 수 없다.

```kotlin
sealed interface NoteUiModel {
    data class Visible(val impression: String, ...) : NoteUiModel
    data class Hidden(val requiredPageLabel: String, ...) : NoteUiModel   // 본문 없음
}
```

**이 매핑은 `commonMain`의 presentation에 있다.** Android와 iOS가 같은 UiModel을 받으므로 판정이
한 번만 존재하고, iOS가 Swift로 가림 로직을 다시 구현할 일이 없다. presentation을 공유하기로 한
주된 이유가 이것이다.

SKIE 같은 브리지를 붙이면 이 `sealed`가 Swift에서 exhaustive enum이 되어, iOS에서 `Hidden` 분기를
빠뜨리면 컴파일 오류가 난다. 브리지 선택 기준은 [KMP 셋업](kmp-setup.md)에 있다.

자세한 내용은 [앱 아키텍처의 UiModel 절](app-architecture.md#72-uimodel)에 있다.

## 9. 독자와 정체성

### 9.1 프로필

「실명 공개인데 닉네임이 없는 상태」는 존재할 수 없다. Figma 문구 「해제하면 닉네임을 설정해야
합니다」가 그대로 불변식이 된다.

```kotlin
class ReaderProfile(
    val id: ReaderId,
    val nickname: Nickname?,
    val publishesAnonymously: Boolean,
) {
    init {
        require(publishesAnonymously || nickname != null) {
            "익명 공개를 해제하려면 닉네임을 먼저 설정해야 합니다."
        }
    }

    fun canRevealName(): Boolean = nickname != null

    fun revealName(): ReaderProfile = ReaderProfile(id, nickname, publishesAnonymously = false)

    fun hideName(): ReaderProfile = ReaderProfile(id, nickname, publishesAnonymously = true)

    fun changeNickname(newNickname: Nickname): ReaderProfile =
        ReaderProfile(id, newNickname, publishesAnonymously)
}
```

닉네임을 계정 속성으로 보존하는 것이 `sealed interface`를 쓰지 않은 이유다. 익명으로 돌렸다가
다시 실명으로 바꿀 때 재입력을 요구하지 않는다.

화면 흐름은 이렇게 된다.

```
익명 ON  → 토글 해제 시도
            canRevealName() == true  → 그대로 revealName()
            canRevealName() == false → 닉네임 설정 다이얼로그 → changeNickname() → revealName()
익명 OFF → 토글 켜기 → hideName()   (닉네임은 그대로 남는다)
```

### 9.2 열람자

로그인 여부에 따라 가진 것이 다르다. 게스트는 쿼터가 있고 프로필이 없다. 회원은 그 반대다.
여기서는 `sealed interface`가 맞다.

```kotlin
sealed interface Viewer {
    data class Guest(val quota: GuestQuota) : Viewer

    data class Member(val profile: ReaderProfile) : Viewer
}

class GuestQuota(
    val viewed: Int,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        require(viewed >= 0) { "열람 횟수는 0 이상이어야 합니다." }
        require(limit > 0) { "열람 한도는 1 이상이어야 합니다." }
    }

    fun remaining(): Int = (limit - viewed).coerceAtLeast(0)

    fun isExhausted(): Boolean = viewed >= limit

    fun consumed(): GuestQuota = GuestQuota((viewed + 1).coerceAtMost(limit), limit)

    companion object {
        const val DEFAULT_LIMIT = 3
    }
}
```

Figma 홈 배너의 `지금 2 / 3`은 `viewed = 2, limit = 3`이다. 세는 단위는 「감상 전문 읽기」를 누른
횟수로 본다. 홈 카드에 「감상 전문 읽기 · 댓글 12」, 「전문 읽기 · 댓글 28」 링크가 있는 것이
근거다.

카운트는 **기기에 저장한다**(DataStore). 비로그인 상태라 서버가 사용자를 식별할 방법이 없기
때문이다. 재설치하면 리셋되는 것은 감수한다.

### 9.3 인증은 경계만 정의한다

Figma 12개 화면에 **로그인 화면이 없다.** 로그인 수단(카카오·구글·이메일), 토큰 형식, 갱신
전략은 이번 설계에서 정하지 않는다. 대신 경계를 고정해서 나중에 무엇이 오든 화면과 도메인이
바뀌지 않게 한다.

```kotlin
interface AuthRepository {
    fun viewer(): Flow<Viewer>

    suspend fun signOut()
}
```

홈 배너의 「로그인하고 계속 읽기」는 지금은 빈 콜백으로 둔다.

## 10. 규칙 목록 (테스트 대상)

도메인 테스트는 이 목록을 그대로 옮긴다.

**쪽수**
1. 음수 쪽수는 만들 수 없다.
2. 총 쪽수는 1 이상이어야 한다.
3. 진행 쪽수가 총 쪽수를 넘으면 만들 수 없다.

**독서 상태**
4. `FINISHED`로 바꾸면 진행 쪽수가 총 쪽수가 된다.
5. `WANT_TO_READ`로 바꾸면 진행 쪽수가 0이 된다.
6. `READING`으로 바꾸면 진행 쪽수가 유지된다.
7. 마지막 쪽을 기록하면 상태가 `FINISHED`가 된다.
8. 중간 쪽을 기록하면 상태가 `READING`이 된다.
9. `FINISHED`인데 진행 쪽수가 총 쪽수가 아닌 `ShelfBook`은 만들 수 없다.
10. `WANT_TO_READ`인데 진행 쪽수가 0이 아닌 `ShelfBook`은 만들 수 없다.
10-1. 읽던 책을 `WANT_TO_READ`로 되돌리면 기록이 사라진다고 알려준다.
10-2. 이미 0쪽이거나 다른 상태로 바꿀 때는 잃을 기록이 없다고 알려준다.

**서재**
11. 상태 필터가 `null`이면 전체를 돌려준다.
12. 여러 권의 상태를 한 번에 바꾸면 선택된 권만 바뀐다.
13. 삭제하면 선택된 권만 빠진다.

**별점**
14. 0.5 단위가 아닌 별점은 만들 수 없다.
15. 0.5 미만이나 5.0 초과는 만들 수 없다.
16. 같은 점수로 만든 두 `Rating`은 같다.

**감상**
17. 느낀점이 공백이면 만들 수 없다.
18. 답글이 200자를 넘으면 만들 수 없다.
19. 인용문이 공백이면 `Quote`를 만들 수 없다.

**스포일러**
20. 감상의 `readingPoint`가 내가 읽은 쪽수보다 크면 가려진다.
21. 같으면 보인다.
22. 감수한 책의 감상은 `readingPoint`와 무관하게 보인다.
23. 감수는 그 책에만 적용되고 다른 책은 여전히 가려진다.
24. 읽은 쪽수를 올리면 그 이하 감상이 보인다.

**정체성**
25. 닉네임이 2자 미만이거나 15자 초과면 만들 수 없다.
25-1. 한글·영문·숫자와 `-`, `_` 외의 문자가 들어가면 만들 수 없다.
25-2. 자음·모음 단독(`ㅋㅋㅋ`, `ㅏㅏ`)은 만들 수 없다.
25-3. 화면이 예외 없이 유효성을 확인할 수 있다(`isValid`).
26. 닉네임 없이 실명 공개인 `ReaderProfile`은 만들 수 없다.
27. 익명으로 전환해도 닉네임은 남는다.

**게스트**
28. 열람할 때마다 남은 횟수가 줄어든다.
29. 한도에 도달하면 소진된다.
30. 한도를 넘겨 소비해도 `viewed`가 한도를 넘지 않는다.

## 11. 미결정 사항

| 항목 | 상태 | 필요한 결정 |
| --- | --- | --- |
| 느낀점 최대 길이 | 임시 1000자 | Figma에 근거 없음. 답글만 200자로 표시됨 |
| 감상 작성 폼의 쪽수 입력칸 | UI 신규 | 읽은 지점과 인용 쪽을 어떻게 나란히 놓을지 |
| 익명 핸들의 수명 | 서버 결정 | 사용자마다 고정인지, 감상마다 새로 생기는지 |
| 인기·정렬 기준 | 서버 결정 | 「인기순」의 정의, 「지금 인기 책들」의 산출 방식 |
| 「밑줄이 겹친 책」의 정의 | 서버 결정 | 같은 구간 인용이 몇 건 이상일 때 겹친 것으로 보는지 |
| 알림 | 미정 | 홈 헤더의 종 아이콘 동작 |
| 로그인 | 범위 밖 | 수단·토큰·화면 전부 |

## 부록 A. 참고 프로젝트에서 바꾼 것

`wtc/android-shopping-order`를 참고하되, 아래 다섯 가지는 의도적으로 다르게 간다. 객체지향
관점에서 문제가 되는 지점이라 근거와 함께 남긴다.

**1. Repository 인터페이스를 `domain`에 둔다**

참고 프로젝트는 `data/repository/product/ProductRepository.kt`에 인터페이스가 있고,
`feature/productlist/ProductListViewModel.kt:18-20`이 그것을 import 한다. `feature` 아래
6개 파일이 `data`를 import 하며, 테스트용 `FakeProductRepository.kt:3`도 마찬가지다.
의존성 역전 원칙이 뒤집힌 배치이고, 나중에 `:domain` 모듈로 분리하려 하면 순환 참조가 된다.

**2. 감싼 값을 도로 꺼내지 않는다**

참고 프로젝트는 `Money`를 만들어 놓고 `Product.priceAmount(): Int`로 알맹이를 노출한다. 그 결과
`ProductUiModel(price: Int)`가 `price * quantity`를 `Int` 산술로 다시 계산해, `Money.times`
연산자가 있는데도 쓰이지 않는다. 포장의 이득이 경계에서 사라진 상태다. 우리는 표시용 문자열까지
UiModel에서 완성하고 도메인 타입을 풀지 않는다.

**3. 컬렉션을 감춘다**

참고 프로젝트의 `Cart.cartContents`는 `public`이라, `ProductListViewModel`이
`cart.cartContents.firstOrNull { it.hasProductId(productId) }?.id`로 내부를 순회한다. 디미터
법칙 위반이며, `Cart`가 답해야 할 질문을 호출부가 대신 계산한다. 우리 `Shelf`는 리스트를
`private`로 두고 `find`·`filterBy`·`countOf`로 답한다.

**4. 도메인에 플랫폼을 들이지 않는다**

참고 프로젝트의 `domain/Product.kt`에 `import android.R.attr.name`이 남아 있다. 실제로 쓰이지는
않지만 도메인이 Android 의존을 받아들일 수 있는 상태라는 뜻이다.

우리는 도메인이 `commonMain`에 있어 **이 실수가 컴파일 단계에서 막힌다.** `android.*`도
`java.*`도 Kotlin/Native 타겟에서 해석되지 않으므로, 규칙이 아니라 빌드가 강제한다. 단일 모듈
Android 프로젝트에서는 얻을 수 없던 이점이다.

**5. Repository가 실제로 일을 한다**

참고 프로젝트의 `ProductRepositoryImpl`은 두 메서드 모두 DataSource로 한 줄 위임한다. DTO→도메인
매핑과 HTTP 오류 판정을 DataSource가 하기 때문에 Repository 계층이 아무 일도 하지 않는다.
우리는 DataSource가 DTO를 반환하고 Repository가 매핑을 담당한다.
