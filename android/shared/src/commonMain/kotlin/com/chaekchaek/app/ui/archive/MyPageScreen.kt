package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.common.avatarResource
import com.chaekchaek.app.ui.common.ChaekCloseButton
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekAccent
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MyPageScreen(
    state: MemberSettingsUiState,
    onBack: () -> Unit,
    onAnonymousReviewsChange: (Boolean, String) -> Unit,
    onSignOut: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showWithdrawalDialog by remember { mutableStateOf(false) }
    val nicknameState = rememberTextFieldState()

    LaunchedEffect(state.nickname) {
        nicknameState.setTextAndPlaceCursorAtEnd(state.nickname)
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ChaekOverlayTokens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MyPageTopBar(onBack)
            Box(
                Modifier.size(30.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.publicNickname.take(1), style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
            }
            Text(state.publicNickname, style = MaterialTheme.typography.titleLarge, fontSize = 23.sp)
            Text("공개 프로필", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, fontSize = 13.sp)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionLabel("공개 설정")
            AnonymousSetting(
                checked = state.anonymousReviews,
                nickname = state.nickname,
                onClick = {
                    when {
                        !state.anonymousReviews -> onAnonymousReviewsChange(true, "")
                        state.nickname.isBlank() -> showNicknameDialog = true
                        else -> onAnonymousReviewsChange(false, state.nickname)
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionLabel("계정 관리")
            ChaekOverlayButton("로그아웃", onSignOut, secondary = true)
            if (state.withdrawalFailure != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = ChaekOverlayTokens.cardShape) {
                    Text(state.withdrawalFailure.message(), Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                ChaekOverlayButton("다시 시도", onWithdraw, enabled = !state.withdrawing)
            } else {
                WithdrawalRow { showWithdrawalDialog = true }
            }
        }
        if (state.showLoading || state.withdrawing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(32.dp).semantics {
                    contentDescription = if (state.withdrawing) "회원 탈퇴 처리 중" else "회원 정보를 불러오는 중"
                },
                strokeWidth = 2.dp,
            )
        }
    }

    if (showNicknameDialog) {
        NicknameDialog(
            nicknameState = nicknameState,
            onDismiss = { showNicknameDialog = false },
            onConfirm = {
                onAnonymousReviewsChange(false, nicknameState.text.toString().trim())
                showNicknameDialog = false
            },
        )
    }
    if (showWithdrawalDialog) {
        WithdrawalDialog(
            onDismiss = { showWithdrawalDialog = false },
            onConfirm = {
                showWithdrawalDialog = false
                onWithdraw()
            },
        )
    }
}

private fun WithdrawalFailure.message(): String =
    when (this) {
        WithdrawalFailure.AuthenticationExpired -> "로그인이 만료됐어요. 로그아웃한 뒤 다시 로그인해 주세요."
        WithdrawalFailure.NetworkUnavailable -> "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        WithdrawalFailure.ServerRejected -> "회원 탈퇴에 실패했어요. 잠시 후 다시 시도해 주세요."
    }

@Composable
private fun MyPageTopBar(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("마이페이지", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontSize = 21.sp)
        ChaekCloseButton(onBack)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, fontSize = 12.sp)
}

@Composable
private fun AnonymousSetting(checked: Boolean, nickname: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                .clip(ChaekOverlayTokens.inputShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (checked) "●" else "○", color = if (checked) ChaekAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = ChaekIconFontFamily(), fontSize = 20.sp,
                modifier = Modifier.clearAndSetSemantics {})
            Text("익명으로 감상 공개", style = MaterialTheme.typography.bodySmall, fontSize = 14.sp)
        }
        Text(
            when {
                checked && nickname.isNotBlank() -> "해제하면 기존 닉네임으로 공개됩니다"
                checked -> "해제하면 닉네임을 설정해야 합니다"
                else -> "닉네임이 감상에 표시됩니다"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun WithdrawalRow(onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ChaekOverlayButton("회원 탈퇴", onClick, secondary = true)
        Text("계정과 관련 데이터가 삭제됩니다", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall, fontSize = 13.sp)
    }
}

@Composable
private fun WithdrawalDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("회원 탈퇴") },
        text = { Text("탈퇴하면 계정과 관련 데이터가 삭제되며 되돌릴 수 없습니다. 정말 탈퇴할까요?") },
        dismissButton = { ChaekOverlayButton("취소", onDismiss, secondary = true) },
        confirmButton = { ChaekOverlayButton("탈퇴하기", onConfirm) },
    )
}

@Composable
internal fun MemberAvatar(displayName: String, size: Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Image(
            painter = painterResource(avatarResource(displayName)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
