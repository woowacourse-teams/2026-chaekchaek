package com.chaekchaek.app.data.remote

import io.ktor.http.Url
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import platform.Foundation.NSProcessInfo

@OptIn(ExperimentalNativeApi::class)
internal actual fun platformApiBaseUrl(): String {
    val process = NSProcessInfo.processInfo
    if (Platform.isDebugBinary && "-uiTestingRefresh" in process.arguments) {
        val fixtureUrl = process.environment["CHAEKCHAEK_UI_TEST_API_URL"] as? String
            ?: error("새로고침 UI 테스트의 로컬 API 주소가 필요합니다")
        val url = Url(fixtureUrl)
        require(url.host == "127.0.0.1" && url.protocol.name == "http")
        return fixtureUrl
    }
    return GeneratedApiEnvironment.iosBaseUrl
}
