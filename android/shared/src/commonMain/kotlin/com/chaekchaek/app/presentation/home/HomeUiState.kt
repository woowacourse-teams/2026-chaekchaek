package com.chaekchaek.app.presentation.home

import com.chaekchaek.app.presentation.common.AppError

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Failure(val error: AppError) : HomeUiState

    data class Content(
        val sections: List<FeedSectionUiModel>,
        val readingBook: ReadingBookUiModel? = null,
    ) : HomeUiState

    data object Empty : HomeUiState
}
