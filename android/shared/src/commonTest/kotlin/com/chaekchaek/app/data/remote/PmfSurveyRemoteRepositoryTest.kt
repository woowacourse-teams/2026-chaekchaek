package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.survey.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.ResponseException
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class PmfSurveyRemoteRepositoryTest {
    @Test fun `회원과 게스트는 각각의 인증 헤더로 문항을 조회하고 제출 토큰과 답변만 전송한다`() = runTest {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val client = client { request ->
            requests += request
            respond(if (request.method == HttpMethod.Get) """{"eligible":true,"alreadySubmitted":false,"submissionToken":"test-submission","questions":[{"questionCode":"Q","question":"질문","multipleSelection":false,"options":[{"answerCode":"A","label":"답변"}]}]}""" else """{"submittedAt":"2026-11-06T00:00:00Z"}""", if (request.method == HttpMethod.Get) HttpStatusCode.OK else HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repo = PmfSurveyRemoteRepository(client)
        val member = SurveyRespondent.Member(3, "test-member")
        assertEquals("질문", repo.availability(member).questions.single().title)
        repo.submit(member, "test-submission", listOf(SurveyAnswer("Q", listOf("A"))))
        repo.availability(SurveyRespondent.Guest("test-guest"))
        assertEquals("Bearer test-member", requests[0].headers[HttpHeaders.Authorization])
        assertNull(requests[0].headers[WriteCredential.GUEST_TOKEN_HEADER])
        assertEquals("test-guest", requests[2].headers[WriteCredential.GUEST_TOKEN_HEADER])
        assertNull(requests[2].headers[HttpHeaders.Authorization])
        assertEquals("/api/v1/pmf-survey/responses", requests[1].url.encodedPath)
        assertEquals(Json.parseToJsonElement("""{"submissionToken":"test-submission","answers":[{"questionCode":"Q","answerCodes":["A"]}]}"""), Json.parseToJsonElement((requests[1].body as OutgoingContent.ByteArrayContent).bytes().decodeToString()))
        client.close()
    }

    @Test fun `미배포 404는 숨기되 조회 실패와 잘못된 응답은 전파한다`() = runTest {
        val respondent = SurveyRespondent.Guest("test-guest")
        val missing = client { respond("{}", HttpStatusCode.NotFound) }
        assertFalse(PmfSurveyRemoteRepository(missing).availability(respondent).canRespond)
        missing.close()
        val failure = client { respond("{}", HttpStatusCode.InternalServerError) }
        assertFailsWith<ResponseException> { PmfSurveyRemoteRepository(failure).availability(respondent) }
        failure.close()
    }

    @Test fun `이미 제출한 응답 충돌만 성공으로 복구하고 다른 충돌은 전파한다`() = runTest {
        for (code in listOf("PMF_SURVEY_ALREADY_RESPONDED", "OTHER_CONFLICT")) {
            val client = client { respond("""{"code":"$code"}""", HttpStatusCode.Conflict, headersOf(HttpHeaders.ContentType, "application/json")) }
            val submit: suspend () -> Unit = { PmfSurveyRemoteRepository(client).submit(SurveyRespondent.Guest("test-guest"), "test-submission", listOf(SurveyAnswer("Q", listOf("A")))) }
            if (code == "PMF_SURVEY_ALREADY_RESPONDED") submit() else assertFailsWith<ResponseException> { submit() }
            client.close()
        }
    }
    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
}
