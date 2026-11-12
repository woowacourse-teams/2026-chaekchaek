package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient

internal expect fun createHttpClient(
    prepareRequest: (suspend (String) -> AuthenticatedRequest)? = null,
): HttpClient
