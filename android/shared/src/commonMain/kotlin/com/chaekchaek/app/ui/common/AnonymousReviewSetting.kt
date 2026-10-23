package com.chaekchaek.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens

/** 마이페이지와 감상 작성에서 같은 공개 설정과 안내를 표시한다. */
@Composable
fun AnonymousReviewSetting(
    checked: Boolean,
    nickname: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(
            modifier = modifier.fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    stateDescription = if (checked) "익명" else "닉네임 공개"
                }.heightIn(min = 50.dp)
                .clip(ChaekOverlayTokens.inputShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .toggleable(value = checked, enabled = enabled, role = Role.Checkbox,
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onValueChange = { onClick() })
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

