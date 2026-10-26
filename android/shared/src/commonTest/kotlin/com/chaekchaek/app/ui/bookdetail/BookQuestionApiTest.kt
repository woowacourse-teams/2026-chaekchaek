package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.data.remote.ApiConfiguration
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
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
import kotlin.test.assertNull

class BookQuestionApiTest {
    @Test
    fun `실제 상세 조회 응답의 질문과 마크다운 자료를 화면 모델까지 전달한다`() = runTest {
        val requested = mutableListOf<String>()
        val client = client(""", "bookQuestion":{"question":"**질문**","plot":"## 줄거리","extract":"> 발췌"}""", requested)
        try {
            val detail = BookDetailRemoteRepository(client, ApiConfiguration("https://dev-api.chaekchaek.com"))
                .detail("9788932923918")
            assertEquals(listOf("https://dev-api.chaekchaek.com/api/v1/books/by-isbn/9788932923918"), requested)
            assertEquals(BookQuestionUiModel("9788932923918", "**질문**", "## 줄거리", "> 발췌"), detail.bookQuestion?.toUiModel(detail.isbn13))
        } finally { client.close() }
    }

    @Test
    fun `이전 서버와 null 빈 질문 응답은 질문 영역을 만들지 않는다`() = runTest {
        for (field in listOf("", """, "bookQuestion":null""", """, "bookQuestion":{}""",
            """, "bookQuestion":{"question":null,"plot":"자료"}""",
            """, "bookQuestion":{"question":"  ","extract":"자료"}""")) {
            val client = client(field)
            try {
                assertNull(BookDetailRemoteRepository(client).detail("9788932923918").bookQuestion, field)
            } finally { client.close() }
        }
    }

    @Test
    fun `누락 null 공백 자료는 빈 탭을 만들지 않고 있는 자료만 표시한다`() = runTest {
        val cases = listOf(
            "" to emptyList(),
            """, "plot":null,"extract":"  """" to emptyList(),
            """, "plot":"줄거리"""" to listOf(BookQuestionReferenceTab.Synopsis),
            """, "plot":"  ","extract":"발췌"""" to listOf(BookQuestionReferenceTab.Excerpt),
        )
        for ((material, tabs) in cases) {
            val client = client(""", "bookQuestion":{"question":"질문"$material}""")
            try {
                val question = BookDetailRemoteRepository(client).detail("9788932923918").bookQuestion!!
                assertEquals(tabs, question.toUiModel("9788932923918").referenceTabs, material)
            } finally { client.close() }
        }
    }

    private fun client(questionField: String, requested: MutableList<String> = mutableListOf()) = HttpClient(MockEngine { request ->
        requested += request.url.toString()
        respond("""{"isbn13":"9788932923918","title":"마음","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":""$questionField}""",
            headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
}
