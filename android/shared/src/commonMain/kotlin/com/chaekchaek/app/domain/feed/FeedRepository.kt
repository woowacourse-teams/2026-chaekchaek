package com.chaekchaek.app.domain.feed

/**
 * 홈 피드를 가져온다. 구현은 data 계층에 있고, 서버가 없는 동안에는 Fake 가 더미를 돌려준다.
 */
interface FeedRepository {
    suspend fun homeFeed(): HomeFeed
}
