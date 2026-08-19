package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.feed.FeedSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PopularBooksRemoteRepositoryTest {
    @Test
    fun `인기 책 응답을 홈 콜라주로 바꾼다`() {
        val feed = PopularBooksResponseDto(
            books = listOf(
                PopularBookDto(
                    bookId = 42,
                    title = "마션",
                    coverImageUrl = "https://example.com/martian.jpg",
                    reviewCount = 12,
                    replyCount = 3,
                ),
            ),
        ).toHomeFeed()

        val section = assertIs<FeedSection.TrendingBooks>(feed.visibleSections().single())
        val book = section.books.single()
        assertEquals("42", book.bookId.value)
        assertEquals("https://example.com/martian.jpg", book.coverId)
        assertEquals(12, book.noteCount)
        assertEquals(3, book.replyCount)
    }
}
