package com.chaekchaek.app.config

import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PolicyStorage(
    val read: () -> String? = { null },
    val write: (String) -> Unit = {},
)

data class PolicyState(
    val policy: MobilePolicy = MobilePolicy(),
    val source: String = "기본값",
    val lastReceivedAt: Long? = null,
    val fetching: Boolean = false,
    val fetchResult: String = "아직 수신하지 않음",
    val requirement: UpdateRequirement = UpdateRequirement.None,
    val downloadReady: Boolean = false,
    val updating: Boolean = false,
)

/** 플랫폼 어댑터는 메인 스레드에서 결과를 전달한다. 유효하지 않은 값은 정상 캐시를 덮어쓰지 않는다. */
class MobilePolicyController(
    val installed: InstalledApp,
    private val storage: PolicyStorage = PolicyStorage(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    val errors = com.chaekchaek.app.presentation.common.ErrorRecoveryState()
    fun storeFailed() = errors.confirm("스토어를 열지 못했어요. 연결 상태를 확인한 뒤 다시 시도해 주세요.", "업데이트하지 못했어요")
    private var accepted = storage.read()?.let { saved ->
        val separator = saved.indexOf('\n')
        if (separator < 0) null else MobilePolicy.parse(saved.substring(separator + 1))?.let {
            saved.substring(0, separator).toLongOrNull()?.let { time ->
                PolicyState(it, "캐시", time, requirement = installed.requirement(it))
            }
        }
    } ?: PolicyState()
    private val mutableState = MutableStateFlow(accepted)
    val state = mutableState.asStateFlow()
    private var override: UpdateRequirement? = null
    private var dismissedPolicy: MobilePolicy? = null

    /** 캐시 정책과 별도로 실제 수신 시각을 보관한다. SDK 캐시 적중은 수신으로 기록하지 않는다. */
    fun beginFetch(): Boolean {
        if (state.value.fetching) return false
        mutableState.value = state.value.copy(fetching = true, fetchResult = "가져오는 중")
        return true
    }

    fun received(raw: String, receivedAt: Long) {
        if (receivedAt < (accepted.lastReceivedAt ?: 0)) {
            failed("이전 응답 무시: 마지막 정상 설정 유지")
            return
        }
        val policy = MobilePolicy.parse(raw)
        if (policy == null) { failed("설정 형식 오류: 마지막 정상 설정 유지"); return }
        storage.write("$receivedAt\n${MobilePolicy.encode(policy)}")
        accepted = accepted.copy(policy = policy, source = "원격", lastReceivedAt = receivedAt,
            fetching = false, fetchResult = "수신 성공", requirement = installed.requirement(policy))
        publish()
    }

    fun failed(message: String) {
        accepted = accepted.copy(fetching = false, fetchResult = message)
        publish()
    }

    fun fetched(raw: String, receivedAt: Long, error: String?) {
        if (error != null) failed(error) else received(raw, receivedAt)
    }

    fun receivedNow(raw: String) = received(raw, now())

    fun dismissRecommended() {
        if (state.value.requirement != UpdateRequirement.Recommended) return
        dismissedPolicy = state.value.policy
        mutableState.value = state.value.copy(requirement = UpdateRequirement.None)
    }

    fun setDownloadReady(ready: Boolean) { mutableState.value = state.value.copy(downloadReady = ready) }
    fun setUpdating(updating: Boolean) { mutableState.value = state.value.copy(updating = updating) }

    /** 호출 UI는 Debug 소스 또는 DEBUG Swift에만 둔다. 원격 정책과 디스크 캐시는 바꾸지 않는다. */
    fun overrideRequirement(requirement: UpdateRequirement?) { override = requirement; dismissedPolicy = null; publish() }

    private fun publish() {
        val requirement = override ?: accepted.requirement.let {
            if (it == UpdateRequirement.Recommended && accepted.policy == dismissedPolicy) UpdateRequirement.None else it
        }
        mutableState.value = accepted.copy(
            source = if (override == null) accepted.source else "임시",
            requirement = requirement,
            downloadReady = state.value.downloadReady,
            updating = state.value.updating,
        )
    }
}
