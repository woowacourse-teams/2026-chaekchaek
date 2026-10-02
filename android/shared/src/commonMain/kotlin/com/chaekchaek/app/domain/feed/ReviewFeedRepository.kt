package com.chaekchaek.app.domain.feed

data class ReviewFeedPage(
    val reviews: List<QuoteCard>,
    val totalCount: Int,
    val nextPage: Int?,
)

sealed interface FeedViewer {
    data object Anonymous : FeedViewer

    data class Member(val accessToken: String) : FeedViewer

    data class Guest(val guestToken: String) : FeedViewer
}

interface ReviewFeedRepository {
    suspend fun reviewFeed(page: Int, viewer: FeedViewer = FeedViewer.Anonymous): ReviewFeedPage
}
