package com.chaekchaek.app.domain.feed

interface FeedReviewActions {
    suspend fun toggleLike(reviewId: Long, likedByMe: Boolean, accessToken: String?)
    suspend fun createReply(reviewId: Long, content: String, accessToken: String?)
}
