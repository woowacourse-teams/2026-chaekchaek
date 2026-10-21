package com.chaekchaek.app.presentation.home


sealed interface HomeUiState {
    data object Loading : HomeUiState


    data class Content(
        val sections: List<FeedSectionUiModel>,
        val readingBook: ReadingBookUiModel? = null,
    ) : HomeUiState

    data object Empty : HomeUiState
}
