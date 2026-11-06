package com.chaekchaek.app.domain.survey

/** 인증값은 메모리에만 보관하며 로그나 로컬 표시 상태에 기록하지 않는다. */
sealed class SurveyRespondent(val credential: String) {
    class Member(val memberId: Long, accessToken: String) : SurveyRespondent(accessToken)
    class Guest(guestToken: String) : SurveyRespondent(guestToken)

    fun isSamePerson(other: SurveyRespondent?): Boolean = when {
        this is Member && other is Member -> memberId == other.memberId
        this is Guest && other is Guest -> credential == other.credential
        else -> false
    }
}

data class SurveyOption(val code: String, val label: String)
data class SurveyQuestion(val code: String, val title: String, val multiple: Boolean, val options: List<SurveyOption>) {
    fun select(current: Set<String>, code: String): Set<String> {
        if (options.none { it.code == code }) return current
        if (!multiple) return setOf(code)
        if (code in current) return current - code
        return if (this.code == "UNKNOWN_FEATURES") {
            if (code == "NO_UNKNOWN_FEATURES") setOf(code) else current - "NO_UNKNOWN_FEATURES" + code
        } else current + code
    }
}

// 제출용 토큰도 로그에 출력되지 않도록 data class를 사용하지 않는다.
class SurveyAvailability(
    val eligible: Boolean,
    val alreadySubmitted: Boolean,
    val submissionToken: String?,
    val questions: List<SurveyQuestion>,
) {
    val canRespond: Boolean get() = eligible && !alreadySubmitted && !submissionToken.isNullOrBlank() && questions.isNotEmpty()
}

data class SurveyAnswer(val questionCode: String, val answerCodes: List<String>)

interface PmfSurveyRepository {
    suspend fun availability(respondent: SurveyRespondent): SurveyAvailability
    suspend fun submit(respondent: SurveyRespondent, submissionToken: String, answers: List<SurveyAnswer>)
}

/** 이 기기에서 스와이프로 숨긴 시각만 저장한다. 응답 완료 여부는 서버가 관리한다. */
interface SurveyDismissalStorage {
    fun hiddenUntilMillis(): Long
    fun hideUntil(epochMillis: Long)
}
