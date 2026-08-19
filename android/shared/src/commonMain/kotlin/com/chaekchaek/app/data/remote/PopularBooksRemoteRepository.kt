package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.FeedSection
import com.chaekchaek.app.domain.feed.HomeFeed
import com.chaekchaek.app.domain.feed.TrendingBook
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

class PopularBooksRemoteRepository(
    private val client: HttpClient = createHttpClient(),
) : FeedRepository {
    override suspend fun homeFeed(): HomeFeed =
        client.get("https://api.chaekchaek.com/api/v1/home/popular-books")
            .body<PopularBooksResponseDto>()
            .toHomeFeed()
}

@Serializable
internal data class PopularBooksResponseDto(
    val books: List<PopularBookDto>,
)

@Serializable
internal data class PopularBookDto(
    val bookId: Long,
    val title: String,
    val coverImageUrl: String,
    val reviewCount: Int,
    val replyCount: Int,
)

internal fun PopularBooksResponseDto.toHomeFeed(): HomeFeed = HomeFeed(
    sections = listOf(
        FeedSection.TrendingBooks(
            books = books.map { book ->
                TrendingBook(
                    bookId = BookId(book.bookId.toString()),
                    title = book.title,
                    coverId = book.coverImageUrl,
                    noteCount = book.reviewCount,
                    replyCount = book.replyCount,
                )
            },
            totalCount = books.size,
        ),
    ),
)
