package com.chaekchaek.app.data.remote

import io.ktor.http.URLProtocol
import io.ktor.http.Url

/** API 주소 관리 책임을 저장소에서 분리한다. 빌드 환경 설정이 기본값을 제공한다. */
class ApiConfiguration(baseUrl: String) {
    val baseUrl: String = baseUrl.trim().trimEnd('/')

    init {
        val url = Url(this.baseUrl)
        require(url.protocol == URLProtocol.HTTP || url.protocol == URLProtocol.HTTPS)
        require(url.host.isNotBlank() && url.encodedPath in listOf("", "/"))
        require(url.user == null && url.password == null && url.parameters.isEmpty() && url.fragment.isEmpty())
    }

    companion object {
        val current: ApiConfiguration by lazy { ApiConfiguration(platformApiBaseUrl()) }
        val environment: String get() = GeneratedApiEnvironment.name
    }
}

internal expect fun platformApiBaseUrl(): String
