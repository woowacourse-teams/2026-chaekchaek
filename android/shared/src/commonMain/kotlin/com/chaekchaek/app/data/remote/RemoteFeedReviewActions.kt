package com.chaekchaek.app.data.remote

import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.domain.feed.FeedReviewActions
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

class RemoteFeedReviewActions(
    private val repository: BookDetailRemoteRepository,
    private val authPlatform: AuthPlatformCallbacks,
    private val authRepository: MobileAuthRemoteRepository = MobileAuthRemoteRepository(),
) : FeedReviewActions {
    override suspend fun toggleLike(reviewId: Long, likedByMe: Boolean, accessToken: String?) {
        publicWrite(accessToken, retryUnauthorized = !likedByMe) { credential ->
            if (likedByMe) repository.unlikeReview(reviewId, credential)
            else repository.likeReview(reviewId, credential)
        }
    }

    override suspend fun createReply(reviewId: Long, content: String, accessToken: String?) {
        publicWrite(accessToken, retryUnauthorized = true) { credential ->
            repository.createReply(reviewId, content, credential)
        }
    }

    private suspend fun writeCredential(accessToken: String?): WriteCredential =
        accessToken?.let(WriteCredential::Member)
            ?: authPlatform.readGuest()?.token?.let(WriteCredential::Guest)
            ?: authRepository.issueGuest().also { authPlatform.writeGuest(it) }.let { WriteCredential.Guest(it.token) }

    private suspend fun <T> publicWrite(
        accessToken: String?,
        retryUnauthorized: Boolean,
        request: suspend (WriteCredential) -> T,
    ): T {
        val credential = writeCredential(accessToken)
        return try {
            request(credential)
        } catch (error: Throwable) {
            if (credential !is WriteCredential.Guest || !error.isUnauthorized() || !retryUnauthorized) throw error
            val guest = authRepository.issueGuest().also { authPlatform.writeGuest(it) }
            request(WriteCredential.Guest(guest.token))
        }
    }
}

private fun Throwable.isUnauthorized(): Boolean =
    this is ResponseException && response.status == HttpStatusCode.Unauthorized
