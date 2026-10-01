package com.chaekchaek.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.ic_profile_outline
import chaekchaek.shared.generated.resources.mascot_outline_b
import org.jetbrains.compose.resources.painterResource

@Composable
fun BrandHeader(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(70.dp).padding(horizontal = 28.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.mascot_outline_b),
            contentDescription = null,
            modifier = Modifier.size(46.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            "책췍",
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        )
        Spacer(Modifier.weight(1f))
        Surface(
            onClick = onProfileClick,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_profile_outline),
                    contentDescription = "내 프로필",
                    modifier = Modifier.size(width = 19.dp, height = 23.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
