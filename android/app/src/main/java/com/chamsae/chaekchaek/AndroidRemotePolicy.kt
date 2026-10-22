package com.chamsae.chaekchaek

import android.content.Context
import com.chaekchaek.app.config.InstalledApp
import com.chaekchaek.app.config.MobilePolicy
import com.chaekchaek.app.config.MobilePolicyController
import com.chaekchaek.app.config.PolicyStorage
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/** Activity를 보관하지 않는 앱 수명 저장소. 개발자 화면과 앱은 같은 적용값을 조회한다. */
class AndroidRemotePolicy private constructor(context: Context) {
    private val preferences = context.getSharedPreferences("mobile_policy", Context.MODE_PRIVATE)
    val controller = MobilePolicyController(
        InstalledApp(androidVersionCode = BuildConfig.VERSION_CODE),
        PolicyStorage(
            read = { preferences.getString("accepted", null) },
            write = { preferences.edit().putString("accepted", it).apply() },
        ),
    )
    private val remote = FirebaseRemoteConfig.getInstance()
    private var registration: ConfigUpdateListenerRegistration? = null

    init {
        remote.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600).setFetchTimeoutInSeconds(10).build())
        remote.setDefaultsAsync(mapOf(MobilePolicy.Parameter to MobilePolicy.encode(MobilePolicy())))
    }

    fun start() {
        if (registration == null) registration = remote.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(update: ConfigUpdate) {
                if (MobilePolicy.Parameter !in update.updatedKeys) return
                remote.activate().addOnCompleteListener { result ->
                    if (result.isSuccessful) accept() else controller.failed("활성화 실패: 마지막 정상 설정 유지")
                }
            }
            override fun onError(error: FirebaseRemoteConfigException) {
                controller.failed("실시간 연결 실패: 마지막 정상 설정 유지")
            }
        })
        refresh()
    }

    fun stop() { registration?.remove(); registration = null }

    fun refresh(force: Boolean = false) {
        if (!controller.beginFetch()) return
        remote.fetch(if (force) 0 else 3600).continueWithTask { fetched ->
            if (!fetched.isSuccessful) throw fetched.exception ?: IllegalStateException("수신 실패")
            remote.activate()
        }.addOnCompleteListener { result ->
            if (result.isSuccessful) accept() else controller.failed("수신 실패: 마지막 정상 설정 유지")
        }
    }

    private fun accept() {
        val value = remote.getValue(MobilePolicy.Parameter)
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            controller.received(value.asString(), remote.info.fetchTimeMillis)
        } else controller.failed("원격 값 없음: 기본값 또는 마지막 정상 설정 유지")
    }

    companion object {
        private var instance: AndroidRemotePolicy? = null
        fun get(context: Context): AndroidRemotePolicy = instance ?: AndroidRemotePolicy(context.applicationContext).also { instance = it }
    }
}
