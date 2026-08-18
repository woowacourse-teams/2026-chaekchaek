package com.chamsae.chaekchaek.ui.bookdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.chamsae.chaekchaek.R

internal fun validProgressPage(input: String, totalPages: Int): Int? =
  input.toIntOrNull()?.takeIf { it in 0..totalPages }

@Composable
internal fun SpoilerGuardDialog(
  currentPage: Int,
  spoilerPage: Int,
  totalPages: Int,
  onDismiss: () -> Unit,
  onUpdateAndRead: (Int) -> Unit,
  onReadAnyway: () -> Unit,
) {
  var input by rememberSaveable(currentPage) { mutableStateOf(currentPage.toString()) }
  val page = remember(input, totalPages) { validProgressPage(input, totalPages) }
  val invalid = input.isNotBlank() && page == null

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(0.4f) }
    Box(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier.widthIn(max = 330.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              "어디까지 읽으셨나요?",
              modifier = Modifier.weight(1f),
              style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
            )
            IconButton(onClick = onDismiss) {
              Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "닫기",
              )
            }
          }
          Text(
            "이 감상은 ${spoilerPage}쪽 이후 내용을 포함해요. 내가 읽은 쪽수를 입력하면 읽은 범위까지 안전하게 볼 수 있어요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
          )
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("내가 읽은 쪽수", style = MaterialTheme.typography.labelMedium)
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                  .border(
                    1.dp,
                    if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    RoundedCornerShape(4.dp),
                  )
                  .padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.width(48.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle =
                  MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                  ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
              )
              Text("쪽", style = MaterialTheme.typography.bodySmall)
              Spacer(Modifier.weight(1f))
              Text("/ ${totalPages}쪽", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (invalid) {
              Text(
                "0에서 ${totalPages} 사이의 쪽수를 입력해 주세요.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
          GuardButton(
            label = "입력한 쪽수까지 보기",
            enabled = page != null,
            onClick = { page?.let(onUpdateAndRead) },
          )
          GuardButton(
            label = "스포일러 감수하고 보기",
            danger = true,
            onClick = onReadAnyway,
          )
        }
      }
    }
  }
}

@Composable
private fun GuardButton(
  label: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
  danger: Boolean = false,
) {
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().height(48.dp),
    enabled = enabled,
    shape = RoundedCornerShape(4.dp),
    border = if (danger) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        contentColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.outline,
        disabledContentColor = MaterialTheme.colorScheme.surface,
      ),
  ) {
    Text(label, style = MaterialTheme.typography.labelLarge)
  }
}
