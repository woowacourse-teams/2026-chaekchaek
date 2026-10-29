package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.BookshelfBook
import com.chaekchaek.app.data.remote.BookshelfBookSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class BookshelfSpineMappingTest {
    @Test fun `목록과 상세가 같은 표지와 제목을 대체 책등에 전달한다`() {
        val summary = BookshelfBookSummary(42, "isbn", "해인의 섬", "cover", authors = listOf("저자"))
        val detail = BookshelfBook(42, "isbn", "해인의 섬", "cover", authors = listOf("저자"))
        assertEquals(detail.asArchive().coverUrl, summary.asArchive().coverUrl)
        assertEquals(detail.asArchive().title, summary.asArchive().title)
        assertEquals(detail.asArchive().creator, summary.asArchive().creator)
        assertEquals(detail.asArchive().spineUrl, summary.asArchive().spineUrl)
    }
}
