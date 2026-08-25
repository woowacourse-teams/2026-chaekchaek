package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.domain.shelf.ReadingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveSortTest {
    @Test
    fun sortsByRecordingTime() {
        val older = book("older", 1L)
        val newer = book("newer", 2L)

        assertEquals(listOf(newer, older), sortArchiveBooks(listOf(older, newer), ArchiveSort.Recent))
        assertEquals(listOf(older, newer), sortArchiveBooks(listOf(newer, older), ArchiveSort.Oldest))
    }

    private fun book(id: String, recordedAt: Long) = ArchiveBookUiModel(
        id = id,
        bookId = recordedAt,
        title = id,
        creator = "",
        publisher = "",
        category = "",
        coverUrl = "",
        status = ReadingStatus.READING,
        currentPage = 0,
        totalPages = 0,
        lastRecordedAt = recordedAt,
    )
}
