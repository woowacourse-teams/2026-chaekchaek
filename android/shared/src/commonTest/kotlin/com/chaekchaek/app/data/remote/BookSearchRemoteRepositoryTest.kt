package com.chaekchaek.app.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class BookSearchRemoteRepositoryTest {
    @Test
    fun `search item maps server fields for the existing search screen`() {
        val result = BookSearchItemDto(
            title = "마션",
            coverImageUrl = "https://example.com/martian.jpg",
            authors = listOf("앤디 위어"),
            translators = listOf("박아람"),
            publishedDate = "2015-07-24",
            isbn13 = "9788925556789",
            category = "국내도서>소설>과학소설",
            publisher = "알에이치코리아",
        ).toSearchResult()

        assertEquals("앤디 위어 · 박아람 옮김", result.creator)
        assertEquals("2015", result.year)
        assertEquals("과학소설", result.category)
        assertEquals(0, result.totalPages)
    }
}
