package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chaekchaek.app.config.MobilePolicyController
import com.chaekchaek.app.config.UpdateRequirement
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens

/** Figma 1524:6124. 내비게이션을 제거하지 않아 입력과 로그인 상태를 보존한다. */
@Composable
fun UpdatePolicyHost(controller: MobilePolicyController, onUpdate: () -> Unit) {
    val state by controller.state.collectAsState()
    val required = state.requirement == UpdateRequirement.Required
    if (state.requirement == UpdateRequirement.None && !state.downloadReady) return
    // Play 동의 창 뒤에 이 안내가 다시 나타나지 않게 한다. 취소하면 강제 안내가 복원된다.
    if (state.updating) return
    val dismiss = {
        if (!required) {
            controller.setDownloadReady(false)
            controller.dismissRecommended()
        }
    }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !required,
            dismissOnClickOutside = !required,
        ),
    ) {
        Surface(
            Modifier.padding(horizontal = 16.dp).widthIn(max = 358.dp).fillMaxWidth(),
            shape = ChaekOverlayTokens.containerShape,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (state.downloadReady) "업데이트가 준비됐어요" else if (required) "업데이트가 필요해요" else "새 버전이 있어요",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 31.5.sp, letterSpacing = (-1.0589).sp))
                Text(if (state.downloadReady) "앱을 다시 시작하면 업데이트가 적용됩니다." else if (required) "계속 이용하려면 최신 버전으로 업데이트해 주세요." else "더 나은 책췍을 만나 보세요.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, letterSpacing = (-0.7625).sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!required) ChaekOverlayButton("나중에", {
                        controller.setDownloadReady(false)
                        controller.dismissRecommended()
                    }, secondary = true, modifier = Modifier.weight(1f).fillMaxHeight())
                    ChaekOverlayButton(if (state.downloadReady) "다시 시작" else "업데이트", onUpdate, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/** 두 플랫폼의 Debug 진입점에서만 합성한다. 임시 설정은 프로세스 메모리에만 둔다. */
@Composable
fun PolicyDeveloperContent(controller: MobilePolicyController, onRefresh: () -> Unit) {
    val state by controller.state.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("원격 설정과 업데이트", style = MaterialTheme.typography.titleMedium)
        Text("설정 출처: ${state.source}\n마지막 수신: ${state.lastReceivedAt?.let { kotlin.time.Instant.fromEpochMilliseconds(it).toString() } ?: "없음"}\n수신 결과: ${state.fetchResult}\n현재 정책: ${state.requirement}\nAndroid 최소/최신: ${state.policy.androidMinimumVersionCode}/${state.policy.androidLatestVersionCode}\niOS 최소/최신: ${state.policy.iosMinimumVersion}/${state.policy.iosLatestVersion}\n검증 문구: ${state.policy.verificationMessage}", style = MaterialTheme.typography.bodyMedium)
        ChaekOverlayButton("설정 다시 받기", onRefresh, modifier = Modifier.fillMaxWidth())
        ChaekOverlayButton("강제 업데이트 재현", { controller.overrideRequirement(UpdateRequirement.Required) }, modifier = Modifier.fillMaxWidth())
        ChaekOverlayButton("권장 업데이트 재현", { controller.overrideRequirement(UpdateRequirement.Recommended) }, modifier = Modifier.fillMaxWidth())
        ChaekOverlayButton("최신 버전 재현", { controller.overrideRequirement(UpdateRequirement.None) }, modifier = Modifier.fillMaxWidth())
        ChaekOverlayButton("임시 설정 해제", { controller.overrideRequirement(null) }, modifier = Modifier.fillMaxWidth())
    }
}
