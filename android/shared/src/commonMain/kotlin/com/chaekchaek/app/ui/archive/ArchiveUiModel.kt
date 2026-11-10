package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.RemoteLibraryBook
import kotlin.time.Instant

data class ArchiveBookUiModel(
    val id: String,
    val bookId: Long,
    val title: String,
    val creator: String,
    val publisher: String,
    val category: String,
    val coverUrl: String,
    val spineUrl: String?,
    val lastRecordedAt: Long,
)

data class ArchiveUiState(
    val items: List<ArchiveBookUiModel> = emptyList(),
    val showLoading: Boolean = false,
    val errorMessage: String? = null,
    val mutationRevision: Long = 0,
)

internal fun booksForSpineShelf(
    books: List<ArchiveBookUiModel>,
    onlySpineImages: Boolean,
    failedSpineUrls: Set<String> = emptySet(),
): List<ArchiveBookUiModel> =
    if (onlySpineImages) {
        books.filter { book ->
            !book.spineUrl.isNullOrBlank() && book.spineUrl !in failedSpineUrls
        }
    } else {
        books
    }

internal fun RemoteLibraryBook.toArchiveBookUiModel(): ArchiveBookUiModel {
    return ArchiveBookUiModel(
        id = isbn13,
        bookId = bookId,
        title = title,
        creator = authors.joinToString(", "),
        publisher = publisher,
        category = category,
        coverUrl = coverImageUrl,
        spineUrl = spineImageUrl,
        lastRecordedAt = runCatching { Instant.parse(readingUpdatedAt).toEpochMilliseconds() }.getOrDefault(0L),
    )
}
