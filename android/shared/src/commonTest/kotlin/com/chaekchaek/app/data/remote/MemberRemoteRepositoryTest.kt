package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MemberRemoteRepositoryTest {
    @Test
    fun memberSettingsUseAuthenticatedMemberEndpoints() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val client = HttpClient(MockEngine { request ->
            requests += request
            respond(
                content = """{"memberId":9,"nickname":"책책이","anonymousNickname":"우아한 달빛 참새","displayAnonymous":false}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val repository = MemberRemoteRepository(client)

        val profile = repository.get("access-token")
        assertEquals(9L, profile.memberId)
        assertEquals("우아한 달빛 참새", profile.anonymousNickname)
        repository.updateNickname("새 이름", "access-token")
        repository.updateAnonymity(true, "access-token")

        assertEquals(
            listOf(
                HttpMethod.Get to "/api/v1/members/me",
                HttpMethod.Patch to "/api/v1/members/me/nickname",
                HttpMethod.Patch to "/api/v1/members/me/anonymity",
            ),
            requests.map { it.method to it.url.encodedPath },
        )
        assertEquals(List(3) { "Bearer access-token" }, requests.map { it.headers[HttpHeaders.Authorization] })
        assertEquals(Json.parseToJsonElement("""{"nickname":"새 이름"}"""), requests[1].jsonBody())
        assertEquals(Json.parseToJsonElement("""{"displayAnonymous":true}"""), requests[2].jsonBody())
    }

    @Test
    fun memberSettingsRequireAnonymousNickname() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """{"memberId":9,"nickname":"책책이","displayAnonymous":false}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        assertFails { MemberRemoteRepository(client).get("access-token") }
    }

    private fun HttpRequestData.jsonBody() =
        Json.parseToJsonElement((body as OutgoingContent.ByteArrayContent).bytes().decodeToString())
}
