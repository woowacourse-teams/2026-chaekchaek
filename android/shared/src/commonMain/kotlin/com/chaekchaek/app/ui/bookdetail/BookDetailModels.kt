package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.data.remote.ReviewScope
import com.chaekchaek.app.data.remote.ReviewSort
import com.chaekchaek.app.domain.rating.Rating
import kotlinx.serialization.Serializable

@Serializable
data class BookDetailArgs(
    val id: String,
    val isbn13: String = "",
    val bookId: Long? = null,
    val title: String,
    val creator: String = "",
    val publisher: String = "",
    val year: String = "",
    val category: String = "",
    val totalPages: Int = 0,
    val coverUrl: String = "",
    val coverId: String = "",
)

enum class ReadingStatus(val label: String, val apiValue: String) {
    WantToRead("읽고 싶어요", "WANT_TO_READ"),
    Reading("읽는 중", "READING"),
    Finished("다 읽음", "FINISHED"),
}

data class RatedBookUiModel(
    val bookId: String,
    val title: String,
    val rating: Rating,
    val ratedAtLabel: String,
)

internal fun List<RatedBookUiModel>.withRecentRating(
    bookId: String,
    title: String,
    rating: Rating,
    ratedAtLabel: String,
): List<RatedBookUiModel> =
    (filterNot { it.bookId == bookId } + RatedBookUiModel(bookId, title, rating, ratedAtLabel)).takeLast(3)

sealed interface BookDetailAuthenticatedAction {
    val requiresMember: Boolean get() = true

    data object AddToLibrary : BookDetailAuthenticatedAction
    data object OpenPageInput : BookDetailAuthenticatedAction
    data object OpenRating : BookDetailAuthenticatedAction
    data object OpenReview : BookDetailAuthenticatedAction {
        override val requiresMember = false
    }
    data object OpenMineFeed : BookDetailAuthenticatedAction
    data class ChangeStatus(val status: ReadingStatus) : BookDetailAuthenticatedAction
    data class SavePage(val page: Int) : BookDetailAuthenticatedAction
    data class LikeReview(val reviewId: Long, val likedByMe: Boolean) : BookDetailAuthenticatedAction {
        override val requiresMember = false
    }
    data class CreateReply(val reviewId: Long, val content: String) : BookDetailAuthenticatedAction {
        override val requiresMember = false
    }
    data class LikeReply(val replyId: Long, val likedByMe: Boolean) : BookDetailAuthenticatedAction {
        override val requiresMember = false
    }
    data class EditReview(val reviewId: Long, val request: ReviewCreateRequest) : BookDetailAuthenticatedAction {
        override val requiresMember = false
    }
    data class DeleteReview(val reviewId: Long) : BookDetailAuthenticatedAction {
        override val requiresMember = false
    }
    data class EditReply(val replyId: Long, val content: String) : BookDetailAuthenticatedAction {
        override val requiresMember = false
    }
    data class DeleteReply(val replyId: Long) : BookDetailAuthenticatedAction {
        override val requiresMember = false
    }
}

data class BookDetailUiState(
    val book: BookDetailArgs? = null,
    val detail: BookDetail? = null,
    val reviews: List<BookReview> = emptyList(),
    val reviewCount: Int = 0,
    val reviewScope: ReviewScope = ReviewScope.ALL,
    val reviewSort: ReviewSort = ReviewSort.LATEST,
    val nextReviewPage: Int? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSubmitting: Boolean = false,
    val signedIn: Boolean = false,
    val pendingAction: BookDetailAuthenticatedAction? = null,
    val requestError: String? = null,
    val guestNickname: String? = null,
) {
    val displayBook: BookDetailArgs?
        get() = detail?.toBookDetailArgs(book ?: return null) ?: book
}

private fun BookDetail.toBookDetailArgs(fallback: BookDetailArgs) =
    fallback.copy(
        isbn13 = isbn13,
        bookId = bookId,
        title = title,
        creator = (authors + translators.map { "$it 옮김" }).joinToString(" · "),
        publisher = publisher,
        year = publishedDate?.take(4).orEmpty(),
        category = category,
        totalPages = totalPages ?: 0,
        coverUrl = coverImageUrl,
    )
