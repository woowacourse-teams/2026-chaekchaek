package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.domain.shelf.ReadingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveSpineFilterTest {
    private fun book(id: String, spine: String?) = ArchiveBookUiModel(
        id, 1L, id, "", "", "", "", spine, ReadingStatus.READING, 0, 0, 0L,
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
    fun uncheckingRestoresBooksEvenWhenFilteredShelfWasEmpty() {
        val books = listOf(book("generated", null))
        assertEquals(emptyList(), booksForSpineShelf(books, true))
        assertEquals(books, booksForSpineShelf(books, false))
    }
}
