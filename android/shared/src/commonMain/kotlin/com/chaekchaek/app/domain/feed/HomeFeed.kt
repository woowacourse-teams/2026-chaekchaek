package com.chaekchaek.app.domain.feed

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.note.NoteId
import kotlin.time.Instant

/**
 * 홈 한 번 로드의 스냅샷. 섹션 구성과 순서는 서버가 정하고 앱은 받은 순서대로 그린다.
 *
 * [sections] 를 private 으로 둔 이유는 "무엇을 그릴지"를 [visibleSections] 가 판정하기
 * 때문이다. 빈 섹션을 걸러내는 규칙이 화면마다 흩어지지 않게 한다.
 */
class HomeFeed(
    private val sections: List<FeedSection>,
) {
    /** 내용이 없는 섹션은 그리지 않는다. */
    fun visibleSections(): List<FeedSection> = sections.filterNot { it.isEmpty() }

    fun isEmpty(): Boolean = visibleSections().isEmpty()
}

/**
 * 홈의 섹션 한 덩어리.
 *
 * 서버가 모르는 타입을 내려주면 data 계층이 이 목록으로 매핑하지 못하고 건너뛴다. 구버전 앱이
 * 새 섹션 때문에 죽지 않아야 하므로 매핑 실패는 오류가 아니라 무시다.
 */
sealed interface FeedSection {
    fun isEmpty(): Boolean

    /** Figma 의 표지 콜라주와 `지금 인기 책들 +12`. */
    data class TrendingBooks(
        val books: List<TrendingBook>,
        val totalCount: Int,
    ) : FeedSection {
        override fun isEmpty(): Boolean = books.isEmpty()
    }

    /** Figma 의 `방금 남겨진 문장`. */
    data class RecentQuotes(
        val cards: List<QuoteCard>,
    ) : FeedSection {
        override fun isEmpty(): Boolean = cards.isEmpty()
    }

    /** Figma 의 `밑줄이 겹친 책`. */
    data class OverlappedBooks(
        val cards: List<OverlappedBook>,
    ) : FeedSection {
        override fun isEmpty(): Boolean = cards.isEmpty()
    }
}

class TrendingBook(
    val bookId: BookId,
    val title: String,
    val coverId: String,
    val noteCount: Int,
    val replyCount: Int,
)

/**
 * `방금 남겨진 문장` 카드. 감상 전문이 아니라 홈에 필요한 미리보기다.
 *
 * [createdAt] 은 Instant 로 보관한다. `4분 전` 같은 상대 시각은 표시 시점에 계산해야 하므로
 * 문자열로 저장하지 않는다.
 */
class QuoteCard(
    val noteId: NoteId,
    val bookId: BookId,
    val bookTitle: String,
    val coverId: String,
    val authorLabel: String,
    val createdAt: Instant,
    val quoteText: String,
    val replyCount: Int,
)

/** `밑줄이 겹친 책` 카드. */
class OverlappedBook(
    val bookId: BookId,
    val title: String,
    val coverId: String,
    val noteCount: Int,
    val authorLabel: String,
    val createdAt: Instant,
    val excerpt: String,
    val replyCount: Int,
)
