package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.feed.FeedSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

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

    @Test
    fun `최신 감상과 읽는 중 책을 홈 피드로 합친다`() {
        val feed = PopularBooksResponseDto(emptyList()).toHomeFeed(
            latestReviews = LatestReviewsResponseDto(
                reviews = listOf(
                    LatestReviewDto(
                        content = "오래 멈춰 읽었다.",
                        createdAt = "2026-08-20T01:29:34Z",
                        author = LatestReviewAuthorDto(
                            displayName = "다정한 참새",
                            profileImageUrl = "https://example.com/profile.jpg",
                        ),
                        replyCount = 2,
                        bookId = 7,
                        bookTitle = "역병",
                        bookCoverImageUrl = "cover-7",
                    ),
                ),
            ),
            readingBook = ReadingBookDto(
                bookId = 7,
                isbn13 = "9780000000007",
                title = "역병",
                coverImageUrl = "cover-7",
                totalPages = 320,
                currentPage = 132,
            ),
        )

        val review = assertIs<FeedSection.RecentQuotes>(feed.visibleSections().single()).cards.single()
        assertEquals("7-2026-08-20T01:29:34Z", review.noteId.value)
        assertEquals("다정한 참새", review.authorLabel)
        assertEquals("https://example.com/profile.jpg", review.authorProfileImageUrl)
        assertEquals(Instant.parse("2026-08-20T01:29:34Z"), review.createdAt)
        assertEquals("오래 멈춰 읽었다.", review.quoteText)
        assertEquals("7", feed.readingBook?.bookId?.value)
        assertEquals(132, feed.readingBook?.currentPage)
    }
}
