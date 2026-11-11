package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createHttpClient(
    refreshAccessToken: (suspend () -> String?)?,
): HttpClient {
    val client = HttpClient(Darwin) {
        configureChaekchaek()
    }
    return if (refreshAccessToken != null) {
        client.installSilentRefresh(refreshAccessToken)
    } else {
        client
    }
}
