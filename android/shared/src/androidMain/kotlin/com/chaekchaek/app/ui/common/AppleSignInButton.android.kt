package com.chaekchaek.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.google_g_logo

@Composable
internal actual fun AppleSignInButton(
    signingIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) = Unit

@Composable
internal actual fun GoogleSignInButton(
    signingIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    androidx.compose.material3.Surface(
        onClick = onClick, enabled = !signingIn,
        modifier = modifier.then(Modifier.height(48.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = androidx.compose.ui.graphics.Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF8E918F)),
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterHorizontally),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                org.jetbrains.compose.resources.painterResource(chaekchaek.shared.generated.resources.Res.drawable.google_g_logo),
                contentDescription = null, modifier = Modifier.size(18.dp),
            )
            androidx.compose.material3.Text("Google로 계속하기", color = androidx.compose.ui.graphics.Color(0xFF171717),
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 17.sp)
        }
    }
}
