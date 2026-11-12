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
import com.chaekchaek.app.data.remote.MobileLoginException
import com.chaekchaek.app.presentation.common.ErrorAction
import com.chaekchaek.app.presentation.common.ErrorNotice
import com.chaekchaek.app.presentation.common.ErrorOperation
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
    val sessionExpiry = remember { ErrorRecoveryState() }
    val requiresLogin by auth.requiresLogin.collectAsState()
    val memberSession by auth.memberSession.collectAsState()
    val expiryNotice by sessionExpiry.notice.collectAsState()
    var authenticating by remember { mutableStateOf<AuthenticationRecovery?>(null) }
    val authState by auth.uiState.collectAsState()
    LaunchedEffect(requiresLogin, memberSession) {
        val member = memberSession
        if (requiresLogin && member != null) {
            sessionExpiry.report(
                MobileLoginException("UNAUTHORIZED", 401, member.requestKey, member.generation),
                ErrorOperation.Request,
            ) {}
        } else {
            sessionExpiry.dismiss()
        }
    }
    CompositionLocalProvider(LocalErrorSources provides sources) {
        content()
        ErrorRecoveryEffect(auth.errors)
        authenticating?.let { (owner, id, originalRequestKey) ->
            val resume: suspend (String) -> Unit = { token ->
                authenticating = null
                if (originalRequestKey != null && originalRequestKey != token) owner.dismiss(id)
                else if (owner in sources.states || owner === sessionExpiry) owner.takeRecovery(id)?.invoke(token)
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
                onAppleSignIn = { auth.requireAppleMemberAuthentication(force = true, action = resume) },
                onGoogleSignIn = { auth.requireMemberAuthentication(force = true, action = resume) },
            )
        }
        if (authState.requiresNickname) {
            val nickname = com.chaekchaek.app.ui.input.rememberLimitedTextFieldState(
                com.chaekchaek.app.ui.archive.NicknameInputPolicy,
            )
            com.chaekchaek.app.ui.archive.NicknameDialog(
                nicknameState = nickname,
                onDismiss = null,
                saving = authState.savingNickname,
                onConfirm = { auth.saveNickname(nickname.text.toString()) },
            )
        }
        val member = memberSession
        val active = sources.states.map { owner ->
            key(owner) {
                val notice by owner.notice.collectAsState()
                val currentNotice = notice
                val availableNotice = currentNotice?.forMember(member?.requestKey, member?.generation)
                if (currentNotice != null && availableNotice == null) {
                    LaunchedEffect(currentNotice.id) { owner.dismiss(currentNotice.id) }
                }
                owner to availableNotice
            }
        }.lastOrNull { (owner, notice) ->
            notice != null && (authenticating == null || owner === auth.errors)
        }?.let { (owner, notice) -> owner to requireNotNull(notice) }
            ?: expiryNotice?.takeIf { authenticating == null && requiresLogin }
                ?.forMember(member?.requestKey, member?.generation)?.let { sessionExpiry to it }
        active?.let { (owner, notice) ->
            ChaekErrorDialog(notice, { owner.dismiss(notice.id) }) {
                val currentMember = auth.memberSession.value
                when (notice.forMember(currentMember?.requestKey, currentMember?.generation)?.action) {
                    ErrorAction.Login -> {
                        if (owner !== sessionExpiry) sessionExpiry.dismiss()
                        authenticating = AuthenticationRecovery(
                            owner, notice.id, notice.memberRequestKey ?: currentMember?.requestKey,
                        )
                    }
                    ErrorAction.Retry, ErrorAction.Confirm -> owner.takeRecovery(notice.id)
                        ?.invoke(currentMember?.requestKey.takeIf { notice.memberRequestKey != null })
                    null -> owner.dismiss(notice.id)
                }
            }
        }
    }
}

private data class AuthenticationRecovery(
    val owner: ErrorRecoveryState,
    val noticeId: Long,
    val requestKey: String?,
)

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
