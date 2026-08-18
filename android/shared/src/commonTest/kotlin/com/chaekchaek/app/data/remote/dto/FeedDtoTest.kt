package com.chaekchaek.app.data.remote.dto

import com.chaekchaek.app.domain.feed.FeedSection
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlin.time.Instant

private fun quoteDto(noteId: String = "nt_1001") = QuoteCardDto(
    noteId = noteId,
    bookId = "bk_001",
    bookTitle = "보이지 않는 도시",
    coverId = "cover-01",
    authorLabel = "김여름의 서재",
    createdAt = "2026-08-05T09:00:00Z",
    quoteText = "도시는 기억으로 만들어진다는 문장에서 오래 멈췄다.",
    replyCount = 12,
)

class FeedDtoMappingTest {
    @Test
    fun `아는 섹션 타입을 도메인으로 바꾼다`() {
        // given : 서버가 세 종류 섹션을 내려줬다
        val response = HomeFeedResponse(
            sections = listOf(
                FeedSectionDto(
                    type = "TRENDING_BOOKS",
                    totalCount = 12,
                    books = listOf(
                        TrendingBookDto("bk_001", "보이지 않는 도시", "cover-01", 128, 46),
                    ),
                ),
                FeedSectionDto(type = "RECENT_QUOTES", quotes = listOf(quoteDto())),
            ),
        )

        // when : 도메인으로 바꾸면
        val sections = response.toDomain().visibleSections()

        // then : 두 섹션이 순서대로 만들어진다
        sections.size shouldBe 2
        sections[0].shouldBeInstanceOf<FeedSection.TrendingBooks>()
        sections[1].shouldBeInstanceOf<FeedSection.RecentQuotes>()
    }

    @Test
    fun `모르는 섹션 타입은 무시한다`() {
        // given : 서버가 앱이 모르는 새 섹션을 함께 내려줬다
        val response = HomeFeedResponse(
            sections = listOf(
                FeedSectionDto(type = "RECENT_QUOTES", quotes = listOf(quoteDto())),
                FeedSectionDto(type = "READING_CHALLENGE_2027"),
            ),
        )

        // when : 도메인으로 바꾸면
        val sections = response.toDomain().visibleSections()

        // then : 아는 섹션만 남고 오류가 나지 않는다 (구버전 앱이 죽지 않아야 한다)
        sections.size shouldBe 1
        sections[0].shouldBeInstanceOf<FeedSection.RecentQuotes>()
    }

    @Test
    fun `모르는 타입만 오면 빈 피드가 된다`() {
        // given : 앱이 아는 섹션이 하나도 없다
        val response = HomeFeedResponse(
            sections = listOf(FeedSectionDto(type = "READING_CHALLENGE_2027")),
        )

        // when & then : 빈 피드로 판정되고 예외가 나지 않는다
        response.toDomain().isEmpty() shouldBe true
    }

    @Test
    fun `시각 문자열을 Instant 로 바꾼다`() {
        // given : ISO-8601 문자열이 담긴 응답이 주어진다
        val response = HomeFeedResponse(
            sections = listOf(FeedSectionDto(type = "RECENT_QUOTES", quotes = listOf(quoteDto()))),
        )

        // when : 도메인으로 바꾸면
        val section = response.toDomain().visibleSections()
            .first() as FeedSection.RecentQuotes

        // then : Instant 로 파싱된다 (표시용 문자열은 presentation 이 만든다)
        section.cards.first().createdAt shouldBe Instant.parse("2026-08-05T09:00:00Z")
    }

    @Test
    fun `식별자를 도메인 타입으로 감싼다`() {
        // given : 문자열 식별자가 담긴 응답이 주어진다
        val response = HomeFeedResponse(
            sections = listOf(FeedSectionDto(type = "RECENT_QUOTES", quotes = listOf(quoteDto()))),
        )

        // when : 도메인으로 바꾸면
        val section = response.toDomain().visibleSections()
            .first() as FeedSection.RecentQuotes

        // then : NoteId 와 BookId 로 감싸져 자리를 바꿔 쓸 수 없게 된다
        section.cards.first().noteId.value shouldBe "nt_1001"
        section.cards.first().bookId.value shouldBe "bk_001"
    }
}
