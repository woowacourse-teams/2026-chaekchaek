package com.chaekchaek.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.ic_chevron_down
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import org.jetbrains.compose.resources.painterResource

@Composable
fun ChaekFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(FilterChipShape)
                .background(if (selected) ChaekInk else ChaekSurfaceMuted)
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.surface else FilterInactiveText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            )
        }
    }
}

@Composable
fun <T> ChaekDropdown(
    selectedOption: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clip(DropdownButtonShape)
                .border(1.dp, ChaekBorder, DropdownButtonShape)
                .clickable(enabled = enabled, role = Role.Button) { expanded = true }
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = optionLabel(selectedOption),
                color = if (enabled) ChaekInk else ChaekInkSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Icon(
                painter = painterResource(Res.drawable.ic_chevron_down),
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
                tint = if (enabled) ChaekInk else ChaekInkSecondary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, ChaekBorder),
        ) {
            options.forEach { option ->
                val selected = option == selectedOption
                DropdownMenuItem(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                    text = {
                        Text(
                            text = optionLabel(option),
                            color = ChaekInk,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                    },
                    onClick = {
                        expanded = false
                        onOptionSelected(option)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            }
        }
    }
}

private val FilterChipShape = RoundedCornerShape(20.dp)
private val DropdownButtonShape = RoundedCornerShape(50)
private val FilterInactiveText = Color(0xFF707070)
