package com.chaekchaek.app.data.remote

import com.chaekchaek.app.auth.MemberAuthorization
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

internal val authRetriedKey = AttributeKey<Boolean>("AuthRetriedKey")
internal val failedMemberAuthorization = AttributeKey<MemberAuthorization>("FailedMemberAuthorization")

internal class AuthenticatedRequest(
    val accessToken: String,
    val member: MemberAuthorization? = null,
    val refreshAfterRejection: suspend () -> String?,
)

internal fun HttpClientConfig<*>.configureChaekchaek() {
    expectSuccess = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

internal fun HttpClient.installSilentRefresh(
    prepareRequest: suspend (String) -> AuthenticatedRequest,
): HttpClient {
    plugin(HttpSend).intercept { request ->
        val authHeader = request.headers[HttpHeaders.Authorization]
        val isAuthEndpoint = request.url.buildString().contains("/api/v1/auth/mobile/")
        if (authHeader == null || !authHeader.startsWith("Bearer ", ignoreCase = true) ||
            isAuthEndpoint || request.attributes.contains(authRetriedKey)
        ) return@intercept execute(request)

        val credentials = prepareRequest(authHeader.substringAfter(' '))
        request.headers[HttpHeaders.Authorization] = "Bearer ${credentials.accessToken}"
        val originalCall = execute(request)
        val completedCall = if (originalCall.response.status == HttpStatusCode.Unauthorized) {
            request.attributes.put(authRetriedKey, true)
            val refreshed = credentials.refreshAfterRejection()
            if (refreshed != null) {
                request.headers[HttpHeaders.Authorization] = "Bearer $refreshed"
                execute(request)
            } else {
                originalCall
            }
        } else {
            originalCall
        }
        if (completedCall.response.status == HttpStatusCode.Unauthorized) {
            credentials.member?.let { completedCall.attributes.put(failedMemberAuthorization, it) }
        }
        completedCall
    }
    return this
}
