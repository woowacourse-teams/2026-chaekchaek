package com.chaekchaek.app.ui.archive

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveSpineFilterTest {
    private fun book(id: String, spine: String?) = ArchiveBookUiModel(
        id, 1L, id, "", "", "", "", spine, 0L,
    )

    @Test
    fun uncheckedShowsEveryBookInOriginalOrder() {
        val books = listOf(book("generated", null), book("image", "https://example.com/spine"))
        assertEquals(books, booksForSpineShelf(books, false))
    }

    @Test
    fun checkedExcludesMissingEmptyAndWhitespaceUrls() {
        val image = book("image", "https://example.com/spine")
        val books = listOf(book("missing", null), image, book("empty", ""), book("blank", " "))
        assertEquals(listOf(image), booksForSpineShelf(books, true))
        assertEquals(4, books.size)
    }

    @Test
    fun checkedExcludesUrlsThatFailedToLoad() {
        val failedUrl = "https://example.com/missing-spine"
        val failed = book("failed", failedUrl)
        val available = book("available", "https://example.com/available-spine")

        assertEquals(
            listOf(available),
            booksForSpineShelf(listOf(failed, available), true, setOf(failedUrl)),
        )
    }

    @Test
    fun uncheckingRestoresBooksEvenWhenFilteredShelfWasEmpty() {
        val books = listOf(book("generated", null))
        assertEquals(emptyList<ArchiveBookUiModel>(), booksForSpineShelf(books, true))
        assertEquals(books, booksForSpineShelf(books, false))
    }
}
