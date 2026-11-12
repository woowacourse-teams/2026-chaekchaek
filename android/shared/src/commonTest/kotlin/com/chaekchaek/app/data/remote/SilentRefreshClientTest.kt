package com.chaekchaek.app.data.remote

import com.chaekchaek.app.auth.AuthSession
import com.chaekchaek.app.presentation.common.ErrorAction
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SilentRefreshClientTest {
    private fun client(
        refreshAccessToken: suspend () -> String?,
        engine: MockEngine,
    ): HttpClient = HttpClient(engine) {
        expectSuccess = true
    }.installSilentRefresh { AuthenticatedRequest(it, refreshAfterRejection = refreshAccessToken) }

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
    fun `첫 재발급이 끝난 뒤 늦은 401이 와도 동일한 갱신 토큰으로 재전송한다`() = runTest {
        val lateRequestSent = CompletableDeferred<Unit>()
        val allowLate401 = CompletableDeferred<Unit>()
        var refreshCount = 0
        val session = authSession {
            refreshCount++
            tokens("refreshed-token")
        }
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { request ->
                val token = request.headers[HttpHeaders.Authorization]
                if (token == "Bearer expired-token") {
                    if (request.url.encodedPath == "/late") {
                        lateRequestSent.complete(Unit)
                        allowLate401.await()
                    }
                    respond("Unauthorized", HttpStatusCode.Unauthorized)
                } else {
                    respond("OK: $token")
                }
            }
        })
        val client = HttpClient(engine) { expectSuccess = true }.installSilentRefresh(session::prepareRequest)

        try {
            val late = async {
                client.get("https://api.example.com/late") {
                    header(HttpHeaders.Authorization, "Bearer member-session")
                }.bodyAsText()
            }
            lateRequestSent.await()
            assertEquals("OK: Bearer refreshed-token", client.get("https://api.example.com/first") {
                header(HttpHeaders.Authorization, "Bearer member-session")
            }.bodyAsText())
            allowLate401.complete(Unit)

            assertEquals("OK: Bearer refreshed-token", late.await())
            assertEquals(1, refreshCount)
        } finally {
            client.close()
        }
    }

    @Test
    fun `갱신 연결 오류 후에도 같은 세션에서 다시 시도할 수 있다`() = runTest {
        var refreshCount = 0
        val session = authSession {
            if (++refreshCount == 1) throw IllegalStateException("connection lost")
            tokens("refreshed-token")
        }
        val first = session.prepareRequest("member-session")
        val concurrent = session.prepareRequest("member-session")

        assertFailsWith<IllegalStateException> { first.refreshAfterRejection() }
        assertFailsWith<IllegalStateException> { concurrent.refreshAfterRejection() }
        assertEquals(1, refreshCount)
        assertEquals("expired-token", session.credentials.value?.tokens?.accessToken)
        assertEquals("refreshed-token", session.prepareRequest("member-session").refreshAfterRejection())
        assertEquals(2, refreshCount)
    }

    @Test
    fun `이전 인증 세션의 지연 응답은 새 세션에서 재전송하지 않는다`() = runTest {
        val session = authSession { error("이전 세션의 갱신이 실행되면 안 됨") }
        val oldRequest = session.prepareRequest("member-session")

        session.signIn(tokens("another-access"))
        session.bindMemberRequestKey("another-session")

        assertFailsWith<CancellationException> { oldRequest.refreshAfterRejection() }
        assertEquals("another-access", session.credentials.value?.tokens?.accessToken)
    }

    @Test
    fun `복구 오류는 요청을 보낸 회원만 재시도하고 같은 회원의 새 로그인 뒤에는 재시도 안내를 사용한다`() = runTest {
        var renewals = 0
        val session = authSession {
            renewals++
            throw MobileLoginException("UNUSABLE_REFRESH_TOKEN", 401)
        }
        val generation = session.credentials.value?.generation
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler { respond("Unauthorized", HttpStatusCode.Unauthorized) }
        })
        val client = HttpClient(engine) { expectSuccess = true }.installSilentRefresh(session::prepareRequest)
        try {
            val unauthorized = assertFailsWith<ClientRequestException> {
                client.get("https://api.example.com/data") { header(HttpHeaders.Authorization, "Bearer member-session") }
            }
            val afterExpiry = assertFailsWith<MobileLoginException> { session.prepareRequest("member-session") }
            assertEquals(1, renewals)
            for (error in listOf(unauthorized, afterExpiry)) {
                val recovery = ErrorRecoveryState()
                recovery.report(error, ErrorOperation.Request) {}
                val notice = requireNotNull(recovery.notice.value)
                assertEquals(ErrorAction.Login, notice.forMember("member-session", generation)?.action)
                assertNull(notice.forMember("other-member", 2))
                assertEquals(ErrorAction.Retry, notice.forMember("member-session", 2)?.action)
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun `개발자 폐기가 실패해도 기다리던 보호 요청은 갱신을 공유한다`() = runTest {
        val revocationStarted = CompletableDeferred<Unit>()
        val finishRevocation = CompletableDeferred<Unit>()
        var renewals = 0
        val session = authSession(
            logout = {
                revocationStarted.complete(Unit)
                finishRevocation.await()
                error("connection lost")
            },
            reissue = { renewals++; tokens("refreshed-token") },
        )
        val revocation = async { assertFailsWith<IllegalStateException> { session.revokeRefreshTokenForTesting() } }
        revocationStarted.await()
        val first = session.prepareRequest("member-session")
        val second = session.prepareRequest("member-session")
        val firstRefresh = async { first.refreshAfterRejection() }
        val secondRefresh = async { second.refreshAfterRejection() }
        runCurrent()
        assertEquals(0, renewals)

        finishRevocation.complete(Unit)
        revocation.await()
        assertEquals("refreshed-token", firstRefresh.await())
        assertEquals("refreshed-token", secondRefresh.await())
        assertEquals(1, renewals)
    }

    @Test
    fun `접근 권한이 없으면 갱신하지 않는다`() = runTest {
        val testClient = client(
            refreshAccessToken = { error("403은 재발급하면 안 됨") },
            engine = MockEngine { respond("Forbidden", HttpStatusCode.Forbidden) },
        )

        val error = assertFailsWith<ClientRequestException> {
            testClient.get("https://api.example.com/data") {
                header(HttpHeaders.Authorization, "Bearer valid-token")
            }
        }
        assertEquals(HttpStatusCode.Forbidden, error.response.status)
        testClient.close()
    }

    private suspend fun TestScope.authSession(
        logout: suspend (String) -> Unit = {},
        reissue: suspend (String) -> MobileAuthTokens,
    ): AuthSession {
        var storedRefreshToken: String? = null
        return AuthSession(
            readRefreshToken = { storedRefreshToken },
            writeRefreshToken = { storedRefreshToken = it },
            clearRefreshToken = { storedRefreshToken = null },
            reissue = reissue,
            logout = logout,
            scope = backgroundScope,
            currentTimeMillis = { testScheduler.currentTime },
        ).also {
            it.signIn(tokens("expired-token"))
            it.bindMemberRequestKey("member-session")
        }
    }

    private fun tokens(accessToken: String) = MobileAuthTokens(
        accessToken = accessToken,
        refreshToken = "refresh-$accessToken",
        tokenType = "Bearer",
        accessTokenExpiresIn = 3_600L,
        refreshTokenExpiresIn = 7_200L,
    )
}
