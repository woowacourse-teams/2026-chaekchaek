package com.chaekchaek.app.di

import kotlin.test.Test
import kotlin.test.assertNotNull

class SharedComponentTest {
    @Test
    fun `컴포넌트를 생성할 수 있다`() {
        // given & when : KSP 가 생성한 create() 로 컴포넌트를 만들면
        val component = SharedComponent::class.create()

        // then : 인스턴스가 만들어진다
        assertNotNull(component)
    }
}
