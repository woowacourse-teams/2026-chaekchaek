package com.chaekchaek.app.domain.feed

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.note.NoteId
import com.chaekchaek.app.fixture.FIXED_INSTANT
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

private fun trendingBook(id: String = "bk_001") = TrendingBook(
    bookId = BookId(id),
    title = "보이지 않는 도시",
    coverId = "cover-01",
    noteCount = 128,
    replyCount = 46,
)

private fun quoteCard(id: String = "nt_1001") = QuoteCard(
    noteId = NoteId(id),
    bookId = BookId("bk_001"),
    bookTitle = "보이지 않는 도시",
    coverId = "cover-01",
    authorLabel = "김여름의 서재",
    authorProfileImageUrl = null,
    createdAt = FIXED_INSTANT,
    quoteText = "도시는 기억으로 만들어진다는 문장에서 오래 멈췄다.",
    replyCount = 12,
)

class HomeFeedTest {
    @Test
    fun `받은 순서대로 섹션을 돌려준다`() {
        // given : 서버가 인기 책, 문장, 겹친 책 순서로 내려줬다
        val feed = HomeFeed(
            listOf(
                FeedSection.TrendingBooks(listOf(trendingBook()), totalCount = 12),
                FeedSection.RecentQuotes(listOf(quoteCard())),
            ),
        )

        // when : 그릴 섹션을 물으면
        val sections = feed.visibleSections()

        // then : 받은 순서가 유지된다
        sections[0].shouldBeInstanceOf<FeedSection.TrendingBooks>()
        sections[1].shouldBeInstanceOf<FeedSection.RecentQuotes>()
    }

    @Test
    fun `내용이 없는 섹션은 그리지 않는다`() {
        // given : 문장 섹션이 비어 있는 피드가 주어진다
        val feed = HomeFeed(
            listOf(
                FeedSection.TrendingBooks(listOf(trendingBook()), totalCount = 12),
                FeedSection.RecentQuotes(emptyList()),
                FeedSection.OverlappedBooks(emptyList()),
            ),
        )

        // when : 그릴 섹션을 물으면
        val sections = feed.visibleSections()

        // then : 내용이 있는 하나만 남는다
        sections.size shouldBe 1
    }

    @Test
    fun `모든 섹션이 비어 있으면 빈 피드다`() {
        // given : 섹션은 있지만 내용이 전부 없는 피드가 주어진다
        val feed = HomeFeed(
            listOf(
                FeedSection.TrendingBooks(emptyList(), totalCount = 0),
                FeedSection.RecentQuotes(emptyList()),
            ),
        )

        // when & then : 빈 피드로 판정된다
        feed.isEmpty() shouldBe true
    }

    @Test
    fun `섹션이 아예 없어도 빈 피드다`() {
        // given & when & then : 서버가 모르는 타입만 내려줘 전부 걸러진 경우다
        HomeFeed(emptyList()).isEmpty() shouldBe true
    }
}
