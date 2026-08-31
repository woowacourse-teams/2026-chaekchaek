package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BookDetailRemoteRepository(
  private val client: HttpClient = createHttpClient(),
) {

  suspend fun detail(isbn13: String, credential: WriteCredential? = null): BookDetail =
    client.get("$BASE_URL/api/v1/books/by-isbn/$isbn13") {
      credential?.let { authenticate(it) }
    }.body<BookDetailDto>().toBookDetail()

  suspend fun reviews(
    bookId: Long,
    scope: ReviewScope,
    sort: ReviewSort,
    credential: WriteCredential? = null,
    page: Int = FIRST_PAGE,
  ): ReviewPage =
    client.get("$BASE_URL/api/v1/books/$bookId/reviews") {
      parameter("page", page)
      parameter("feed", scope.name)
      parameter("sort", sort.name)
      credential?.let { authenticate(it) }
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

  suspend fun ratingComparison(isbn13: String, criterion: Double, accessToken: String): RatingComparison =
    client.get("$BASE_URL/api/v1/members/me/ratings/comparison") {
      header(HttpHeaders.Authorization, "Bearer $accessToken")
      parameter("isbn13", isbn13)
      parameter("criterion", criterion)
    }.body<RatingComparisonDto>().toRatingComparison()

  suspend fun createReview(bookId: Long, request: ReviewCreateRequest, credential: WriteCredential): BookReview =
    client.post("$BASE_URL/api/v1/books/$bookId/reviews") {
      authenticatedJson(
        credential,
        if (credential is WriteCredential.Guest) request.copy(currentPage = null, totalPages = null) else request,
      )
    }.body<ReviewDto>().toBookReview()

  suspend fun updateReview(
    reviewId: Long,
    request: ReviewCreateRequest,
    credential: WriteCredential,
  ): BookReview =
    client.patch("$BASE_URL/api/v1/reviews/$reviewId") {
      authenticate(credential)
      contentType(ContentType.Application.Json)
      setBody(request.toUpdateBody(includeReadingProgress = credential is WriteCredential.Member))
    }.body<ReviewDto>().toBookReview()

  suspend fun deleteReview(reviewId: Long, credential: WriteCredential) {
    client.delete("$BASE_URL/api/v1/reviews/$reviewId") {
      authenticate(credential)
    }
  }

  suspend fun replies(
    reviewId: Long,
    credential: WriteCredential? = null,
    page: Int = FIRST_PAGE,
  ): ReplyPage =
    client.get("$BASE_URL/api/v1/reviews/$reviewId/replies") {
      parameter("page", page)
      credential?.let { authenticate(it) }
    }.body<ReplyPageDto>().toReplyPage()

  suspend fun likeReview(reviewId: Long, credential: WriteCredential): ReactionResult =
    client.post("$BASE_URL/api/v1/reviews/$reviewId/reactions") {
      authenticate(credential)
    }.body<ReactionDto>().toReactionResult()

  suspend fun unlikeReview(reviewId: Long, credential: WriteCredential) {
    client.delete("$BASE_URL/api/v1/reviews/$reviewId/reactions") {
      authenticate(credential)
    }
  }

  suspend fun createReply(reviewId: Long, content: String, credential: WriteCredential): ReviewReply =
    client.post("$BASE_URL/api/v1/reviews/$reviewId/replies") {
      authenticatedJson(credential, ReplyCreateRequest(content))
    }.body<ReviewReplyDto>().toReviewReply()

  suspend fun updateReply(replyId: Long, content: String, credential: WriteCredential): ReviewReply =
    client.patch("$BASE_URL/api/v1/replies/$replyId") {
      authenticatedJson(credential, ReplyCreateRequest(content))
    }.body<ReviewReplyDto>().toReviewReply()

  suspend fun deleteReply(replyId: Long, credential: WriteCredential) {
    client.delete("$BASE_URL/api/v1/replies/$replyId") {
      authenticate(credential)
    }
  }

  suspend fun likeReply(replyId: Long, credential: WriteCredential): ReactionResult =
    client.post("$BASE_URL/api/v1/replies/$replyId/reactions") {
      authenticate(credential)
    }.body<ReactionDto>().toReactionResult()

  suspend fun unlikeReply(replyId: Long, credential: WriteCredential) {
    client.delete("$BASE_URL/api/v1/replies/$replyId/reactions") {
      authenticate(credential)
    }
  }

  private fun io.ktor.client.request.HttpRequestBuilder.authenticatedJson(accessToken: String, body: Any) {
    header(HttpHeaders.Authorization, "Bearer $accessToken")
    contentType(ContentType.Application.Json)
    setBody(body)
  }

  private fun io.ktor.client.request.HttpRequestBuilder.authenticatedJson(
    credential: WriteCredential,
    body: Any,
  ) {
    authenticate(credential)
    contentType(ContentType.Application.Json)
    setBody(body)
  }

  private fun io.ktor.client.request.HttpRequestBuilder.authenticate(credential: WriteCredential) {
    header(credential.headerName, credential.headerValue)
  }

  private companion object {
    const val BASE_URL = "https://api.chaekchaek.com"
    const val FIRST_PAGE = 1
  }
}

private fun ReviewCreateRequest.toUpdateBody(includeReadingProgress: Boolean) = buildJsonObject {
  put("content", content)
  put("quote", quote?.let(::JsonPrimitive) ?: JsonNull)
  put("chapter", chapter?.let(::JsonPrimitive) ?: JsonNull)
  put("isSpoiler", isSpoiler)
  if (includeReadingProgress) {
    put("currentPage", currentPage?.let(::JsonPrimitive) ?: JsonNull)
    put("totalPages", totalPages?.let(::JsonPrimitive) ?: JsonNull)
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

data class RatingComparison(
  val lower: RatingComparisonBook?,
  val current: RatingComparisonBook?,
  val higher: RatingComparisonBook?,
)

data class RatingComparisonBook(
  val bookId: Long,
  val title: String,
  val myRating: Double,
  val ratingUpdatedAt: String,
)

data class ReviewPage(val totalCount: Int, val nextPage: Int?, val items: List<BookReview>)

data class ReplyPage(val totalCount: Int, val nextPage: Int?, val items: List<ReviewReply>)

data class ReactionResult(val likeCount: Int, val likedByMe: Boolean)

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
  val likedByMe: Boolean = false,
  val isSpoiler: Boolean = false,
  val recentReplies: List<ReviewReply> = emptyList(),
  val authorProfileImageUrl: String? = null,
  val writtenByMe: Boolean = false,
  val deleted: Boolean = false,
)

data class ReviewReply(
  val replyId: Long,
  val content: String,
  val authorName: String,
  val anonymous: Boolean,
  val likeCount: Int,
  val likedByMe: Boolean = false,
  val authorProfileImageUrl: String? = null,
  val writtenByMe: Boolean = false,
  val deleted: Boolean = false,
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
private data class RatingComparisonDto(
  val lower: RatingComparisonBookDto? = null,
  val current: RatingComparisonBookDto? = null,
  val higher: RatingComparisonBookDto? = null,
)

@Serializable
private data class RatingComparisonBookDto(
  val bookId: Long,
  val title: String,
  val myRating: Double,
  val ratingUpdatedAt: String,
)

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
internal data class ReplyPageDto(
  val totalCount: Int,
  val nextPage: Int? = null,
  val items: List<ReviewReplyDto>,
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
  val likedByMe: Boolean = false,
  val isSpoiler: Boolean = false,
  val recentReplies: List<ReviewReplyDto> = emptyList(),
  val deleted: Boolean,
)

@Serializable
internal data class ReviewAuthorDto(
  val displayName: String,
  val anonymous: Boolean,
  val mine: Boolean,
  val actorType: String,
  val profileImageUrl: String? = null,
)

@Serializable
internal data class ReviewReplyDto(
  val replyId: Long,
  val content: String,
  val author: ReviewAuthorDto,
  val likeCount: Int,
  val likedByMe: Boolean = false,
  val deleted: Boolean,
)

@Serializable
internal data class ReactionDto(val likeCount: Int, val likedByMe: Boolean)

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

private fun RatingComparisonDto.toRatingComparison() =
  RatingComparison(lower?.toRatingComparisonBook(), current?.toRatingComparisonBook(), higher?.toRatingComparisonBook())

private fun RatingComparisonBookDto.toRatingComparisonBook() =
  RatingComparisonBook(bookId, title, myRating, ratingUpdatedAt)

internal fun BookDetailRecordDto.toLibraryRecord() =
  status?.let { LibraryRecord(it, currentPage ?: 0, myRating) }

internal fun ReviewPageDto.toReviewPage() =
  ReviewPage(
    totalCount = totalCount,
    nextPage = nextPage,
    items = items.map(ReviewDto::toBookReview),
  )

internal fun ReplyPageDto.toReplyPage() =
  ReplyPage(
    totalCount = totalCount,
    nextPage = nextPage,
    items = items.map(ReviewReplyDto::toReviewReply),
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
    authorProfileImageUrl = author.profileImageUrl,
    anonymous = author.anonymous,
    replyCount = replyCount,
    likeCount = likeCount,
    likedByMe = likedByMe,
    isSpoiler = isSpoiler,
    recentReplies = recentReplies.map(ReviewReplyDto::toReviewReply),
    writtenByMe = author.mine,
    deleted = deleted,
  )

internal fun ReviewReplyDto.toReviewReply() =
  ReviewReply(
    replyId = replyId,
    content = content,
    authorName = author.displayName,
    authorProfileImageUrl = author.profileImageUrl,
    anonymous = author.anonymous,
    likeCount = likeCount,
    likedByMe = likedByMe,
    writtenByMe = author.mine,
    deleted = deleted,
  )

internal fun ReactionDto.toReactionResult() = ReactionResult(likeCount, likedByMe)
