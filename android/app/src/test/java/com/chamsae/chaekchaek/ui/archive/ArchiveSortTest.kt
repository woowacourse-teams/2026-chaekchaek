package com.chamsae.chaekchaek.ui.archive

import com.chamsae.chaekchaek.data.ArchivedBook
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveSortTest {
  @Test
  fun `기록 시각을 최신순과 오래된순으로 정렬한다`() {
    val older = book("older", 1L)
    val newer = book("newer", 2L)

    assertEquals(listOf(newer, older), sortArchivedBooks(listOf(older, newer), ArchiveSort.Recent))
    assertEquals(listOf(older, newer), sortArchivedBooks(listOf(newer, older), ArchiveSort.Oldest))
  }

  private fun book(id: String, recordedAt: Long) =
    ArchivedBook(
      id = id,
      title = id,
      creator = "",
      publisher = "",
      year = "",
      coverUrl = "",
      note = "",
      lastRecordedAt = recordedAt,
    )
}
