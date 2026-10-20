package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.ui.input.TextInputPolicy
import com.chaekchaek.app.domain.rating.Rating
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.ReplyPage
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.data.remote.ReviewReply
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal const val API_LOADING_DELAY_MILLIS = 500L

internal fun averageRatingInTenths(averageRating: Double): Int =
    (averageRating.coerceIn(0.0, 5.0) * 10).roundToInt()

internal data class ReviewMetadata(
    val dateLabel: String,
    val progressLabel: String?,
)

internal fun reviewMetadata(createdAt: String, currentPage: Int?): ReviewMetadata =
    ReviewMetadata(
        dateLabel = createdAt.take(10).replace('-', '.'),
        progressLabel = currentPage?.let { "p.${it}까지" },
    )

/** 숨긴 읽기 메타데이터는 수정 시 보존하고, 별점 시안 값은 기존 감상 API로 전달하지 않는다. */
internal fun buildReviewRequest(
    draft: BookDetailReviewDraft,
    initialReview: BookReview? = null,
): ReviewCreateRequest {
    require(draft.rating == null) { "별점이 포함된 초안은 기존 감상 API에 저장할 수 없습니다." }
    return ReviewCreateRequest(
        content = draft.content.trim(),
        quote = draft.quote?.trim()?.ifEmpty { null },
        chapter = initialReview?.chapter,
        currentPage = initialReview?.currentPage,
        isSpoiler = draft.isSpoiler,
    )
}

internal object BookDetailInputRules {
    const val MAX_CONTENT_LENGTH = 1000
    const val MAX_QUOTE_LENGTH = 500
    const val MAX_CHAPTER_LENGTH = 255

    val ContentPolicy = TextInputPolicy(MAX_CONTENT_LENGTH, utf16 = true)
    val QuotePolicy = TextInputPolicy(MAX_QUOTE_LENGTH, utf16 = true)
    val ChapterPolicy = TextInputPolicy(MAX_CHAPTER_LENGTH, utf16 = true)
    val PagePolicy = TextInputPolicy(7, digitsOnly = true)

    fun validPage(value: String, totalPages: Int): Int? =
        value.toIntOrNull()?.takeIf { it >= 0 && (totalPages <= 0 || it <= totalPages) }

    fun canSubmitPage(page: Int?): Boolean = page != null

    fun canSubmitReview(content: String, pageValue: String, totalPages: Int): Boolean =
        content.isNotBlank() && content.length <= MAX_CONTENT_LENGTH &&
            (pageValue.isBlank() || validPage(pageValue, totalPages) != null)

    fun hasReviewDraft(
        content: String,
        quote: String,
        chapter: String,
        pageValue: String,
        initialPage: Int,
        isSpoiler: Boolean,
    ): Boolean =
        content.isNotEmpty() || quote.isNotEmpty() || chapter.isNotEmpty() || isSpoiler ||
            pageValue != initialPage.takeIf { it > 0 }?.toString().orEmpty()
}

internal object ReplyInputRules {
    const val MAX_LENGTH = 200
    val Policy = TextInputPolicy(MAX_LENGTH, utf16 = true)

    fun canSubmit(value: String): Boolean = value.isNotBlank() && value.length <= MAX_LENGTH
}

internal object RatingDialogRules {
    fun ratingAtSlot(slot: Int): Rating = Rating.ofHalfStars(slot + 1)

    fun label(rating: Rating): String = "${rating.score} · ${description(rating)}"

    private fun description(rating: Rating): String =
        when (rating.score.toInt()) {
            0, 1 -> "아쉬워요"
            2 -> "그저 그래요"
            3 -> "괜찮아요"
            4 -> "좋았어요"
            else -> "최고예요"
        }
}

internal fun shouldLockReview(
    reviewId: Long,
    isSpoiler: Boolean,
    revealedReviewIds: Set<Long>,
): Boolean = isSpoiler && reviewId !in revealedReviewIds

internal fun canManageContent(writtenByMe: Boolean, deleted: Boolean): Boolean = writtenByMe && !deleted

internal fun visibleReviews(reviews: List<BookReview>): List<BookReview> = reviews.filterNot { it.deleted }

internal data class ReviewDisplay(
    val reviews: List<BookReview>,
    val count: Int,
    val showEmpty: Boolean,
)

internal fun reviewDisplay(
    reviews: List<BookReview>,
    totalCount: Int,
    hasMore: Boolean,
): ReviewDisplay {
    val visibleReviews = visibleReviews(reviews)
    val loadedDeletedCount = reviews.count { it.deleted }
    return ReviewDisplay(
        reviews = visibleReviews,
        count = (totalCount - loadedDeletedCount).coerceAtLeast(visibleReviews.size),
        showEmpty = visibleReviews.isEmpty() && !hasMore,
    )
}

internal data class ReplyDisplay(val replies: List<ReviewReply>, val hasMore: Boolean)

internal fun replyDisplay(replies: List<ReviewReply>, totalCount: Int): ReplyDisplay = ReplyDisplay(
    replies = replies.filterNot { it.deleted },
    // 서버의 전체 개수에는 삭제 항목도 포함되므로 원본 조회 개수로 비교한다.
    hasMore = replies.size < totalCount,
)

internal suspend fun loadAllReplies(loadPage: suspend (Int) -> ReplyPage): List<ReviewReply> {
    val replies = mutableListOf<ReviewReply>()
    val loadedPages = mutableSetOf<Int>()
    var page: Int? = 1
    while (page != null && loadedPages.add(page)) {
        val response = loadPage(page)
        replies += response.items
        page = response.nextPage
    }
    return replies.distinctBy(ReviewReply::replyId)
}

internal fun maskAsChirps(content: String): String =
    content.map { character ->
        if (character.isWhitespace() || character in VISIBLE_MASK_PUNCTUATION) character else '짹'
    }.joinToString("")

internal suspend fun <T> withDelayedLoading(
    onLoadingChanged: (Boolean) -> Unit,
    request: suspend () -> T,
): T = coroutineScope {
    val indicator = launch {
        delay(API_LOADING_DELAY_MILLIS)
        onLoadingChanged(true)
    }
    try {
        request()
    } finally {
        indicator.cancelAndJoin()
        onLoadingChanged(false)
    }
}

private const val VISIBLE_MASK_PUNCTUATION = ".,!?…:;\"'“”‘’()[]{}-·"
