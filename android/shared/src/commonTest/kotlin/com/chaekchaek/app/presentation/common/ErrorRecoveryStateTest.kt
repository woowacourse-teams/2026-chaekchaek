package com.chaekchaek.app.presentation.common

import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CancellationException
import kotlin.test.*

class ErrorRecoveryStateTest {
    @Test
    fun recoveryDependsOnStatusAndAvailableAction() {
        val errors = ErrorRecoveryState()
        for ((status, expected) in listOf(401 to ErrorAction.Login, 400 to ErrorAction.Confirm,
            403 to ErrorAction.Confirm, 404 to ErrorAction.Confirm, 409 to ErrorAction.Confirm,
            422 to ErrorAction.Confirm, 429 to ErrorAction.Retry, 500 to ErrorAction.Retry)) {
            errors.report(MobileLoginException("server detail must stay private", status), ErrorOperation.LibrarySave) {}
            assertEquals(expected, errors.notice.value?.action, "HTTP $status")
            assertFalse(errors.notice.value!!.message.contains("server detail"))
            if (expected == ErrorAction.Confirm) assertNull(errors.takeRecovery(errors.notice.value!!.id))
        }
        errors.report(IllegalStateException("private"), ErrorOperation.HomeLoad)
        assertEquals(ErrorAction.Confirm, errors.notice.value?.action)
        errors.report(MobileLoginException("provider", 401), ErrorOperation.SignIn) {}
        assertEquals(ErrorAction.Retry, errors.notice.value?.action)
    }

    @Test
    fun staleOrRepeatedActionsCannotRunAnotherPendingRequest() {
        val errors = ErrorRecoveryState()
        val resumed = mutableListOf<String?>()
        errors.report(Exception(), ErrorOperation.LibrarySave, resumed::add)
        val oldId = errors.notice.value!!.id
        errors.report(Exception(), ErrorOperation.LibrarySave, resumed::add)
        val id = errors.notice.value!!.id
        errors.dismiss(oldId)
        assertEquals(id, errors.notice.value?.id)
        assertNull(errors.takeRecovery(oldId))
        errors.takeRecovery(id)?.invoke("fresh-token")
        errors.takeRecovery(id)?.invoke("duplicate")
        assertEquals(listOf<String?>("fresh-token"), resumed)
        assertNull(errors.notice.value)
    }

    @Test
    fun closeDoesNotExecuteAndCancellationDoesNotBecomeAnError() {
        val errors = ErrorRecoveryState()
        var calls = 0
        errors.report(Exception(), ErrorOperation.Request) { calls++ }
        val id = errors.notice.value!!.id
        errors.dismiss()
        assertNull(errors.takeRecovery(id))
        assertEquals(0, calls)
        assertFailsWith<CancellationException> {
            errors.report(CancellationException(), ErrorOperation.Request) { calls++ }
        }
        assertNull(errors.notice.value)
    }
}
