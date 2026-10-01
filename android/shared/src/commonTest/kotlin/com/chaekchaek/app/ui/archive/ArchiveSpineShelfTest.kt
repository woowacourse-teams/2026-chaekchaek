package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.domain.shelf.ReadingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveSpineShelfTest {
    @Test
    fun `책등 이미지가 있는 책만 책장에 표시한다`() {
        val withSpine = book("with-spine", "https://example.com/spine")
        val missingSpine = book("missing-spine", null)
        val blankSpine = book("blank-spine", " ")

        assertEquals(listOf(withSpine), booksWithAvailableSpines(listOf(withSpine, missingSpine, blankSpine)))
    }

    @Test
    fun `책등 이미지가 하나도 없으면 책장이 비어 있다`() {
        assertEquals(emptyList(), booksWithAvailableSpines(listOf(book("missing-spine", null))))
    }

    private fun book(id: String, spineUrl: String?) = ArchiveBookUiModel(
        id = id,
        bookId = id.hashCode().toLong(),
        title = id,
        creator = "",
        publisher = "",
        category = "",
        coverUrl = "",
        spineUrl = spineUrl,
        status = ReadingStatus.READING,
        currentPage = 0,
        totalPages = 0,
        lastRecordedAt = 0,
    )
}
