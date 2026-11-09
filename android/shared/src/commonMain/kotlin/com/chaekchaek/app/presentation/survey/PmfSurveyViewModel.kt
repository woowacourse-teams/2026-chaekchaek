package com.chaekchaek.app.presentation.survey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.UserErrorReporter
import com.chaekchaek.app.domain.survey.*
import com.chaekchaek.app.presentation.common.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class SurveyOptionUiModel(val code: String, val label: String)
data class SurveyQuestionUiModel(val code: String, val title: String, val multiple: Boolean, val options: List<SurveyOptionUiModel>)
data class PmfSurveyUiState(
    val barVisible: Boolean = false,
    val dialogOpen: Boolean = false,
    val completed: Boolean = false,
    val questions: List<SurveyQuestionUiModel> = emptyList(),
    val answers: Map<String, Set<String>> = emptyMap(),
    val questionIndex: Int = 0,
    val submitting: Boolean = false,
    val showLoading: Boolean = false,
) {
    val question get() = questions.getOrNull(questionIndex)
    val canContinue get() = !submitting && question?.let { answers[it.code].orEmpty().isNotEmpty() } == true
}

class PmfSurveyViewModel(
    private val repository: PmfSurveyRepository,
    private val dismissal: SurveyDismissalStorage,
    private val clock: Clock,
    errorReporter: UserErrorReporter = UserErrorReporter.None,
) : ViewModel() {
    val errors = ErrorRecoveryState(errorReporter)
    private val state = MutableStateFlow(PmfSurveyUiState())
    val uiState = state.asStateFlow()
    private var respondent: SurveyRespondent? = null
    private var availability: SurveyAvailability? = null
    private var generation = 0L
    private var request: Job? = null
    private var wake: Job? = null
    private var homeVisible = false

    fun authenticate(person: SurveyRespondent?) {
        val samePerson = respondent?.isSamePerson(person) == true
        respondent = person
        if (samePerson) return
        generation++
        request?.cancel()
        request = null
        wake?.cancel()
        availability = null
        errors.dismiss()
        state.value = PmfSurveyUiState()
        if (homeVisible) load()
    }

    fun onHomeEntered() {
        homeVisible = true
        if (!state.value.dialogOpen && !state.value.completed) load()
    }

    fun onHomeHidden() {
        homeVisible = false
        wake?.cancel()
    }

    private fun load() {
        val person = respondent ?: return
        if (request?.isActive == true || state.value.completed) return
        val remaining = dismissal.hiddenUntilMillis() - clock.now().toEpochMilliseconds()
        if (remaining > 0) {
            state.value = state.value.copy(barVisible = false)
            wake?.cancel()
            if (homeVisible) wake = viewModelScope.launch { delay(remaining); load() }
            return
        }
        val revision = generation
        request = viewModelScope.launch {
            try {
                val result = repository.availability(person)
                if (revision != generation) return@launch
                availability = result
                val questions = result.questions.map { q -> SurveyQuestionUiModel(q.code, q.title, q.multiple, q.options.map { SurveyOptionUiModel(it.code, it.label) }) }
                val answers = result.questions.associate { q -> q.code to state.value.answers[q.code].orEmpty().intersect(q.options.map { it.code }.toSet()) }
                state.value = state.value.copy(barVisible = result.canRespond, questions = questions, answers = answers,
                    questionIndex = state.value.questionIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0)))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (revision == generation) report(error, ErrorOperation.SurveyLoad) { load() }
            }
        }
    }

    fun open() {
        if (state.value.dialogOpen || state.value.completed || !state.value.barVisible || availability?.canRespond != true) return
        // 응답을 시작한 설문과 제출용 토큰은 늦게 도착한 홈 조회로 교체하지 않는다.
        generation++
        request?.cancel()
        request = null
        state.value = state.value.copy(dialogOpen = true)
    }

    fun close() {
        if (!state.value.submitting) state.value = state.value.copy(dialogOpen = false)
    }

    fun dismissForDay() {
        if (!state.value.barVisible || state.value.dialogOpen) return
        // 홈 재조회보다 사용자가 선택한 숨김을 우선하고 늦게 도착한 응답도 무시한다.
        generation++
        request?.cancel()
        request = null
        dismissal.hideUntil(clock.now().toEpochMilliseconds() + HIDE_FOR_DAY_MILLIS)
        state.value = state.value.copy(barVisible = false)
        load()
    }

    fun select(code: String) {
        if (!state.value.dialogOpen || state.value.submitting) return
        val q = availability?.questions?.getOrNull(state.value.questionIndex) ?: return
        val selected = q.select(state.value.answers[q.code].orEmpty(), code)
        state.value = state.value.copy(answers = state.value.answers + (q.code to selected))
    }

    fun previous() {
        if (state.value.dialogOpen && !state.value.submitting && state.value.questionIndex > 0) state.value = state.value.copy(questionIndex = state.value.questionIndex - 1)
    }

    fun next() {
        if (!state.value.dialogOpen || !state.value.canContinue) return
        if (state.value.questionIndex < state.value.questions.lastIndex) state.value = state.value.copy(questionIndex = state.value.questionIndex + 1)
        else submit()
    }

    private fun submit() {
        if (state.value.submitting) return
        val person = respondent ?: return
        val survey = availability?.takeIf { it.canRespond } ?: return
        if (survey.questions.any { state.value.answers[it.code].orEmpty().isEmpty() }) return
        val answers = survey.questions.map { q -> SurveyAnswer(q.code, q.options.map { it.code }.filter { it in state.value.answers[q.code].orEmpty() }) }
        val revision = generation
        state.value = state.value.copy(submitting = true)
        request = viewModelScope.launch {
            try {
                withDelayedApiLoading({ loading -> if (revision == generation) state.value = state.value.copy(showLoading = loading) }) {
                    repository.submit(person, requireNotNull(survey.submissionToken), answers)
                }
                if (revision == generation) state.value = state.value.copy(barVisible = false, completed = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (revision == generation) report(error, ErrorOperation.SurveySubmit) { submit() }
            } finally {
                if (revision == generation) state.value = state.value.copy(submitting = false, showLoading = false)
            }
        }
    }

    private fun report(error: Throwable, operation: ErrorOperation, retry: () -> Unit) {
        val revision = generation
        errors.report(error, operation) { freshToken ->
            if (revision == generation) {
                val member = respondent as? SurveyRespondent.Member
                if (freshToken != null && member != null) respondent = SurveyRespondent.Member(member.memberId, freshToken)
                retry()
            }
        }
    }

    companion object { const val HIDE_FOR_DAY_MILLIS = 86_400_000L }
}
