package com.chaekchaek.app.ui.archive

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val API_LOADING_DELAY_MILLIS = 500L

internal suspend fun <T> withDelayedLoading(
    onLoadingChanged: (Boolean) -> Unit,
    request: suspend () -> T,
): Result<T> = coroutineScope {
    val indicator = launch {
        delay(API_LOADING_DELAY_MILLIS)
        onLoadingChanged(true)
    }
    try {
        runCatching { request() }
    } finally {
        indicator.cancelAndJoin()
        onLoadingChanged(false)
    }
}
