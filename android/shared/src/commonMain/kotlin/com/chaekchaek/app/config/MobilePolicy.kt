package com.chaekchaek.app.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 비밀값과 스토어 주소는 원격 설정에 넣지 않는다. 하나의 payload로 버전 경계를 함께 검증한다. */
@Serializable
data class MobilePolicy(
    val schemaVersion: Int = 1,
    val androidMinimumVersionCode: Int = 0,
    val androidLatestVersionCode: Int = 0,
    val iosMinimumVersion: String = "0.0.0",
    val iosLatestVersion: String = "0.0.0",
    val verificationMessage: String = "",
) {
    companion object {
        const val Parameter = "mobile_policy_v1"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun parse(raw: String): MobilePolicy? = runCatching {
            require(raw.length <= 4096)
            json.decodeFromString<MobilePolicy>(raw).also {
                require(it.schemaVersion == 1)
                require(it.androidMinimumVersionCode >= 0)
                require(it.androidLatestVersionCode >= it.androidMinimumVersionCode)
                val minimum = requireNotNull(AppVersion.parse(it.iosMinimumVersion))
                val latest = requireNotNull(AppVersion.parse(it.iosLatestVersion))
                require(latest >= minimum)
                require(it.verificationMessage.length <= 200)
            }
        }.getOrNull()

        fun encode(policy: MobilePolicy): String = json.encodeToString(serializer(), policy)
    }
}

data class AppVersion private constructor(val parts: List<Int>) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        for (i in 0..2) {
            val result = parts[i].compareTo(other.parts[i])
            if (result != 0) return result
        }
        return 0
    }

    companion object {
        fun parse(value: String): AppVersion? {
            val parts = value.split('.')
            if (parts.size !in 2..3 || parts.any { it.isEmpty() || it.any { c -> c !in '0'..'9' } }) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            return AppVersion(numbers + List(3 - numbers.size) { 0 })
        }
    }
}

enum class UpdateRequirement { None, Recommended, Required }

/** Android는 증가하는 버전 코드, iOS는 숫자로 나눈 버전으로 판정한다. */
class InstalledApp(val androidVersionCode: Int = 0, val iosVersion: String = "") {
    fun requirement(policy: MobilePolicy): UpdateRequirement {
        if (iosVersion.isNotEmpty()) {
            val installed = AppVersion.parse(iosVersion) ?: return UpdateRequirement.None
            return when {
                installed < requireNotNull(AppVersion.parse(policy.iosMinimumVersion)) -> UpdateRequirement.Required
                installed < requireNotNull(AppVersion.parse(policy.iosLatestVersion)) -> UpdateRequirement.Recommended
                else -> UpdateRequirement.None
            }
        }
        return when {
            androidVersionCode < policy.androidMinimumVersionCode -> UpdateRequirement.Required
            androidVersionCode < policy.androidLatestVersionCode -> UpdateRequirement.Recommended
            else -> UpdateRequirement.None
        }
    }
}
