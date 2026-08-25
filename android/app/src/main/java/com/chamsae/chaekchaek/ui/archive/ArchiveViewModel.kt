package com.chamsae.chaekchaek.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.ReadingStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ArchiveUiState(
  val items: List<ArchivedBook> = emptyList(),
  val anonymousReviews: Boolean = true,
  val nickname: String = "",
)

class ArchiveViewModel(private val libraryRepository: LibraryRepository) : ViewModel() {
  val uiState: StateFlow<ArchiveUiState> =
    combine(
      libraryRepository.items,
      libraryRepository.anonymousReviews,
      libraryRepository.nickname,
    ) { items, anonymousReviews, nickname ->
      ArchiveUiState(items, anonymousReviews, nickname)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue =
        ArchiveUiState(
          items = libraryRepository.items.value,
          anonymousReviews = libraryRepository.anonymousReviews.value,
          nickname = libraryRepository.nickname.value,
        ),
    )

  suspend fun remove(bookIds: Set<String>) = libraryRepository.remove(bookIds)

  fun changeStatus(bookIds: Set<String>, status: ReadingStatus) =
    libraryRepository.changeStatus(bookIds, status)

  suspend fun setAnonymousReviews(anonymous: Boolean, nickname: String = "") =
    libraryRepository.setAnonymousReviews(anonymous, nickname)
}
