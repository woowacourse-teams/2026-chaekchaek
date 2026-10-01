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
                        isbn13 = "9780000000007",
                        bookTitle = "역병",
                        bookCoverImageUrl = "cover-7",
                        quote = "페스트균은 결코 죽거나 사라지지 않는다.",
                        likeCount = 2,
                        likedByMe = true,
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
        assertEquals("9780000000007", review.isbn13)
        assertEquals("다정한 참새", review.authorLabel)
        assertEquals("https://example.com/profile.jpg", review.authorProfileImageUrl)
        assertEquals(Instant.parse("2026-08-20T01:29:34Z"), review.createdAt)
        assertEquals("오래 멈춰 읽었다.", review.quoteText)
        assertEquals("페스트균은 결코 죽거나 사라지지 않는다.", review.quote)
        assertEquals(2, review.likeCount)
        assertEquals(true, review.likedByMe)
        assertEquals("7", feed.readingBook?.bookId?.value)
        assertEquals(132, feed.readingBook?.currentPage)
    }

    @Test
    fun `도서별 감상 상세에서 일치하는 최신 감상의 인용문과 반응을 보강한다`() {
        val latestReviews = LatestReviewsResponseDto(
            reviews = listOf(
                LatestReviewDto(
                    content = "정말 잔인한 말",
                    createdAt = "2026-09-30T14:46:04.333587Z",
                    author = LatestReviewAuthorDto(displayName = "낭눈"),
                    replyCount = 0,
                    bookId = 95,
                    bookTitle = "사랑의 편린들",
                    bookCoverImageUrl = "cover-95",
                ),
                LatestReviewDto(
                    content = "인용문이 없는 감상",
                    createdAt = "2026-09-30T14:45:00Z",
                    author = LatestReviewAuthorDto(displayName = "참새"),
                    replyCount = 0,
                    bookId = 95,
                    bookTitle = "사랑의 편린들",
                    bookCoverImageUrl = "cover-95",
                ),
            ),
        )

        val enriched = latestReviews.withReviewDetails(
            reviewDetailsByBook = mapOf(
                95L to listOf(
                    ReviewDto(
                        reviewId = 241,
                        content = "정말 잔인한 말",
                        quote = "우리의 추억 속의 나는, 영원히 널 사랑할 거야.",
                        createdAt = "2026-09-30T14:46:04.333587Z",
                        author = ReviewAuthorDto(
                            displayName = "낭눈",
                            anonymous = false,
                            mine = false,
                            actorType = "MEMBER",
                        ),
                        replyCount = 1,
                        likeCount = 2,
                        likedByMe = true,
                        deleted = false,
                    ),
                ),
            ),
        )

        val quotedReview = enriched.reviews.first()
        assertEquals("우리의 추억 속의 나는, 영원히 널 사랑할 거야.", quotedReview.quote)
        assertEquals(2, quotedReview.likeCount)
        assertEquals(true, quotedReview.likedByMe)
        assertEquals(1, quotedReview.replyCount)

        val reviewWithoutQuote = enriched.reviews.last()
        assertEquals(null, reviewWithoutQuote.quote)
        assertEquals(0, reviewWithoutQuote.likeCount)
    }
}
