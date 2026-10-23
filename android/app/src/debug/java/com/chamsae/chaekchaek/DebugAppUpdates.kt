package com.chamsae.chaekchaek

import android.content.Context
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager

/** Play가 없는 설치에서도 상태 전이를 테스트한다. 실제 Play UI와 설치는 별도 검증한다. */
internal object DebugAppUpdates {
    var fake: FakeAppUpdateManager? = null
        private set

    fun enable(context: Context) {
        fake = FakeAppUpdateManager(context.applicationContext).apply {
            setUpdateAvailable(BuildConfig.VERSION_CODE + 1)
        }
    }
    fun reset() { fake = null }
}
