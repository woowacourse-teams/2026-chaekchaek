package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModelTest {
    @Test
    fun memberSettingsCallServerAndOnlyApplySuccessfulResponses() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val requests = mutableListOf<HttpRequestData>()
        var nicknameRequests = 0
        val client = HttpClient(MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/v1/library" -> respondJson("""{"nextPage":null,"items":[]}""")
                "/api/v1/members/me/nickname" -> {
                    nicknameRequests += 1
                    if (nicknameRequests == 1) {
                        respondJson("""{"memberId":9,"nickname":"새 이름","displayAnonymous":true}""")
                    } else {
                        respondJson("""{"code":"NICKNAME_ALREADY_EXISTS"}""", HttpStatusCode.Conflict)
                    }
                }
                "/api/v1/members/me/anonymity" -> {
                    val anonymous = request.jsonBody() == Json.parseToJsonElement("""{"displayAnonymous":true}""")
                    respondJson("""{"memberId":9,"nickname":"새 이름","displayAnonymous":$anonymous}""")
                }
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        try {
            val viewModel = ArchiveViewModel(LibraryRemoteRepository(client))
            viewModel.authenticate("access-token")
            viewModel.setAnonymousReviews(false, " 새 이름 ")
            viewModel.uiState.first { !it.anonymousReviews }

            val memberRequests = requests.filter { it.url.encodedPath.startsWith("/api/v1/members/me/") }

            assertEquals(
                listOf(
                    HttpMethod.Patch to "/api/v1/members/me/nickname",
                    HttpMethod.Patch to "/api/v1/members/me/anonymity",
                ),
                memberRequests.map { it.method to it.url.encodedPath },
            )
            assertEquals(List(2) { "Bearer access-token" }, memberRequests.map { it.headers[HttpHeaders.Authorization] })
            assertEquals(Json.parseToJsonElement("""{"nickname":"새 이름"}"""), memberRequests[0].jsonBody())
            assertEquals(Json.parseToJsonElement("""{"displayAnonymous":false}"""), memberRequests[1].jsonBody())
            assertEquals(false, viewModel.uiState.value.anonymousReviews)
            assertEquals("새 이름", viewModel.uiState.value.nickname)

            requests.clear()
            viewModel.setAnonymousReviews(true)
            viewModel.uiState.first { it.anonymousReviews }

            assertEquals(listOf("/api/v1/members/me/anonymity"), requests.map { it.url.encodedPath })
            assertEquals(Json.parseToJsonElement("""{"displayAnonymous":true}"""), requests.single().jsonBody())
            assertEquals(true, viewModel.uiState.value.anonymousReviews)

            requests.clear()
            viewModel.setAnonymousReviews(false, "중복")
            viewModel.uiState.first { it.errorMessage != null }

            assertEquals(listOf("/api/v1/members/me/nickname"), requests.map { it.url.encodedPath })
            assertEquals(true, viewModel.uiState.value.anonymousReviews)
            assertEquals("새 이름", viewModel.uiState.value.nickname)
            assertEquals("설정을 변경하지 못했어요", viewModel.uiState.value.errorMessage)
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }

    private fun MockRequestHandleScope.respondJson(content: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(
            content = content,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    private fun HttpRequestData.jsonBody() =
        Json.parseToJsonElement((body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
}
