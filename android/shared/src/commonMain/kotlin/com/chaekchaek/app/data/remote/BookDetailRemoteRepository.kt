package com.chaekchaek.app.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class BookDetailRemoteRepository {
  private val client = createHttpClient()

  suspend fun detail(isbn13: String, accessToken: String? = null): BookDetail =
    client.get("$BASE_URL/api/v1/books/by-isbn/$isbn13") {
      accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }.body<BookDetailDto>().toBookDetail()

  suspend fun reviews(
    bookId: Long,
    scope: ReviewScope,
    sort: ReviewSort,
    accessToken: String? = null,
    page: Int = FIRST_PAGE,
  ): ReviewPage =
    client.get("$BASE_URL/api/v1/books/$bookId/reviews") {
      parameter("page", page)
      parameter("feed", scope.name)
      parameter("sort", sort.name)
      accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }.body<ReviewPageDto>().toReviewPage()

  suspend fun addToLibrary(isbn13: String, totalPages: Int?, accessToken: String): LibraryRecord =
    client.post("$BASE_URL/api/v1/library") {
      authenticatedJson(accessToken, LibraryAddRequest(isbn13, "READING", totalPages))
    }.body<LibraryRecordDto>().toLibraryRecord()

  suspend fun updateReadingStatus(bookId: Long, status: String, accessToken: String): LibraryRecord =
    client.patch("$BASE_URL/api/v1/library/$bookId") {
      authenticatedJson(accessToken, LibraryUpdateRequest(status = status))
    }.body<LibraryRecordDto>().toLibraryRecord()

  suspend fun updateCurrentPage(bookId: Long, currentPage: Int, totalPages: Int?, accessToken: String): LibraryRecord =
    client.patch("$BASE_URL/api/v1/library/$bookId") {
      authenticatedJson(accessToken, LibraryUpdateRequest(currentPage = currentPage, totalPages = totalPages))
    }.body<LibraryRecordDto>().toLibraryRecord()

  suspend fun rate(bookId: Long, rating: Double, accessToken: String): LibraryRecord =
    client.put("$BASE_URL/api/v1/library/$bookId/rating") {
      authenticatedJson(accessToken, RatingRequest(rating))
    }.body<LibraryRecordDto>().toLibraryRecord()

  suspend fun createReview(bookId: Long, request: ReviewCreateRequest, accessToken: String): BookReview =
    client.post("$BASE_URL/api/v1/books/$bookId/reviews") {
      authenticatedJson(accessToken, request)
    }.body<ReviewDto>().toBookReview()

  suspend fun likeReview(reviewId: Long, accessToken: String) {
    client.post("$BASE_URL/api/v1/reviews/$reviewId/reactions") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
    }
  }

  suspend fun createReply(reviewId: Long, content: String, accessToken: String) {
    client.post("$BASE_URL/api/v1/reviews/$reviewId/replies") {
      authenticatedJson(accessToken, ReplyCreateRequest(content))
    }
  }

  private fun io.ktor.client.request.HttpRequestBuilder.authenticatedJson(accessToken: String, body: Any) {
    header(HttpHeaders.Authorization, "Bearer $accessToken")
    contentType(ContentType.Application.Json)
    setBody(body)
  }

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
  val myRecord: LibraryRecord?,
)

data class LibraryRecord(
  val status: String,
  val currentPage: Int,
  val rating: Double?,
  val bookId: Long? = null,
)

data class ReviewPage(val totalCount: Int, val nextPage: Int?, val items: List<BookReview>)

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
  val myRecord: BookDetailRecordDto? = null,
)

@Serializable
internal data class LibraryRecordDto(
  val bookId: Long,
  val status: String,
  val currentPage: Int,
  val rating: Double? = null,
)

@Serializable
internal data class BookDetailRecordDto(
  val status: String? = null,
  val currentPage: Int? = null,
  val myRating: Double? = null,
)

@Serializable
private data class LibraryAddRequest(val isbn13: String, val status: String, val totalPages: Int? = null)

@Serializable
private data class LibraryUpdateRequest(
  val status: String? = null,
  val currentPage: Int? = null,
  val totalPages: Int? = null,
)

@Serializable
private data class RatingRequest(val rating: Double)

@Serializable
data class ReviewCreateRequest(
  val content: String,
  val quote: String? = null,
  val chapter: String? = null,
  val currentPage: Int? = null,
  val totalPages: Int? = null,
  val isSpoiler: Boolean = false,
)

@Serializable
private data class ReplyCreateRequest(val content: String)

@Serializable
internal data class ReviewPageDto(
  val totalCount: Int,
  val nextPage: Int? = null,
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
    myRecord = myRecord?.toLibraryRecord(),
  )

internal fun LibraryRecordDto.toLibraryRecord() = LibraryRecord(status, currentPage, rating, bookId)

internal fun BookDetailRecordDto.toLibraryRecord() =
  status?.let { LibraryRecord(it, currentPage ?: 0, myRating) }

internal fun ReviewPageDto.toReviewPage() =
  ReviewPage(
    totalCount = totalCount,
    nextPage = nextPage,
    items = items.map(ReviewDto::toBookReview),
  )

internal fun ReviewDto.toBookReview() =
  BookReview(
    reviewId = reviewId,
    content = content,
    quote = quote,
    chapter = chapter,
    currentPage = currentPage,
    createdAt = createdAt,
    authorName = author.displayName,
    anonymous = author.anonymous,
    replyCount = replyCount,
    likeCount = likeCount,
  )
