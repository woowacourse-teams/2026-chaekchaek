package com.chaekchaek.app.ui

import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HomeRetryTest {
    @Test
    fun temporaryRefreshFailuresStillRetryHomeWithTheCurrentToken() = runTest {
        for (error in listOf(
            MobileLoginException("HTTP_429", 429),
            MobileLoginException("INTERNAL_SERVER_ERROR", 500),
            IllegalStateException("offline"),
        )) {
            val requests = mutableListOf<String?>()
            retryHomeWithSessionRecovery(
                currentAccessToken = { "current-token" },
                refreshAccessToken = { throw error },
                retryHome = requests::add,
            )
            assertEquals(listOf<String?>("current-token"), requests)
        }
    }

    @Test
    fun guestRetryStillLoadsHome() = runTest {
        val requests = mutableListOf<String?>()
        retryHomeWithSessionRecovery({ null }, { null }, requests::add)
        assertEquals(listOf<String?>(null), requests)
    }

    @Test
    fun changedTokenLeavesReloadToAuthenticationSynchronization() = runTest {
        for (refreshedToken in listOf("new-token", null)) {
            var requests = 0
            retryHomeWithSessionRecovery({ "old-token" }, { refreshedToken }, { requests++ })
            assertEquals(0, requests)
        }
    }

    @Test
    fun cancellationDoesNotStartAnotherHomeRequest() = runTest {
        var requests = 0
        assertFailsWith<CancellationException> {
            retryHomeWithSessionRecovery(
                { "current-token" },
                { throw CancellationException("screen closed") },
                { requests++ },
            )
        }
        assertEquals(0, requests)
    }
}
