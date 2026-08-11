package com.chaekchaek.app.di

import me.tatarka.inject.annotations.Component

/**
 * 공유 모듈의 의존성 그래프. 각 플랫폼이 이 컴포넌트를 만들어 ViewModel 을 꺼낸다.
 *
 * kotlin-inject 는 KSP 로 `SharedComponent::class.create()` 확장 함수를 생성한다.
 */
@Component
abstract class SharedComponent
