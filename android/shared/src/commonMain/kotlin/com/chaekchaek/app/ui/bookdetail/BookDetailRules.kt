package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.domain.rating.Rating
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val API_LOADING_DELAY_MILLIS = 500L

internal object BookDetailInputRules {
    const val MAX_CONTENT_LENGTH = 1000
    const val MAX_QUOTE_LENGTH = 500
    const val MAX_CHAPTER_LENGTH = 255

    fun validPage(value: String, totalPages: Int): Int? =
        value.toIntOrNull()?.takeIf { it >= 0 && (totalPages <= 0 || it <= totalPages) }

    fun canSubmitReview(content: String, pageValue: String, totalPages: Int): Boolean =
        content.isNotBlank() && content.length <= MAX_CONTENT_LENGTH &&
            (pageValue.isBlank() || validPage(pageValue, totalPages) != null)
}

internal object ReplyInputRules {
    const val MAX_LENGTH = 200

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
    currentPage: Int,
    reviewPage: Int?,
    isSpoiler: Boolean,
    spoilersRevealed: Boolean,
): Boolean = !spoilersRevealed && (isSpoiler || reviewPage?.let { it > currentPage } == true)

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
