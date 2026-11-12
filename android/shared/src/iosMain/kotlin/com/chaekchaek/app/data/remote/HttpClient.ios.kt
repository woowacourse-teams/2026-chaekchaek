package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createHttpClient(
    prepareRequest: (suspend (String) -> AuthenticatedRequest)?,
): HttpClient {
    val client = HttpClient(Darwin) {
        configureChaekchaek()
    }
    return if (prepareRequest != null) {
        client.installSilentRefresh(prepareRequest)
    } else {
        client
    }
}
