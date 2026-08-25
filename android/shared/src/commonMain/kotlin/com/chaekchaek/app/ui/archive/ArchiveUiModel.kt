package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.RemoteLibraryBook
import com.chaekchaek.app.domain.shelf.ReadingStatus
import kotlin.time.Instant

data class ArchiveBookUiModel(
    val id: String,
    val bookId: Long,
    val title: String,
    val creator: String,
    val publisher: String,
    val category: String,
    val coverUrl: String,
    val status: ReadingStatus,
    val currentPage: Int,
    val totalPages: Int,
    val lastRecordedAt: Long,
) {
    val progressRatio: Float
        get() = if (totalPages == 0) 0f else currentPage.toFloat() / totalPages
}

data class ArchiveUiState(
    val items: List<ArchiveBookUiModel> = emptyList(),
    val anonymousReviews: Boolean = true,
    val nickname: String = "",
    val showLoading: Boolean = false,
    val errorMessage: String? = null,
)

internal val ReadingStatus.label: String
    get() = when (this) {
        ReadingStatus.WANT_TO_READ -> "읽고 싶어요"
        ReadingStatus.READING -> "읽는 중"
        ReadingStatus.FINISHED -> "다 읽음"
    }

internal val ReadingStatus.apiValue: String
    get() = when (this) {
        ReadingStatus.WANT_TO_READ -> "WANT_TO_READ"
        ReadingStatus.READING -> "READING"
        ReadingStatus.FINISHED -> "FINISHED"
    }

internal fun RemoteLibraryBook.toArchiveBookUiModel(): ArchiveBookUiModel {
    val pages = totalPages?.coerceAtLeast(0) ?: 0
    val safeCurrentPage = currentPage.coerceAtLeast(0).let { if (pages == 0) it else it.coerceAtMost(pages) }
    return ArchiveBookUiModel(
        id = isbn13,
        bookId = bookId,
        title = title,
        creator = authors.joinToString(", "),
        publisher = publisher,
        category = category,
        coverUrl = coverImageUrl,
        status = ReadingStatus.entries.firstOrNull { it.apiValue == status } ?: ReadingStatus.READING,
        currentPage = safeCurrentPage,
        totalPages = pages,
        lastRecordedAt = runCatching { Instant.parse(readingUpdatedAt).toEpochMilliseconds() }.getOrDefault(0L),
    )
}
