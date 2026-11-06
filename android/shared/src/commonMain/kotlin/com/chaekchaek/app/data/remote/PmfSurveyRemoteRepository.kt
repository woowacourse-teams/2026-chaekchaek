package com.chaekchaek.app.data.remote

import com.chaekchaek.app.domain.survey.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class PmfSurveyRemoteRepository(
    private val client: HttpClient = createHttpClient(),
    private val apiConfiguration: ApiConfiguration = ApiConfiguration.current,
) : PmfSurveyRepository {
    override suspend fun availability(respondent: SurveyRespondent): SurveyAvailability = try {
        client.get("${apiConfiguration.baseUrl}/api/v1/pmf-survey") {
            authenticate(respondent)
        }.body<AvailabilityDto>().let { dto ->
            SurveyAvailability(dto.eligible, dto.alreadySubmitted, dto.submissionToken, dto.questions.map { q ->
                SurveyQuestion(q.questionCode, q.question, q.multipleSelection, q.options.map { SurveyOption(it.answerCode, it.label) })
            })
        }
    } catch (error: ResponseException) {
        // 아직 설문이 배포되지 않은 환경에서는 기존 홈 이용을 계속한다.
        if (error.response.status == HttpStatusCode.NotFound) SurveyAvailability(false, false, null, emptyList())
        else throw error
    }

    override suspend fun submit(respondent: SurveyRespondent, submissionToken: String, answers: List<SurveyAnswer>) {
        try {
            client.post("${apiConfiguration.baseUrl}/api/v1/pmf-survey/responses") {
                authenticate(respondent)
                contentType(ContentType.Application.Json)
                setBody(SubmitDto(submissionToken, answers.map { AnswerDto(it.questionCode, it.answerCodes) }))
            }.body<SubmittedDto>()
        } catch (error: ResponseException) {
            // 저장 후 응답이 유실되어 재시도해도 서버의 완료 상태를 복구한다.
            if (error.response.status != HttpStatusCode.Conflict ||
                runCatching { error.response.body<ProblemDto>().code }.getOrNull() != "PMF_SURVEY_ALREADY_RESPONDED") throw error
        }
    }

    private fun HttpRequestBuilder.authenticate(respondent: SurveyRespondent) {
        val credential = when (respondent) {
            is SurveyRespondent.Member -> WriteCredential.Member(respondent.credential)
            is SurveyRespondent.Guest -> WriteCredential.Guest(respondent.credential)
        }
        header(credential.headerName, credential.headerValue)
    }
}

@Serializable private class AvailabilityDto(val eligible: Boolean, val alreadySubmitted: Boolean, val submissionToken: String? = null, val questions: List<QuestionDto>)
@Serializable private class QuestionDto(val questionCode: String, val question: String, val multipleSelection: Boolean, val options: List<OptionDto>)
@Serializable private class OptionDto(val answerCode: String, val label: String)
@Serializable private class SubmitDto(val submissionToken: String, val answers: List<AnswerDto>)
@Serializable private class AnswerDto(val questionCode: String, val answerCodes: List<String>)
@Serializable private class SubmittedDto(val submittedAt: String)
@Serializable private class ProblemDto(val code: String)
