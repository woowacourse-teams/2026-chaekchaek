package com.chamsae.chaekchaek.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chamsae.chaekchaek.theme.ChaekInk
import com.chamsae.chaekchaek.theme.ChaekSurface

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LoginRequiredSheet(
  signingIn: Boolean,
  error: String?,
  onDismiss: () -> Unit,
  onGoogleSignIn: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color.White,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("로그인이 필요해요", style = MaterialTheme.typography.titleLarge)
      Text("내 독서 기록을 남기고 감상에 참여하려면 로그인해 주세요.", style = MaterialTheme.typography.bodyMedium)
      error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      Surface(
        onClick = onGoogleSignIn,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = ChaekInk,
      ) {
        Text(
          if (signingIn) "로그인 중..." else "Google로 계속하기",
          modifier = Modifier.padding(vertical = 14.dp),
          color = ChaekSurface,
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}
