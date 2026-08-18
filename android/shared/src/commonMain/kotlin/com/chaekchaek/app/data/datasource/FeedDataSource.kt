package com.chaekchaek.app.data.datasource

import com.chaekchaek.app.data.remote.dto.HomeFeedResponse

/**
 * DataSource 는 DTO 를 돌려주고 Repository 가 도메인으로 바꾼다.
 *
 * 이렇게 나눈 덕분에 서버가 없는 동안에도 [FeedFakeDataSource] 를 쓰면서 매핑 코드가 매번
 * 실행된다. Fake 를 Repository 레벨에 두면 매핑이 서버 붙는 날까지 한 번도 안 돌아본다.
 */
interface FeedDataSource {
    suspend fun homeFeed(): HomeFeedResponse
}
