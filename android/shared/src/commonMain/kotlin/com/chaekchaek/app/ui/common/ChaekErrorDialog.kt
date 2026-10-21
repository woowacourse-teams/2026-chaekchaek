package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chaekchaek.app.auth.AuthViewModel
import com.chaekchaek.app.presentation.common.ErrorAction
import com.chaekchaek.app.presentation.common.ErrorNotice
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens

private class ErrorSources { val states = mutableStateListOf<ErrorRecoveryState>() }
private val LocalErrorSources = staticCompositionLocalOf<ErrorSources?> { null }

@Composable
fun ErrorRecoveryEffect(recovery: ErrorRecoveryState) {
    val sources = LocalErrorSources.current
    if (sources != null) {
        DisposableEffect(sources, recovery) {
            sources.states.add(recovery)
            onDispose { sources.states.remove(recovery) }
        }
    } else {
        val notice by recovery.notice.collectAsState()
        notice?.let { value ->
            ChaekErrorDialog(value, { recovery.dismiss(value.id) }) {
                recovery.takeRecovery(value.id)?.invoke(null)
            }
        }
    }
}

/** 모든 시트 위에 오류 하나만 표시한다. 로그인 성공 전에는 대기 작업을 소비하지 않는다. */
@Composable
internal fun ErrorRecoveryHost(auth: AuthViewModel, content: @Composable () -> Unit) {
    val sources = remember { ErrorSources() }
    var authenticating by remember { mutableStateOf<Pair<ErrorRecoveryState, Long>?>(null) }
    val authState by auth.uiState.collectAsState()
    CompositionLocalProvider(LocalErrorSources provides sources) {
        content()
        ErrorRecoveryEffect(auth.errors)
        authenticating?.let { (owner, id) ->
            val resume: suspend (String) -> Unit = { token ->
                authenticating = null
                if (owner in sources.states) owner.takeRecovery(id)?.invoke(token)
            }
            LoginRequiredSheet(
                signingIn = authState.signingIn,
                error = null,
                appleSignInAvailable = auth.appleSignInAvailable,
                onDismiss = {
                    auth.cancelPendingAuthentication()
                    authenticating = null
                    owner.dismiss(id)
                },
                onAppleSignIn = { auth.requireAppleAuthentication(force = true, action = resume) },
                onGoogleSignIn = { auth.requireAuthentication(force = true, action = resume) },
            )
        }
        val active = sources.states.map { owner ->
            val notice = key(owner) { owner.notice.collectAsState().value }
            owner to notice
        }.lastOrNull { (owner, notice) ->
            notice != null && (authenticating == null || owner === auth.errors)
        }?.let { (owner, notice) -> owner to requireNotNull(notice) }
        active?.let { (owner, notice) ->
            ChaekErrorDialog(notice, { owner.dismiss(notice.id) }) {
                if (notice.action == ErrorAction.Login) authenticating = owner to notice.id
                else owner.takeRecovery(notice.id)?.invoke(null)
            }
        }
    }
}

/** Figma 책췍/ErrorDialog 1394:5478. 두 버튼은 좁은 화면과 큰 글자에서도 가로 배치한다. */
@Composable
fun ChaekErrorDialog(notice: ErrorNotice, onDismiss: () -> Unit, onAction: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.padding(horizontal = 16.dp).widthIn(max = 358.dp).fillMaxWidth(),
            shape = ChaekOverlayTokens.containerShape,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.End) {
                    ChaekCloseButton(onDismiss)
                }
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(notice.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 31.5.sp, letterSpacing = (-1.0589).sp))
                    Text(notice.message, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, letterSpacing = (-0.7625).sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (notice.action != ErrorAction.Confirm) {
                            ChaekOverlayButton("닫기", onDismiss, secondary = true, modifier = Modifier.weight(1f).fillMaxHeight(), textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = (-0.1504).sp))
                        }
                        ChaekOverlayButton(notice.action.label, onAction, modifier = Modifier.weight(1f).fillMaxHeight(), textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = (-0.1504).sp))
                    }
                }
            }
        }
    }
}
