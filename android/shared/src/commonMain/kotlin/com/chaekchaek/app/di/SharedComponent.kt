package com.chaekchaek.app.di

import com.chaekchaek.app.data.remote.PopularBooksRemoteRepository
import com.chaekchaek.app.domain.feed.FeedRepository
import com.chaekchaek.app.presentation.home.HomeViewModel
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import kotlin.time.Clock

/**
 * 공유 모듈의 의존성 그래프. 각 플랫폼이 이 컴포넌트를 만들어 ViewModel 을 꺼낸다.
 *
 * kotlin-inject 는 KSP 로 `SharedComponent::class.create()` 확장 함수를 생성한다.
 */
@Component
abstract class SharedComponent {
    abstract val homeViewModel: HomeViewModel

    @Provides
    fun clock(): Clock = Clock.System

    @Provides
    fun feedRepository(): FeedRepository = PopularBooksRemoteRepository()
}
