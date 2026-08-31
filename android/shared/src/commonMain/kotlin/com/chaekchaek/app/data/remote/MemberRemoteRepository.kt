package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class MemberRemoteRepository(private val client: HttpClient = createHttpClient()) {
    suspend fun get(accessToken: String): RemoteMemberProfile =
        client.get("$BASE_URL/api/v1/members/me") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body<MemberResponseDto>().toRemoteMemberProfile()

    suspend fun updateNickname(nickname: String, accessToken: String): RemoteMemberProfile =
        client.patch("$BASE_URL/api/v1/members/me/nickname") {
            authenticatedJson(accessToken, NicknameRequest(nickname))
        }.body<MemberResponseDto>().toRemoteMemberProfile()

    suspend fun updateAnonymity(displayAnonymous: Boolean, accessToken: String): RemoteMemberProfile =
        client.patch("$BASE_URL/api/v1/members/me/anonymity") {
            authenticatedJson(accessToken, AnonymityRequest(displayAnonymous))
        }.body<MemberResponseDto>().toRemoteMemberProfile()

    private fun io.ktor.client.request.HttpRequestBuilder.authenticatedJson(accessToken: String, body: Any) {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val BASE_URL = "https://api.chaekchaek.com"
    }
}

data class RemoteMemberProfile(
    val memberId: Long,
    val nickname: String,
    val anonymousNickname: String,
    val displayAnonymous: Boolean,
)

@Serializable
private data class NicknameRequest(val nickname: String)

@Serializable
private data class AnonymityRequest(val displayAnonymous: Boolean)

@Serializable
private data class MemberResponseDto(
    val memberId: Long,
    val nickname: String? = null,
    val anonymousNickname: String = "",
    val displayAnonymous: Boolean,
) {
    fun toRemoteMemberProfile() = RemoteMemberProfile(memberId, nickname.orEmpty(), anonymousNickname, displayAnonymous)
}
