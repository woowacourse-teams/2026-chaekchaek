package com.chaekchaek.app.data.repository

import com.chaekchaek.app.data.datasource.FeedDataSource
import com.chaekchaek.app.data.remote.dto.toDomain
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.domain.feed.HomeFeed
import me.tatarka.inject.annotations.Inject

@Inject
class FeedRepositoryImpl(
    private val dataSource: FeedDataSource,
) : FeedRepository {
    override suspend fun homeFeed(): HomeFeed = dataSource.homeFeed().toDomain()
}
