package com.chaekchaek.app.analytics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AnalyticsCollectionPolicy(
    @SerialName("schema_version") val schemaVersion: Int,
    val revision: String,
    val enabled: Boolean,
    @SerialName("sample_rate") val sampleRate: Double,
    val events: Map<String, AnalyticsEventRule> = emptyMap(),
)

@Serializable
private data class AnalyticsEventRule(
    val enabled: Boolean = true,
    @SerialName("sample_rate") val sampleRate: Double? = null,
)

/** 호출과 갱신은 양 플랫폼 모두 UI 스레드에서 수행한다. */
class AnalyticsCollectionController(
    readCachedPolicy: () -> String? = { null },
    private val writeCachedPolicy: (String) -> Unit = {},
    samplingSeed: String = "default",
) {
    private val sample = stableSample(samplingSeed)
    private var policy = runCatching { readCachedPolicy()?.let(::parsePolicy) }.getOrNull()
        ?: AnalyticsCollectionPolicy(1, "default", true, 1.0)

    /** 비어 있거나 잘못된 응답은 마지막 유효 설정을 덮어쓰지 않는다. */
    fun receive(payload: String): Boolean {
        val next = parsePolicy(payload) ?: return false
        policy = next
        runCatching { writeCachedPolicy(payload) }
        return true
    }

    fun filter(event: AnalyticsEvent): AnalyticsEvent? {
        val rule = policy.events[event.name]
        val rate = rule?.sampleRate ?: policy.sampleRate
        if (!policy.enabled || rule?.enabled == false || sample >= rate) return null
        return event.copy(
            stringParameters = event.stringParameters + ("collection_revision" to policy.revision),
            doubleParameters = event.doubleParameters + ("sample_rate" to rate),
        )
    }

    companion object {
        const val FlagKey = "mobile-analytics-collection-v1"
        private val json = Json { ignoreUnknownKeys = true }

        private fun parsePolicy(payload: String): AnalyticsCollectionPolicy? = runCatching {
            require(payload.length <= 32_768)
            json.decodeFromString<AnalyticsCollectionPolicy>(payload).also { policy ->
                require(policy.schemaVersion == 1)
                require(policy.revision.matches(Regex("[A-Za-z0-9_.-]{1,64}")))
                require(policy.sampleRate.isFinite() && policy.sampleRate in 0.0..1.0)
                require(policy.events.size <= 128)
                policy.events.forEach { (name, rule) ->
                    require(name.matches(Regex("[a-z][a-z0-9_]{0,39}")))
                    require(rule.sampleRate == null || (rule.sampleRate.isFinite() && rule.sampleRate in 0.0..1.0))
                }
            }
        }.getOrNull()

        // 설치별로 같은 표본을 유지하여 start/result와 여러 방문의 연결을 보존한다.
        private fun stableSample(seed: String): Double {
            var hash = 2_166_136_261u
            seed.forEach { hash = (hash xor it.code.toUInt()) * 16_777_619u }
            return hash.toDouble() / 4_294_967_296.0
        }
    }
}
