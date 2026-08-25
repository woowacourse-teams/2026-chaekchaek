package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.book.BookSearchSort
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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

    @Test
    fun `requested page and next page are preserved`() = runTest {
        var requestedPage: String? = null
        val client = HttpClient(MockEngine { request ->
            requestedPage = request.url.parameters["page"]
            respond(
                content = """{"totalCount":12,"nextPage":3,"items":[]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val page = BookSearchRemoteRepository(client).search("책", BookSearchSort.LATEST, page = 2)

        assertEquals("2", requestedPage)
        assertEquals(12, page.totalCount)
        assertEquals(3, page.nextPage)
    }
}
