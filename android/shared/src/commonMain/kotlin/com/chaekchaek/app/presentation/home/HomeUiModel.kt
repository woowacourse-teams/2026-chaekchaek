package com.chaekchaek.app.presentation.home

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.note.NoteId

sealed interface FeedSectionUiModel {
    data class TrendingBooks(
        val books: List<TrendingBookUiModel>,
        val moreLabel: String,
    ) : FeedSectionUiModel

    data class RecentQuotes(
        val title: String,
        val cards: List<QuoteCardUiModel>,
    ) : FeedSectionUiModel

    data class OverlappedBooks(
        val title: String,
        val cards: List<OverlappedCardUiModel>,
    ) : FeedSectionUiModel
}

data class TrendingBookUiModel(
    val bookId: BookId,
    val title: String,
    val coverId: String,
    val statsLabel: String,
    val isbn13: String = "",
)

data class QuoteCardUiModel(
    val noteId: NoteId,
    val bookId: BookId,
    val isbn13: String = "",
    val bookTitle: String,
    val coverId: String,
    val authorLabel: String,
    val quoteText: String,
    val replyLabel: String,
)

data class OverlappedCardUiModel(
    val bookId: BookId,
    val title: String,
    val coverId: String,
    val noteCountLabel: String,
    val authorLabel: String,
    val excerpt: String,
    val replyLabel: String,
)

data class GuestBannerUiModel(
    val progressLabel: String,
    val exhausted: Boolean,
)

data class ReadingBookUiModel(
    val bookId: BookId = BookId(""),
    val isbn13: String = "",
    val title: String,
    val coverId: String,
    val currentPage: Int,
    val totalPages: Int,
)

internal object HomeLabels {
    const val RECENT_QUOTES_TITLE = "최근 감상들"
    const val OVERLAPPED_BOOKS_TITLE = "밑줄이 겹친 책"

    fun trendingMore(totalCount: Int): String = "지금 인기 책들 +$totalCount"

    fun trendingStats(noteCount: Int, replyCount: Int): String =
        "감상 $noteCount · 댓글 $replyCount"

    fun author(authorLabel: String, timeLabel: String): String = "$authorLabel · $timeLabel"

    fun quoteReply(replyCount: Int): String = "답글 $replyCount"

    fun noteCount(noteCount: Int): String = "감상 $noteCount"

    fun overlappedReply(replyCount: Int): String = "답글 $replyCount"

    fun guestProgress(viewed: Int, limit: Int): String = "지금 $viewed / $limit"
}
