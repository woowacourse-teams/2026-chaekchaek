package com.chaekchaek.app.data.fake

import com.chaekchaek.app.data.datasource.FeedDataSource
import com.chaekchaek.app.data.remote.dto.FeedSectionDto
import com.chaekchaek.app.data.remote.dto.HomeFeedResponse
import com.chaekchaek.app.data.remote.dto.OverlappedBookDto
import com.chaekchaek.app.data.remote.dto.QuoteCardDto
import com.chaekchaek.app.data.remote.dto.TrendingBookDto
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * 서버가 준비되기 전까지 쓰는 더미 피드. 문구와 수치는 Figma 홈 화면(36:1206)에서 가져왔다.
 *
 * 상대 시각(`4분 전`)이 시간이 지나도 그대로이면 안 되므로 문자열이 아니라 현재 시각 기준으로
 * 계산한 [kotlin.time.Instant] 를 내려준다. [clock] 을 주입받아 테스트에서 고정할 수 있다.
 */
@Inject
class FeedFakeDataSource(
    private val clock: Clock,
) : FeedDataSource {
    override suspend fun homeFeed(): HomeFeedResponse {
        delay(FAKE_DELAY_MILLIS)
        val now = clock.now()
        return HomeFeedResponse(
            sections = listOf(
                FeedSectionDto(
                    type = "TRENDING_BOOKS",
                    totalCount = 12,
                    books = trendingBooks(),
                ),
                FeedSectionDto(
                    type = "RECENT_QUOTES",
                    quotes = listOf(
                        QuoteCardDto(
                            noteId = "nt_1001",
                            bookId = "bk_001",
                            bookTitle = "보이지 않는 도시",
                            coverId = "cover-01",
                            authorLabel = "김여름의 서재",
                            createdAt = (now - 4.minutes).toString(),
                            quoteText = "도시는 기억으로 만들어진다는 문장에서 오래 멈췄다. " +
                                "떠난 장소도 읽는 동안은 다시 현재가 된다.",
                            replyCount = 12,
                        ),
                        QuoteCardDto(
                            noteId = "nt_1002",
                            bookId = "bk_003",
                            bookTitle = "마션",
                            coverId = "cover-03",
                            authorLabel = "정우의 서재",
                            createdAt = (now - 38.minutes).toString(),
                            quoteText = "나는 이 행성에서 과학으로 헤쳐 나갈 것이다.",
                            replyCount = 5,
                        ),
                        QuoteCardDto(
                            noteId = "nt_1003",
                            bookId = "bk_004",
                            bookTitle = "침묵하는 다수",
                            coverId = "cover-04",
                            authorLabel = "하늘의 서재",
                            createdAt = (now - 1.hours).toString(),
                            quoteText = "말하지 않기로 한 사람들의 자리를 오래 생각했다.",
                            replyCount = 3,
                        ),
                    ),
                ),
                FeedSectionDto(
                    type = "OVERLAPPED_BOOKS",
                    overlapped = listOf(
                        OverlappedBookDto(
                            bookId = "bk_002",
                            title = "역병",
                            coverId = "cover-02",
                            noteCount = 96,
                            authorLabel = "윤서의 서재",
                            createdAt = (now - 6.hours).toString(),
                            excerpt = "무너지는 세계에서 서로를 돌보는 일은 거창한 구원이 " +
                                "아니라 매일의 선택이었다.",
                            replyCount = 28,
                        ),
                    ),
                ),
            ),
        )
    }

    private fun trendingBooks(): List<TrendingBookDto> = listOf(
        TrendingBookDto("bk_001", "보이지 않는 도시", "cover-01", noteCount = 128, replyCount = 46),
        TrendingBookDto("bk_002", "역병", "cover-02", noteCount = 96, replyCount = 28),
        TrendingBookDto("bk_003", "마션", "cover-03", noteCount = 30, replyCount = 12),
        TrendingBookDto("bk_004", "침묵하는 다수", "cover-04", noteCount = 24, replyCount = 7),
        TrendingBookDto("bk_005", "장일장진", "cover-05", noteCount = 21, replyCount = 12),
        TrendingBookDto("bk_006", "여름의 문장들", "cover-06", noteCount = 18, replyCount = 4),
        TrendingBookDto("bk_007", "밤의 도서관", "cover-07", noteCount = 15, replyCount = 9),
        TrendingBookDto("bk_008", "그리고 아무도", "cover-08", noteCount = 14, replyCount = 3),
        TrendingBookDto("bk_009", "우리가 빛의 속도로", "cover-09", noteCount = 12, replyCount = 6),
        TrendingBookDto("bk_010", "작별하지 않는다", "cover-10", noteCount = 11, replyCount = 2),
        TrendingBookDto("bk_011", "천 개의 파랑", "cover-11", noteCount = 9, replyCount = 5),
        TrendingBookDto("bk_012", "구의 증명", "cover-12", noteCount = 8, replyCount = 1),
    )

    private companion object {
        /** 로딩 상태가 실제로 보이도록 흉내 낸다. 서버가 붙으면 사라진다. */
        const val FAKE_DELAY_MILLIS = 400L
    }
}
