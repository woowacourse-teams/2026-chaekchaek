package com.chaekchaek.app.ui

import kotlinx.coroutines.CancellationException

internal suspend fun retryHomeWithSessionRecovery(
    currentAccessToken: () -> String?,
    refreshAccessToken: suspend () -> String?,
    retryHome: (String?) -> Unit,
) {
    val previousAccessToken = currentAccessToken()
    val accessToken = try {
        refreshAccessToken()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        currentAccessToken()
    }
    // 토큰이 바뀌면 내비게이션의 인증 동기화가 홈을 다시 불러온다.
    if (previousAccessToken == accessToken) retryHome(accessToken)
}
