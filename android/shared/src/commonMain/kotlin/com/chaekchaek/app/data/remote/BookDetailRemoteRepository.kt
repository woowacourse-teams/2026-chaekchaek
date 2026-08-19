package com.chaekchaek.app.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

class BookDetailRemoteRepository {
  private val client = createHttpClient()

  suspend fun detail(isbn13: String): BookDetail =
    client.get("$BASE_URL/api/v1/books/by-isbn/$isbn13").body<BookDetailDto>().toBookDetail()

  suspend fun reviews(bookId: Long, scope: ReviewScope, sort: ReviewSort, accessToken: String? = null): ReviewPage =
    client.get("$BASE_URL/api/v1/books/$bookId/reviews") {
      parameter("page", FIRST_PAGE)
      parameter("feed", scope.name)
      parameter("sort", sort.name)
      accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }.body<ReviewPageDto>().toReviewPage()

  private companion object {
    const val BASE_URL = "https://api.chaekchaek.com"
    const val FIRST_PAGE = 1
  }
}

enum class ReviewScope { ALL, MINE }

enum class ReviewSort { PAGE, LATEST, OLDEST, POPULAR }

data class BookDetail(
  val bookId: Long?,
  val isbn13: String,
  val title: String,
  val authors: List<String>,
  val translators: List<String>,
  val publisher: String,
  val category: String,
  val publishedDate: String?,
  val totalPages: Int?,
  val coverImageUrl: String,
  val commentCount: Int?,
  val averageRating: Double?,
  val ratingCount: Int?,
)

data class ReviewPage(val totalCount: Int, val items: List<BookReview>)

data class BookReview(
  val reviewId: Long,
  val content: String,
  val quote: String?,
  val chapter: String?,
  val currentPage: Int?,
  val createdAt: String,
  val authorName: String,
  val anonymous: Boolean,
  val replyCount: Int,
  val likeCount: Int,
)

@Serializable
internal data class BookDetailDto(
  val bookId: Long? = null,
  val isbn13: String,
  val title: String,
  val authors: List<String>,
  val translators: List<String>,
  val publisher: String,
  val category: String,
  val publishedDate: String? = null,
  val totalPages: Int? = null,
  val coverImageUrl: String,
  val commentCount: Int? = null,
  val averageRating: Double? = null,
  val ratingCount: Int? = null,
)

@Serializable
internal data class ReviewPageDto(
  val totalCount: Int,
  val items: List<ReviewDto>,
)

@Serializable
internal data class ReviewDto(
  val reviewId: Long,
  val content: String,
  val quote: String? = null,
  val chapter: String? = null,
  val currentPage: Int? = null,
  val createdAt: String,
  val author: ReviewAuthorDto,
  val replyCount: Int,
  val likeCount: Int,
)

@Serializable
internal data class ReviewAuthorDto(val displayName: String, val anonymous: Boolean)

internal fun BookDetailDto.toBookDetail() =
  BookDetail(
    bookId = bookId,
    isbn13 = isbn13,
    title = title,
    authors = authors,
    translators = translators,
    publisher = publisher,
    category = category.substringAfterLast('>'),
    publishedDate = publishedDate,
    totalPages = totalPages,
    coverImageUrl = coverImageUrl,
    commentCount = commentCount,
    averageRating = averageRating,
    ratingCount = ratingCount,
  )

internal fun ReviewPageDto.toReviewPage() =
  ReviewPage(
    totalCount = totalCount,
    items = items.map {
      BookReview(
        reviewId = it.reviewId,
        content = it.content,
        quote = it.quote,
        chapter = it.chapter,
        currentPage = it.currentPage,
        createdAt = it.createdAt,
        authorName = it.author.displayName,
        anonymous = it.author.anonymous,
        replyCount = it.replyCount,
        likeCount = it.likeCount,
      )
    },
  )
