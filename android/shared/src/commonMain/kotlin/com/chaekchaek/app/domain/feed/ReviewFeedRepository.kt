package com.chaekchaek.app.domain.feed

data class ReviewFeedPage(
    val reviews: List<QuoteCard>,
    val totalCount: Int,
    val nextPage: Int?,
)

interface ReviewFeedRepository {
    suspend fun reviewFeed(page: Int, accessToken: String? = null): ReviewFeedPage
}
