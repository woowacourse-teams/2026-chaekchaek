package com.chaekchaek.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import kotlinx.coroutines.launch

@Composable
internal fun LazyListScrollTopButton(listState: LazyListState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val threshold = remember(density) { with(density) { 240.dp.roundToPx() } }
    val visible by remember(listState, threshold) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset >= threshold
        }
    }
    val scope = rememberCoroutineScope()
    if (visible) {
        ScrollTopButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = modifier,
        )
    }
}

@Composable
internal fun ScrollTopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp).shadow(6.dp, CircleShape)
            .semantics { contentDescription = "맨 위로 이동" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("⌃", fontSize = 15.sp, lineHeight = 12.sp, fontFamily = ChaekIconFontFamily())
            Text("TOP", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
