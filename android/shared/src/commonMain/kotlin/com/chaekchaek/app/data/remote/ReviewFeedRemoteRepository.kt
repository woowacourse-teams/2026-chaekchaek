package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.feed.QuoteCard
import com.chaekchaek.app.domain.feed.ReviewFeedPage
import com.chaekchaek.app.domain.feed.ReviewFeedRepository
import com.chaekchaek.app.domain.note.NoteId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlin.time.Instant

class ReviewFeedRemoteRepository(
    private val client: HttpClient = createHttpClient(),
    private val apiConfiguration: ApiConfiguration = ApiConfiguration.current,
) : ReviewFeedRepository {
    override suspend fun reviewFeed(page: Int, accessToken: String?): ReviewFeedPage =
        client.get("${apiConfiguration.baseUrl}/api/v1/feed/reviews") {
            parameter("page", page)
            accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }.body<ReviewFeedPageDto>().toReviewFeedPage()
}

@Serializable
internal data class ReviewFeedPageDto(
    val reviews: List<ReviewFeedItemDto>,
    val totalCount: Int,
    val nextPage: Int? = null,
)

@Serializable
internal data class ReviewFeedItemDto(
    val reviewId: Long,
    val content: String,
    val quote: String? = null,
    val isSpoiler: Boolean,
    val createdAt: String,
    val author: LatestReviewAuthorDto,
    val likeCount: Int,
    val likedByMe: Boolean,
    val replyCount: Int,
    val bookId: Long,
    val isbn13: String,
    val bookTitle: String,
    val bookCoverImageUrl: String,
)

internal fun ReviewFeedPageDto.toReviewFeedPage() = ReviewFeedPage(
    reviews = reviews.map { review ->
        QuoteCard(
            reviewId = review.reviewId,
            noteId = NoteId("review-${review.reviewId}"),
            bookId = BookId(review.bookId.toString()),
            isbn13 = review.isbn13,
            bookTitle = review.bookTitle,
            coverId = review.bookCoverImageUrl,
            authorLabel = review.author.displayName,
            authorProfileImageUrl = review.author.profileImageUrl,
            createdAt = Instant.parse(review.createdAt),
            quoteText = review.content,
            replyCount = review.replyCount,
            quote = review.quote,
            likeCount = review.likeCount,
            likedByMe = review.likedByMe,
            isSpoiler = review.isSpoiler,
        )
    },
    totalCount = totalCount,
    nextPage = nextPage,
)
