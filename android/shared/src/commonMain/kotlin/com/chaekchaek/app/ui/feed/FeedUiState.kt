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
        val requestError: String? = null,
    ) : FeedUiState

    data object Empty : FeedUiState
}
