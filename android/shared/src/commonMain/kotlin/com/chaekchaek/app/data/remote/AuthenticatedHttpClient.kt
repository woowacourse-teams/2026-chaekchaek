package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

internal val authRetriedKey = AttributeKey<Boolean>("AuthRetriedKey")

internal object AuthTokenProvider {
    var refreshHandler: (suspend () -> String?)? = null
}

internal fun HttpClientConfig<*>.configureChaekchaek() {
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

internal fun HttpClient.installSilentRefresh(
    refreshAccessToken: suspend () -> String?,
): HttpClient {
    plugin(HttpSend).intercept { request ->
        val originalCall = execute(request)
        val authHeader = request.headers[HttpHeaders.Authorization]
        val isAuthEndpoint = request.url.buildString().contains("/api/v1/auth/mobile/")
        if (originalCall.response.status == HttpStatusCode.Unauthorized &&
            authHeader != null &&
            !isAuthEndpoint &&
            !request.attributes.contains(authRetriedKey)
        ) {
            request.attributes.put(authRetriedKey, true)
            val refreshed = refreshAccessToken()
            if (refreshed != null) {
                request.headers[HttpHeaders.Authorization] = "Bearer $refreshed"
                execute(request)
            } else {
                originalCall
            }
        } else {
            originalCall
        }
    }
    return this
}
