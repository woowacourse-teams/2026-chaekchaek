package com.chamsae.chaekchaek

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.chaekchaek.app.config.MobilePolicyController
import com.chaekchaek.app.config.UpdateRequirement
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/** Play 업데이트만 담당한다. 최소 지원 버전의 판정은 공통 정책에 남긴다. */
class AndroidAppUpdates(
    private val activity: ComponentActivity,
    private var manager: AppUpdateManager,
    private val controller: MobilePolicyController,
    private val onStoreFailure: () -> Unit,
) {
    private var checking = false
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        controller.setUpdating(false)
        if (result.resultCode == Activity.RESULT_OK && controller.state.value.requirement == UpdateRequirement.Recommended) {
            controller.dismissRecommended()
        }
    }
    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            controller.setUpdating(false)
            controller.setDownloadReady(true)
        }
        if (state.installStatus() == InstallStatus.FAILED || state.installStatus() == InstallStatus.CANCELED) controller.setUpdating(false)
    }

    init { manager.registerListener(listener) }
    fun setManager(value: AppUpdateManager) {
        if (value === manager) return
        manager.unregisterListener(listener)
        manager = value
        manager.registerListener(listener)
        controller.setDownloadReady(false)
        controller.setUpdating(false)
    }
    fun dispose() { manager.unregisterListener(listener) }

    fun resume() {
        controller.setUpdating(false)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (activity.isDestroyed) return@addOnSuccessListener
            if (info.installStatus() == InstallStatus.DOWNLOADED) controller.setDownloadReady(true)
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                controller.state.value.requirement == UpdateRequirement.Required) launch(info, AppUpdateType.IMMEDIATE)
        }
    }

    fun update() {
        if (checking) return
        if (controller.state.value.downloadReady) {
            manager.completeUpdate().addOnFailureListener { onStoreFailure() }
            return
        }
        checking = true
        manager.appUpdateInfo.addOnSuccessListener { info ->
            checking = false
            if (activity.isDestroyed) return@addOnSuccessListener
            val required = controller.state.value.requirement == UpdateRequirement.Required
            val type = if (required) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(type) &&
                (!required || info.availableVersionCode() >= controller.state.value.policy.androidMinimumVersionCode)) {
                launch(info, type)
            } else openStore()
        }.addOnFailureListener { checking = false; if (!activity.isDestroyed) openStore() }
    }

    private fun launch(info: com.google.android.play.core.appupdate.AppUpdateInfo, type: Int) {
        controller.setUpdating(true)
        val started = runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }.getOrDefault(false)
        if (!started) { controller.setUpdating(false); openStore() }
    }

    private fun openStore() {
        // 주소와 패키지는 앱에 고정해 원격 설정에 의한 임의 사이트 이동을 막는다.
        val opened = runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}")))
        }.isSuccess || runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
        }.isSuccess
        if (!opened) onStoreFailure()
    }
}
