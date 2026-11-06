package com.chaekchaek.app.presentation.survey

import com.chaekchaek.app.domain.survey.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PmfSurveyViewModelTest {
    private fun scenario(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }

    @Test fun `선택 전 다음은 비활성이고 이전과 닫기 후에도 선택을 보존한다`() = scenario {
        val vm = viewModel()
        vm.authenticate(SurveyRespondent.Guest("test-guest")); vm.onHomeEntered(); runCurrent(); vm.open()
        assertFalse(vm.uiState.value.canContinue)
        vm.next(); assertEquals(0, vm.uiState.value.questionIndex)
        vm.select("A"); vm.next(); vm.select("B"); vm.previous()
        assertEquals(setOf("A"), vm.uiState.value.answers["FIRST"])
        vm.close(); vm.open()
        assertEquals(setOf("A"), vm.uiState.value.answers["FIRST"])
    }

    @Test fun `몰랐던 기능 없음은 다른 복수 선택과 함께 선택할 수 없다`() = scenario {
        val vm = viewModel(); vm.authenticate(SurveyRespondent.Guest("test-guest")); vm.onHomeEntered(); runCurrent(); vm.open()
        vm.select("A"); vm.next(); vm.select("A"); vm.select("B")
        assertEquals(setOf("A", "B"), vm.uiState.value.answers["UNKNOWN_FEATURES"])
        vm.select("NO_UNKNOWN_FEATURES")
        assertEquals(setOf("NO_UNKNOWN_FEATURES"), vm.uiState.value.answers["UNKNOWN_FEATURES"])
        vm.select("B")
        assertEquals(setOf("B"), vm.uiState.value.answers["UNKNOWN_FEATURES"])
    }

    @Test fun `기기에 저장한 숨김은 재실행에도 유지되고 정확히 24시간 후 재조회한다`() = scenario {
        var now = 0L
        val storage = MemoryDismissal()
        val repository = Repository()
        fun newVm() = PmfSurveyViewModel(repository, storage, object : Clock { override fun now() = Instant.fromEpochMilliseconds(now) })
        val first = newVm(); first.authenticate(SurveyRespondent.Guest("test-guest")); first.onHomeEntered(); runCurrent()
        first.dismissForDay(); first.onHomeHidden()
        assertEquals(86_400_000L, storage.until)
        val second = newVm(); second.authenticate(SurveyRespondent.Guest("test-guest")); second.onHomeEntered(); runCurrent()
        now = 86_399_999; advanceTimeBy(86_399_999); runCurrent()
        assertFalse(second.uiState.value.barVisible)
        assertEquals(1, repository.loads)
        now++; advanceTimeBy(1); runCurrent()
        assertTrue(second.uiState.value.barVisible)
        assertEquals(2, repository.loads)
        second.onHomeHidden()
    }

    @Test fun `재조회 중 숨기면 늦은 응답을 무시하고 24시간 뒤 다시 조회한다`() = scenario {
        var now = 0L
        var loads = 0
        val pending = CompletableDeferred<SurveyAvailability>()
        val repo = object : PmfSurveyRepository {
            override suspend fun availability(respondent: SurveyRespondent): SurveyAvailability =
                if (++loads == 2) withContext(NonCancellable) { pending.await() } else available()
            override suspend fun submit(respondent: SurveyRespondent, submissionToken: String, answers: List<SurveyAnswer>) = Unit
        }
        val vm = PmfSurveyViewModel(repo, MemoryDismissal(), object : Clock { override fun now() = Instant.fromEpochMilliseconds(now) })
        vm.authenticate(SurveyRespondent.Guest("test-guest")); vm.onHomeEntered(); runCurrent()
        vm.onHomeEntered(); runCurrent()
        vm.dismissForDay(); runCurrent()
        pending.complete(available()); runCurrent()
        assertFalse(vm.uiState.value.barVisible)
        now = 86_400_000; advanceTimeBy(86_400_000); runCurrent()
        assertEquals(3, loads)
        assertTrue(vm.uiState.value.barVisible)
        vm.onHomeHidden()
    }

    @Test fun `이미 응답했거나 대상이 아니면 바를 표시하지 않는다`() = scenario {
        for (result in listOf(SurveyAvailability(false, false, null, emptyList()), SurveyAvailability(true, true, null, emptyList()))) {
            val vm = viewModel(Repository(result)); vm.authenticate(SurveyRespondent.Guest("test-guest")); vm.onHomeEntered(); runCurrent()
            assertFalse(vm.uiState.value.barVisible)
        }
    }

    @Test fun `제출 실패의 선택을 보존하고 재시도 중 중복 제출을 막으며 완료 후 바를 숨긴다`() = scenario {
        val repo = Repository(); repo.fail = true
        val vm = viewModel(repo); vm.authenticate(SurveyRespondent.Member(1, "test-member")); vm.onHomeEntered(); runCurrent(); vm.open()
        vm.select("A"); vm.next(); vm.select("B"); vm.next(); vm.next(); runCurrent()
        assertEquals(1, repo.submits)
        assertTrue(vm.uiState.value.dialogOpen)
        assertEquals(setOf("B"), vm.uiState.value.answers["UNKNOWN_FEATURES"])
        val notice = assertNotNull(vm.errors.notice.value)
        repo.fail = false
        vm.errors.takeRecovery(notice.id)?.invoke(null); runCurrent()
        assertEquals(2, repo.submits)
        assertTrue(vm.uiState.value.completed)
        assertFalse(vm.uiState.value.barVisible)
        vm.close(); vm.onHomeEntered(); runCurrent()
        assertFalse(vm.uiState.value.dialogOpen)
        assertEquals(1, repo.loads)
    }

    @Test fun `같은 회원의 토큰 갱신은 답변을 보존하고 다른 회원 전환은 삭제한다`() = scenario {
        val vm = viewModel(); vm.authenticate(SurveyRespondent.Member(1, "test-old")); vm.onHomeEntered(); runCurrent(); vm.open(); vm.select("A")
        vm.authenticate(SurveyRespondent.Member(1, "test-new"))
        assertTrue(vm.uiState.value.dialogOpen)
        assertEquals(setOf("A"), vm.uiState.value.answers["FIRST"])
        vm.authenticate(SurveyRespondent.Member(2, "test-other")); runCurrent()
        assertFalse(vm.uiState.value.dialogOpen)
        assertTrue(vm.uiState.value.answers.values.all { it.isEmpty() })
    }

    @Test fun `취소를 무시하고 늦게 도착한 이전 회원의 응답은 현재 회원을 변경하지 않는다`() = scenario {
        val pending = CompletableDeferred<SurveyAvailability>()
        val repo = object : PmfSurveyRepository {
            override suspend fun availability(respondent: SurveyRespondent): SurveyAvailability =
                if ((respondent as SurveyRespondent.Member).memberId == 1L) withContext(NonCancellable) { pending.await() }
                else SurveyAvailability(false, false, null, emptyList())
            override suspend fun submit(respondent: SurveyRespondent, submissionToken: String, answers: List<SurveyAnswer>) = Unit
        }
        val vm = viewModel(repo); vm.authenticate(SurveyRespondent.Member(1, "test-old")); vm.onHomeEntered(); runCurrent()
        vm.authenticate(SurveyRespondent.Member(2, "test-new")); runCurrent(); pending.complete(available()); runCurrent()
        assertFalse(vm.uiState.value.barVisible)
    }

    private fun viewModel(repo: PmfSurveyRepository = Repository()) = PmfSurveyViewModel(repo, MemoryDismissal(), object : Clock { override fun now() = Instant.fromEpochMilliseconds(0) })
    private class MemoryDismissal : SurveyDismissalStorage {
        var until = 0L
        override fun hiddenUntilMillis() = until
        override fun hideUntil(epochMillis: Long) { until = epochMillis }
    }
    private class Repository(val result: SurveyAvailability = available()) : PmfSurveyRepository {
        var loads = 0; var submits = 0; var fail = false
        override suspend fun availability(respondent: SurveyRespondent): SurveyAvailability { loads++; return result }
        override suspend fun submit(respondent: SurveyRespondent, submissionToken: String, answers: List<SurveyAnswer>) {
            submits++; assertEquals("test-submission", submissionToken)
            assertEquals(listOf(SurveyAnswer("FIRST", listOf("A")), SurveyAnswer("UNKNOWN_FEATURES", listOf("B"))), answers)
            if (fail) error("test network failure")
        }
    }
    companion object {
        private fun available() = SurveyAvailability(true, false, "test-submission", listOf(
            SurveyQuestion("FIRST", "첫 문항", false, listOf(SurveyOption("A", "가"))),
            SurveyQuestion("UNKNOWN_FEATURES", "복수 문항", true, listOf(SurveyOption("A", "가"), SurveyOption("B", "나"), SurveyOption("NO_UNKNOWN_FEATURES", "없음"))),
        ))
    }
}
