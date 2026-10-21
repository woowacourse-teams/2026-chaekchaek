package com.chaekchaek.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted

/** Saved 상태도 탭을 소비하여 바깥 카드 이동이나 삭제로 이어지지 않는다. */
@Composable
fun LibrarySaveButton(
    bookTitle: String,
    saved: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saving: Boolean = false,
    showLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val label = when {
        saved -> "담았어요"
        saving -> "담는 중"
        else -> "서재에 담기"
    }
    Row(
        modifier = modifier.heightIn(min = 44.dp)
            .alpha(if (!enabled && !saved && !saving) 0.45f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (saved) ChaekAccent else ChaekSurfaceMuted)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) { if (!saved && !saving && enabled) onSave() }
            .semantics(mergeDescendants = true) {
                contentDescription = "$bookTitle, $label"
                stateDescription = if (saved) "서재에 저장됨" else if (saving) "저장 중" else "저장되지 않음"
                if (!saved && (!enabled || saving)) disabled()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLoading && saving) {
            CircularProgressIndicator(Modifier.size(16.dp), color = ChaekInk, strokeWidth = 2.dp)
        } else {
            Text(if (saved) "✓" else "+", fontFamily = ChaekIconFontFamily(), color = ChaekInk,
                style = MaterialTheme.typography.labelMedium)
        }
        Text(label, modifier = Modifier.weight(1f, fill = false), color = ChaekInk,
            style = MaterialTheme.typography.labelMedium)
    }
}
