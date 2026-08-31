package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.MemberRemoteRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
class MemberSettingsViewModelTest {
    @Test
    fun authenticateLoadsMemberSettingsAndLogoutClearsThem() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val requests = mutableListOf<HttpRequestData>()
        val client = testClient { request ->
            requests += request
            respondJson("""{"memberId":9,"nickname":"서버 이름","anonymousNickname":"우아한 달빛 참새","displayAnonymous":false}""")
        }

        try {
            val viewModel = MemberSettingsViewModel(MemberRemoteRepository(client))

            viewModel.authenticate("access-token")
            viewModel.uiState.first { it.nickname == "서버 이름" }

            assertEquals(true, viewModel.uiState.value.signedIn)
            assertEquals(false, viewModel.uiState.value.anonymousReviews)
            assertEquals("우아한 달빛 참새", viewModel.uiState.value.anonymousNickname)
            assertEquals("Bearer access-token", requests.single().headers[HttpHeaders.Authorization])

            viewModel.authenticate(null)

            assertEquals(MemberSettingsUiState(), viewModel.uiState.value)
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun existingNicknameOnlyUpdatesAnonymityWhenRevealingName() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val requests = mutableListOf<HttpRequestData>()
        val client = testClient { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/v1/members/me" ->
                    respondJson("""{"memberId":9,"nickname":"기존 이름","anonymousNickname":"우아한 달빛 참새","displayAnonymous":true}""")
                "/api/v1/members/me/anonymity" ->
                    respondJson("""{"memberId":9,"nickname":"기존 이름","anonymousNickname":"우아한 달빛 참새","displayAnonymous":false}""")
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        try {
            val viewModel = MemberSettingsViewModel(MemberRemoteRepository(client))
            viewModel.authenticate("access-token")
            viewModel.uiState.first { it.nickname == "기존 이름" }
            requests.clear()

            viewModel.setAnonymousReviews(false, "기존 이름")
            viewModel.uiState.first { !it.anonymousReviews }

            assertEquals(listOf("/api/v1/members/me/anonymity"), requests.map { it.url.encodedPath })
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun partialSuccessIsAppliedAndRetrySendsOnlyIncompleteSetting() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val requests = mutableListOf<HttpRequestData>()
        var anonymityRequests = 0
        val client = testClient { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/v1/members/me" ->
                    respondJson("""{"memberId":9,"nickname":"이전 이름","anonymousNickname":"우아한 달빛 참새","displayAnonymous":true}""")
                "/api/v1/members/me/nickname" ->
                    respondJson("""{"memberId":9,"nickname":"새 이름","anonymousNickname":"우아한 달빛 참새","displayAnonymous":true}""")
                "/api/v1/members/me/anonymity" -> {
                    anonymityRequests += 1
                    if (anonymityRequests == 1) {
                        respondJson("""{"code":"SERVER_ERROR"}""", HttpStatusCode.InternalServerError)
                    } else {
                        respondJson("""{"memberId":9,"nickname":"새 이름","anonymousNickname":"우아한 달빛 참새","displayAnonymous":false}""")
                    }
                }
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        }

        try {
            val viewModel = MemberSettingsViewModel(MemberRemoteRepository(client))
            viewModel.authenticate("access-token")
            viewModel.uiState.first { it.nickname == "이전 이름" }
            requests.clear()

            viewModel.setAnonymousReviews(false, " 새 이름 ")
            viewModel.uiState.first { it.errorMessage != null }

            assertEquals("새 이름", viewModel.uiState.value.nickname)
            assertEquals(true, viewModel.uiState.value.anonymousReviews)
            assertEquals(
                listOf("/api/v1/members/me/nickname", "/api/v1/members/me/anonymity"),
                requests.map { it.url.encodedPath },
            )
            assertEquals(Json.parseToJsonElement("""{"nickname":"새 이름"}"""), requests[0].jsonBody())
            requests.clear()

            viewModel.clearError()
            viewModel.retry()
            viewModel.uiState.first { !it.anonymousReviews }

            assertEquals(listOf("/api/v1/members/me/anonymity"), requests.map { it.url.encodedPath })
            assertEquals(Json.parseToJsonElement("""{"displayAnonymous":false}"""), requests.single().jsonBody())
            assertEquals(null, viewModel.uiState.value.errorMessage)
        } finally {
            client.close()
            Dispatchers.resetMain()
        }
    }

    private fun testClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
        HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
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
