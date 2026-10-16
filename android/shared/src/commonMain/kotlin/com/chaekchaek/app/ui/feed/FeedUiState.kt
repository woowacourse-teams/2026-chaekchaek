package com.chaekchaek.app.ui.feed

import com.chaekchaek.app.presentation.common.AppError
import com.chaekchaek.app.presentation.home.QuoteCardUiModel

sealed interface FeedUiState {
    data object Loading : FeedUiState

    data class Failure(val error: AppError) : FeedUiState

    data class Content(
        val reviews: List<QuoteCardUiModel>,
        val totalCount: Int,
        val nextPage: Int?,
        val loadingMore: Boolean = false,
        val requestError: FeedRequestError? = null,
    ) : FeedUiState

    data object Empty : FeedUiState
}

sealed interface FeedRequestError {
    val message: String

    data object LoadMore : FeedRequestError {
        override val message = "다음 감상을 불러오지 못했어요. 다시 시도해 주세요."
    }

    data object Interaction : FeedRequestError {
        override val message = "요청을 처리하지 못했어요. 다시 시도해 주세요."
    }
}
