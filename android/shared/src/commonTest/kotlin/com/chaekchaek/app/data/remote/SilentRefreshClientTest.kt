package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SilentRefreshClientTest {
    private fun client(
        refreshAccessToken: suspend () -> String?,
        engine: MockEngine,
    ): HttpClient = HttpClient(engine) {
        expectSuccess = true
    }.installSilentRefresh(refreshAccessToken)

    @Test
    fun `401 수신 시 토큰을 갱신하고 원래 요청을 재전송하여 성공한다`() = runTest {
        var refreshCount = 0
        val requestedTokens = mutableListOf<String?>()
        val engine = MockEngine { request ->
            val token = request.headers[HttpHeaders.Authorization]
            requestedTokens.add(token)
            if (token == "Bearer expired-token") {
                respond("Unauthorized", HttpStatusCode.Unauthorized)
            } else if (token == "Bearer refreshed-token") {
                respond("OK: fresh", HttpStatusCode.OK)
            } else {
                respond("Bad Request", HttpStatusCode.BadRequest)
            }
        }

        val testClient = client(
            refreshAccessToken = {
                refreshCount++
                "refreshed-token"
            },
            engine = engine,
        )

        val response = testClient.get("https://api.example.com/data") {
            header(HttpHeaders.Authorization, "Bearer expired-token")
        }

        assertEquals("OK: fresh", response.bodyAsText())
        assertEquals(1, refreshCount)
        assertEquals(listOf<String?>("Bearer expired-token", "Bearer refreshed-token"), requestedTokens)
    }

    @Test
    fun `인증 엔드포인트에서 401 수신 시 인터셉터가 개입하지 않고 즉시 401을 반환한다`() = runTest {
        var refreshCount = 0
        val engine = MockEngine {
            respond("Unauthorized", HttpStatusCode.Unauthorized)
        }

        val testClient = client(
            refreshAccessToken = {
                refreshCount++
                "refreshed-token"
            },
            engine = engine,
        )

        assertFailsWith<ClientRequestException> {
            testClient.get("https://api.example.com/api/v1/auth/mobile/reissue") {
                header(HttpHeaders.Authorization, "Bearer expired-token")
            }.bodyAsText()
        }
        assertEquals(0, refreshCount)
    }

    @Test
    fun `갱신된 토큰으로도 401이 오면 재시도를 1회로 제한하고 실패한다`() = runTest {
        var refreshCount = 0
        val engine = MockEngine {
            respond("Unauthorized", HttpStatusCode.Unauthorized)
        }

        val testClient = client(
            refreshAccessToken = {
                refreshCount++
                "still-invalid-token"
            },
            engine = engine,
        )

        assertFailsWith<ClientRequestException> {
            testClient.get("https://api.example.com/data") {
                header(HttpHeaders.Authorization, "Bearer expired-token")
            }.bodyAsText()
        }
        assertEquals(1, refreshCount)
    }

    @Test
    fun `Refresh 토큰이 폐기되어 갱신에 실패하면 401 오류를 전달한다`() = runTest {
        var refreshCount = 0
        val engine = MockEngine {
            respond("Unauthorized", HttpStatusCode.Unauthorized)
        }

        val testClient = client(
            refreshAccessToken = {
                refreshCount++
                null
            },
            engine = engine,
        )

        assertFailsWith<ClientRequestException> {
            testClient.get("https://api.example.com/data") {
                header(HttpHeaders.Authorization, "Bearer expired-token")
            }.bodyAsText()
        }
        assertEquals(1, refreshCount)
    }

    @Test
    fun `다중 동시 401 요청이 발생해도 갱신 함수는 1회만 호출되고 새 토큰을 공유한다`() = runTest {
        var refreshCount = 0
        val refreshStarted = CompletableDeferred<Unit>()
        val allowRefreshToFinish = CompletableDeferred<Unit>()
        val engine = MockEngine { request ->
            val token = request.headers[HttpHeaders.Authorization]
            if (token == "Bearer expired-token") {
                respond("Unauthorized", HttpStatusCode.Unauthorized)
            } else if (token == "Bearer refreshed-token") {
                respond("OK: path=${request.url.encodedPath}", HttpStatusCode.OK)
            } else {
                respond("Bad Request", HttpStatusCode.BadRequest)
            }
        }

        // AuthSession의 단일 비행(Single Flight) 갱신 로직을 모사
        var inFlightRefresh: kotlinx.coroutines.Deferred<String?>? = null
        val refreshMutex = kotlinx.coroutines.sync.Mutex()
        suspend fun singleFlightRefresh(): String? {
            val deferred = refreshMutex.withLock {
                inFlightRefresh?.takeUnless { it.isCompleted } ?: async {
                    refreshCount++
                    refreshStarted.complete(Unit)
                    allowRefreshToFinish.await()
                    "refreshed-token"
                }.also { inFlightRefresh = it }
            }
            return try {
                deferred.await()
            } finally {
                refreshMutex.withLock {
                    if (inFlightRefresh === deferred && deferred.isCompleted) inFlightRefresh = null
                }
            }
        }

        val testClient = client(
            refreshAccessToken = ::singleFlightRefresh,
            engine = engine,
        )

        val first = async {
            testClient.get("https://api.example.com/first") {
                header(HttpHeaders.Authorization, "Bearer expired-token")
            }.bodyAsText()
        }
        val second = async {
            testClient.get("https://api.example.com/second") {
                header(HttpHeaders.Authorization, "Bearer expired-token")
            }.bodyAsText()
        }

        refreshStarted.await()
        allowRefreshToFinish.complete(Unit)

        assertEquals("OK: path=/first", first.await())
        assertEquals("OK: path=/second", second.await())
        assertEquals(1, refreshCount)
    }
}
