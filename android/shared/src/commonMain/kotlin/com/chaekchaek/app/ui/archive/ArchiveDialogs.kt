package com.chaekchaek.app.ui.archive

import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import com.chaekchaek.app.ui.input.TextInputPolicy
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.domain.reader.Nickname
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.theme.ChaekAccent

import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens


@Composable
internal fun DeleteConfirmationDialog(selectedCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("책 삭제", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                "선택한 ${selectedCount}권을 서재에서 삭제할까요? 삭제한 책은 다시 복구할 수 없어요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        dismissButton = { DialogDismissButton(onDismiss) },
        confirmButton = { DialogConfirmButton(label = "삭제", onClick = onConfirm) },
    )
}

@Composable
private fun DialogDismissButton(onClick: () -> Unit) {
    ChaekOverlayButton("취소", onClick, secondary = true)
}

@Composable
private fun DialogConfirmButton(label: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    ChaekOverlayButton(label, onClick, enabled = enabled, modifier = modifier)
}


@Composable
internal fun NicknameDialog(
    nicknameState: TextFieldState,
    onDismiss: (() -> Unit)?,
    onConfirm: () -> Unit,
    saving: Boolean = false,
) {
    val nickname = nicknameState.text.toString()
    val errorReporter = com.chaekchaek.app.analytics.LocalAnalyticsTracker.current.errors
    val errors = remember(errorReporter) { com.chaekchaek.app.presentation.common.ErrorRecoveryState(errorReporter) }
    com.chaekchaek.app.ui.common.ErrorRecoveryEffect(errors)
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        dismissButton = if (onDismiss == null) null else ({ DialogDismissButton(onDismiss) }),
        title = { Text("닉네임 설정", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "기록과 감상에 표시할 닉네임이 필요해요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("닉네임", style = MaterialTheme.typography.labelMedium)
                NicknameInput(nicknameState, readOnly = saving)
                Text("공백이 아닌 최대 10자", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            DialogConfirmButton(
                label = if (saving) "저장 중" else "확인",
                modifier = Modifier.semantics { contentDescription = if (saving) "저장 중" else "확인" },
                enabled = !saving,
                onClick = {
                    if (Nickname.isValid(nickname.trim())) onConfirm()
                    else errors.confirm("공백이 아닌 최대 10자의 닉네임을 입력해 주세요.")
                },
            )
        },
    )
}

internal val NicknameInputPolicy = TextInputPolicy(Nickname.MAX_LENGTH, utf16 = true)

@Composable
private fun NicknameInput(nicknameState: TextFieldState, readOnly: Boolean = false) {
    val nickname = nicknameState.text.toString()
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).background(MaterialTheme.colorScheme.surfaceVariant, ChaekOverlayTokens.inputShape).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            state = nicknameState,
            readOnly = readOnly,
            modifier = Modifier.weight(1f).semantics { contentDescription = "닉네임 입력" },
            inputTransformation = NicknameInputPolicy.inputTransformation,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorator = { innerTextField ->
                Box {
                    if (nickname.isEmpty()) {
                        Text(
                            "닉네임을 입력하세요",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Text(
            "${nickname.length}/10",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
        )
    }
}
