package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.domain.feed.HomeFeed
import com.chaekchaek.app.domain.feed.QuoteCard
import com.chaekchaek.app.domain.feed.ReadingBook
import com.chaekchaek.app.domain.feed.TrendingBook
import com.chaekchaek.app.domain.note.NoteId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlin.time.Instant

class PopularBooksRemoteRepository(
    private val client: HttpClient = createHttpClient(),
) : FeedRepository {
    override suspend fun homeFeed(accessToken: String?): HomeFeed {
        val popularBooks = client.get("$BASE_URL/api/v1/home/popular-books")
            .body<PopularBooksResponseDto>()
        val latestReviews = client.get("$BASE_URL/api/v1/home/latest-reviews")
            .body<LatestReviewsResponseDto>()
        val readingBook = accessToken?.let { token ->
            client.get("$BASE_URL/api/v1/library") {
                header(HttpHeaders.Authorization, "Bearer $token")
                url {
                    parameters.append("page", "1")
                    parameters.append("status", "READING")
                    parameters.append("sort", "RECENT")
                }
            }.body<LibraryResponseDto>().items.firstOrNull()
        }
        return popularBooks.toHomeFeed(latestReviews, readingBook)
    }

    private companion object {
        const val BASE_URL = "https://api.chaekchaek.com"
    }
}

@Serializable
internal data class PopularBooksResponseDto(
    val books: List<PopularBookDto>,
)

@Serializable
internal data class PopularBookDto(
    val bookId: Long,
    val isbn13: String = "",
    val title: String,
    val coverImageUrl: String,
    val reviewCount: Int,
    val replyCount: Int,
)

@Serializable
internal data class LatestReviewsResponseDto(
    val reviews: List<LatestReviewDto>,
)

@Serializable
internal data class LatestReviewDto(
    val content: String,
    val createdAt: String,
    val author: LatestReviewAuthorDto,
    val replyCount: Int,
    val bookId: Long,
    val isbn13: String = "",
    val bookTitle: String,
    val bookCoverImageUrl: String,
)

@Serializable
internal data class LatestReviewAuthorDto(
    val displayName: String,
    val profileImageUrl: String? = null,
)

@Serializable
internal data class LibraryResponseDto(
    val items: List<ReadingBookDto>,
)

@Serializable
internal data class ReadingBookDto(
    val bookId: Long,
    val isbn13: String,
    val title: String,
    val coverImageUrl: String,
    val totalPages: Int? = null,
    val currentPage: Int,
)

internal fun PopularBooksResponseDto.toHomeFeed(
    latestReviews: LatestReviewsResponseDto = LatestReviewsResponseDto(emptyList()),
    readingBook: ReadingBookDto? = null,
): HomeFeed = HomeFeed(
    sections = listOf(
        FeedSection.TrendingBooks(
            books = books.map { book ->
                TrendingBook(
                    bookId = BookId(book.bookId.toString()),
                    isbn13 = book.isbn13,
                    title = book.title,
                    coverId = book.coverImageUrl,
                    noteCount = book.reviewCount,
                    replyCount = book.replyCount,
                )
            },
            totalCount = books.size,
        ),
        FeedSection.RecentQuotes(
            cards = latestReviews.reviews.map { review ->
                QuoteCard(
                    noteId = NoteId("${review.bookId}-${review.createdAt}"),
                    bookId = BookId(review.bookId.toString()),
                    isbn13 = review.isbn13,
                    bookTitle = review.bookTitle,
                    coverId = review.bookCoverImageUrl,
                    authorLabel = review.author.displayName,
                    authorProfileImageUrl = review.author.profileImageUrl,
                    createdAt = Instant.parse(review.createdAt),
                    quoteText = review.content,
                    replyCount = review.replyCount,
                )
            },
        ),
    ),
    readingBook = readingBook?.let { book ->
        ReadingBook(
            bookId = BookId(book.bookId.toString()),
            isbn13 = book.isbn13,
            title = book.title,
            coverId = book.coverImageUrl,
            currentPage = book.currentPage,
            totalPages = book.totalPages ?: 0,
        )
    },
)
