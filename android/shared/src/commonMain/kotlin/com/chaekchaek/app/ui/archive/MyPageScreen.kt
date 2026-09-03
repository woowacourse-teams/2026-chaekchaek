package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.common.avatarResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MyPageScreen(
    state: MemberSettingsUiState,
    onBack: () -> Unit,
    onAnonymousReviewsChange: (Boolean, String) -> Unit,
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
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MyPageTopBar(onBack)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MemberAvatar(state.publicNickname, 80.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        state.publicNickname,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        "공개 프로필",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                WithdrawalRow { showWithdrawalDialog = true }
                state.withdrawalErrorMessage?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = onWithdraw) { Text("다시 시도") }
                    }
                }
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

internal val MemberSettingsUiState.publicNickname: String
    get() = (if (anonymousReviews) anonymousNickname else nickname).ifBlank { "책책이" }

@Composable
private fun MyPageTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                "‹",
                modifier = Modifier.clearAndSetSemantics { contentDescription = "뒤로가기" },
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Text("마이페이지", style = MaterialTheme.typography.headlineSmall)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
}

@Composable
private fun AnonymousSetting(checked: Boolean, nickname: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp).background(
                if (checked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(4.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                val checkColor = MaterialTheme.colorScheme.surface
                Canvas(Modifier.size(12.dp)) {
                    drawLine(
                        checkColor,
                        Offset(0f, size.height * 0.55f),
                        Offset(size.width * 0.35f, size.height),
                        2.dp.toPx(),
                        StrokeCap.Round,
                    )
                    drawLine(
                        checkColor,
                        Offset(size.width * 0.35f, size.height),
                        Offset(size.width, 0f),
                        2.dp.toPx(),
                        StrokeCap.Round,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("익명으로 감상 공개", style = MaterialTheme.typography.titleSmall)
            Text(
                when {
                    checked && nickname.isNotBlank() -> "해제하면 기존 닉네임으로 공개됩니다"
                    checked -> "해제하면 닉네임을 설정해야 합니다"
                    else -> "닉네임이 감상에 표시됩니다"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WithdrawalRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "⌫",
            modifier = Modifier.clearAndSetSemantics {},
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleLarge,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("회원 탈퇴", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
            Text(
                "계정과 관련 데이터가 삭제됩니다",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            "›",
            modifier = Modifier.clearAndSetSemantics {},
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun WithdrawalDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("회원 탈퇴") },
        text = { Text("탈퇴하면 계정과 관련 데이터가 삭제되며 되돌릴 수 없습니다. 정말 탈퇴할까요?") },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("취소") }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("탈퇴하기") }
        },
    )
}

@Composable
internal fun MemberAvatar(displayName: String, size: Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Image(
            painter = painterResource(avatarResource(displayName)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
