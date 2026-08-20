package com.chamsae.chaekchaek.ui.common

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val API_LOADING_DELAY_MILLIS = 500L

internal suspend fun <T> withDelayedApiLoading(
  onLoadingChanged: (Boolean) -> Unit,
  request: suspend () -> T,
): T = coroutineScope {
  val indicator = launch {
    delay(API_LOADING_DELAY_MILLIS)
    onLoadingChanged(true)
  }
  try {
    request()
  } finally {
    indicator.cancelAndJoin()
    onLoadingChanged(false)
  }
}
