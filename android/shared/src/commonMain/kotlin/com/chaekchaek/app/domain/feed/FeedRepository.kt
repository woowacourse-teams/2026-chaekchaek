package com.chaekchaek.app.domain.feed

/**
 * 홈 피드를 가져온다. 현재는 공개 인기 책 API를 홈 콜라주로 전달한다.
 */
interface FeedRepository {
    suspend fun homeFeed(): HomeFeed
}
