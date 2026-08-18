package com.chaekchaek.app.data.remote.dto

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.domain.feed.HomeFeed
import com.chaekchaek.app.domain.feed.OverlappedBook
import com.chaekchaek.app.domain.feed.QuoteCard
import com.chaekchaek.app.domain.feed.TrendingBook
import com.chaekchaek.app.domain.note.NoteId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * `GET /feed/home` 응답. 섹션 배열을 순서대로 받는다.
 *
 * 서버가 새 섹션 타입을 추가해도 구버전 앱이 죽지 않아야 하므로, 모르는 `type` 은 오류가
 * 아니라 무시다. [toDomain] 이 매핑하지 못한 섹션을 걸러낸다.
 */
@Serializable
data class HomeFeedResponse(
    val sections: List<FeedSectionDto>,
)

@Serializable
data class FeedSectionDto(
    val type: String,
    val totalCount: Int = 0,
    val books: List<TrendingBookDto> = emptyList(),
    val quotes: List<QuoteCardDto> = emptyList(),
    val overlapped: List<OverlappedBookDto> = emptyList(),
)

@Serializable
data class TrendingBookDto(
    val id: String,
    val title: String,
    @SerialName("coverId") val coverId: String,
    val noteCount: Int,
    val replyCount: Int,
)

@Serializable
data class QuoteCardDto(
    val noteId: String,
    val bookId: String,
    val bookTitle: String,
    val coverId: String,
    val authorLabel: String,
    val createdAt: String,
    val quoteText: String,
    val replyCount: Int,
)

@Serializable
data class OverlappedBookDto(
    val bookId: String,
    val title: String,
    val coverId: String,
    val noteCount: Int,
    val authorLabel: String,
    val createdAt: String,
    val excerpt: String,
    val replyCount: Int,
)

private const val TYPE_TRENDING = "TRENDING_BOOKS"
private const val TYPE_RECENT_QUOTES = "RECENT_QUOTES"
private const val TYPE_OVERLAPPED = "OVERLAPPED_BOOKS"

fun HomeFeedResponse.toDomain(): HomeFeed = HomeFeed(sections.mapNotNull { it.toDomain() })

/** 모르는 타입은 null 을 돌려주고 호출부가 건너뛴다. */
private fun FeedSectionDto.toDomain(): FeedSection? = when (type) {
    TYPE_TRENDING -> FeedSection.TrendingBooks(books.map { it.toDomain() }, totalCount)
    TYPE_RECENT_QUOTES -> FeedSection.RecentQuotes(quotes.map { it.toDomain() })
    TYPE_OVERLAPPED -> FeedSection.OverlappedBooks(overlapped.map { it.toDomain() })
    else -> null
}

private fun TrendingBookDto.toDomain() = TrendingBook(
    bookId = BookId(id),
    title = title,
    coverId = coverId,
    noteCount = noteCount,
    replyCount = replyCount,
)

private fun QuoteCardDto.toDomain() = QuoteCard(
    noteId = NoteId(noteId),
    bookId = BookId(bookId),
    bookTitle = bookTitle,
    coverId = coverId,
    authorLabel = authorLabel,
    createdAt = Instant.parse(createdAt),
    quoteText = quoteText,
    replyCount = replyCount,
)

private fun OverlappedBookDto.toDomain() = OverlappedBook(
    bookId = BookId(bookId),
    title = title,
    coverId = coverId,
    noteCount = noteCount,
    authorLabel = authorLabel,
    createdAt = Instant.parse(createdAt),
    excerpt = excerpt,
    replyCount = replyCount,
)
