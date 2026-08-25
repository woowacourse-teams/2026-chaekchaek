package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchPage
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

class BookSearchRemoteRepository(
    private val client: HttpClient = createHttpClient(),
) : BookSearchRepository {
    override suspend fun search(query: String, sort: BookSearchSort, page: Int): BookSearchPage =
        client.get("https://api.chaekchaek.com/api/v1/books") {
            parameter("query", query)
            parameter("sort", sort.name)
            parameter("page", page)
        }.body<BookSearchResponseDto>().toSearchPage()
}

@Serializable
internal data class BookSearchResponseDto(
    val totalCount: Int,
    val nextPage: Int? = null,
    val items: List<BookSearchItemDto>,
)

@Serializable
internal data class BookSearchItemDto(
    val title: String,
    val coverImageUrl: String,
    val authors: List<String>,
    val translators: List<String>,
    val publishedDate: String,
    val isbn13: String,
    val category: String,
    val publisher: String,
)

internal fun BookSearchItemDto.toSearchResult(): BookSearchResult =
    BookSearchResult(
        title = title,
        creator = (authors + translators.map { "$it 옮김" }).joinToString(" · "),
        publisher = publisher,
        year = publishedDate.take(4),
        coverUrl = coverImageUrl,
        isbn13 = isbn13,
        category = category.substringAfterLast('>'),
    )

private fun BookSearchResponseDto.toSearchPage(): BookSearchPage =
    BookSearchPage(
        totalCount = totalCount,
        nextPage = nextPage,
        items = items.map(BookSearchItemDto::toSearchResult),
    )
