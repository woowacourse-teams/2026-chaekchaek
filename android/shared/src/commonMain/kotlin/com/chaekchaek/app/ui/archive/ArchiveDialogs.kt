package com.chaekchaek.app.ui.archive

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.domain.shelf.ReadingStatus
import com.chaekchaek.app.domain.reader.Nickname
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.theme.ChaekAccent

import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens

@Composable
internal fun StatusChangeDialog(selectedCount: Int, onDismiss: () -> Unit, onChange: (ReadingStatus) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(ReadingStatus.READING) }
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("독서 상태 변경", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "선택한 ${selectedCount}권의 상태를 변경합니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReadingStatus.entries.forEach { status ->
                        StatusOptionRow(status = status, selected = selected == status, onClick = { selected = status })
                    }
                }
            }
        },
        dismissButton = { DialogDismissButton(onDismiss) },
        confirmButton = { DialogConfirmButton(label = "변경", onClick = { onChange(selected) }) },
    )
}

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
private fun DialogConfirmButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    ChaekOverlayButton(label, onClick, enabled = enabled)
}

@Composable
private fun StatusOptionRow(status: ReadingStatus, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = ChaekOverlayTokens.inputShape,
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(ChaekAccent, CircleShape))
        }
        Text(
            status.label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
internal fun NicknameDialog(
    nicknameState: TextFieldState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val nickname = nicknameState.text.toString()
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("닉네임 설정", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "기록과 감상에 표시할 닉네임이 필요해요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("닉네임", style = MaterialTheme.typography.labelMedium)
                NicknameInput(nicknameState)
                Text("공백이 아닌 최대 10자", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = { DialogDismissButton(onDismiss) },
        confirmButton = {
            DialogConfirmButton(
                label = "확인",
                onClick = onConfirm,
                enabled = Nickname.isValid(nickname.trim()),
            )
        },
    )
}

@Composable
private fun NicknameInput(nicknameState: TextFieldState) {
    val nickname = nicknameState.text.toString()
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).background(MaterialTheme.colorScheme.surfaceVariant, ChaekOverlayTokens.inputShape).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            state = nicknameState,
            modifier = Modifier.weight(1f),
            inputTransformation = InputTransformation.maxLength(Nickname.MAX_LENGTH),
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
