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
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode

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

        assertEquals("앤디 위어  박아람 옮김", result.creator)
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
                content = response(12, 3, item("책", "9788937460449", "2015-07-24")),
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

    @Test
    fun `mixed search response accepts null missing and blank dates without losing books`() = runTest {
        val client = searchClient { response(4, null,
            item("호밀밭의 파수꾼", "9788937460449", "2001-05-30"),
            item("출판일 null", "9788936433598", null),
            item("출판일 누락", "9788925568683", null).replace(",\"publishedDate\":null", ""),
            item("출판일 빈 값", "9788937473135", ""),
        ) }
        val result = BookSearchRemoteRepository(client).search("호밀", BookSearchSort.LATEST, 1)

        assertEquals(listOf("2001", "", "", ""), result.items.map { it.year })
        assertEquals(4, result.items.size)
    }

    @Test
    fun `search excludes empty whitespace null and missing ISBNs before displaying products`() = runTest {
        val client = searchClient { response(5, null,
            item("미국을 대표하는 문학 고전 세트", "", null),
            item("공백 ISBN", "   ", "2026-01-01"),
            item("null ISBN", null, null),
            item("ISBN 누락", null, null).replace(",\"isbn13\":null", ""),
            item("호밀밭의 파수꾼", "9788937460449", null),
        ) }
        val result = BookSearchRemoteRepository(client).search("호밀밭", BookSearchSort.LATEST, 1)

        assertEquals(listOf("호밀밭의 파수꾼"), result.items.map { it.title })
        assertEquals("", result.items.single().year)
        assertEquals(5, result.totalCount) // 서버의 전체 결과 수는 클라이언트가 재계산할 수 없다.
    }

    @Test
    fun `filtered pages continue to the next server page until a selectable book is found`() = runTest {
        val requests = mutableListOf<Int>()
        val client = searchClient { page ->
            requests += page
            when (page) {
                1 -> response(12, 3, item("세트", "", null))
                3 -> response(12, 5, item("다른 세트", "", null))
                else -> response(12, 7, item("단행본", "9788937460449", null))
            }
        }
        val result = BookSearchRemoteRepository(client).search("책", BookSearchSort.LATEST, 1)

        assertEquals(listOf(1, 3, 5), requests)
        assertEquals("단행본", result.items.single().title)
        assertEquals(7, result.nextPage)
        assertEquals(5, result.lastLoadedPage)
    }

    @Test
    fun `only excluded products return empty after the last server page`() = runTest {
        val requests = mutableListOf<Int>()
        val client = searchClient { page ->
            requests += page
            response(2, if (page == 1) 2 else null, item("세트", "", null))
        }
        val result = BookSearchRemoteRepository(client).search("세트", BookSearchSort.LATEST, 1)

        assertEquals(listOf(1, 2), requests)
        assertTrue(result.items.isEmpty())
        assertEquals(null, result.nextPage)
    }

    @Test
    fun `failure after a filtered page is propagated instead of returning empty`() = runTest {
        val client = HttpClient(MockEngine { request ->
            if (request.url.parameters["page"] == "1") {
                respond(response(2, 2, item("세트", "", null)),
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else respond("실패", HttpStatusCode.InternalServerError)
        }) {
            expectSuccess = true
            install(ContentNegotiation) { json() }
        }
        assertFailsWith<ServerResponseException> {
            BookSearchRemoteRepository(client).search("책", BookSearchSort.LATEST, 1)
        }
    }

    @Test
    fun `single page query preserves an empty filtered page for bounded restoration`() = runTest {
        val requests = mutableListOf<Int>()
        val client = searchClient { page ->
            requests += page
            check(page == 2) { "다음 페이지를 요청하면 안 됩니다" }
            response(3, 3, item("ISBN 없는 세트", "", null))
        }
        val result = BookSearchRemoteRepository(client).searchPage("책", BookSearchSort.LATEST, 2)
        assertTrue(result.items.isEmpty())
        assertEquals(3, result.nextPage)
        assertEquals(2, result.lastLoadedPage)
        assertEquals(listOf(2), requests)
    }

    @Test
    fun `bookId 없는 검색 결과를 제외하고 다음 페이지에서 유효한 책을 찾는다`() = runTest {
        val requests = mutableListOf<Int>()
        val client = searchClient { page ->
            requests += page
            if (page == 1) response(3, 2,
                item("ID null", "9788937460449", null).replace("\"bookId\":42", "\"bookId\":null"),
                item("ID 누락", "9788936433598", null).replace("\"bookId\":42,", ""))
            else response(3, null, item("정상 도서", "9788925568683", null))
        }
        try {
            val result = BookSearchRemoteRepository(client).search("책", BookSearchSort.LATEST, 1)
            assertEquals(listOf(1, 2), requests)
            assertEquals(listOf("정상 도서"), result.items.map { it.title })
            assertEquals(3, result.totalCount)
        } finally { client.close() }
    }

    private fun searchClient(body: (Int) -> String) = HttpClient(MockEngine { request ->
        respond(body(request.url.parameters["page"]!!.toInt()),
            headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        install(ContentNegotiation) { json() }
    }

    private fun response(total: Int, next: Int?, vararg items: String) =
        """{"totalCount":$total,"nextPage":$next,"items":[${items.joinToString(",")}]}"""

    private fun item(title: String, isbn: String?, date: String?) =
        """{"bookId":42,"title":"$title","coverImageUrl":"cover","authors":["저자"],"translators":[],"publisher":"출판사","category":"소설","isbn13":${isbn?.let { "\"$it\"" }},"publishedDate":${date?.let { "\"$it\"" }}}"""

}
