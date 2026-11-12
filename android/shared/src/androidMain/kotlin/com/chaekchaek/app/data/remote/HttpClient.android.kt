package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createHttpClient(
    prepareRequest: (suspend (String) -> AuthenticatedRequest)?,
): HttpClient {
    val client = HttpClient(OkHttp) {
        configureChaekchaek()
    }
    return if (prepareRequest != null) {
        client.installSilentRefresh(prepareRequest)
    } else {
        client
    }
}
