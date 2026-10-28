package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.ui.theme.ChaekActionSurface
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import kotlin.math.roundToInt

internal fun snapBookshelfImageScale(value: Float): Int =
    ((value.coerceIn(100f, 200f) - 100f) / 5f).roundToInt() * 5 + 100

/** Figma ExportSizeSlider 1330:3402: 4dp track and 18dp thumb in a 44dp touch area. */
@Composable
internal fun BookshelfImageScale(percent: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val change by rememberUpdatedState(onChange)
    val trackColor = ChaekSurfaceMuted
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("책등 크기", style = MaterialTheme.typography.bodyMedium)
            Text("$percent%", style = MaterialTheme.typography.bodyMedium)
        }
        Canvas(Modifier.fillMaxWidth().height(44.dp).testTag("bookshelf-image-scale")
            .semantics {
                contentDescription = "책등 이미지 배율"
                progressBarRangeInfo = ProgressBarRangeInfo(percent.toFloat(), 100f..200f, 19)
                if (enabled) setProgress { value -> change(snapBookshelfImageScale(value)); true }
            }
            .pointerInput(enabled) {
                if (enabled) detectTapGestures { point ->
                    change(snapBookshelfImageScale(100f + 100f * point.x / size.width.coerceAtLeast(1)))
                }
            }
            .pointerInput(enabled) {
                if (enabled) detectDragGestures(onDragStart = { point ->
                    change(snapBookshelfImageScale(100f + 100f * point.x / size.width.coerceAtLeast(1)))
                }) { event, _ ->
                    event.consume()
                    change(snapBookshelfImageScale(100f + 100f * event.position.x / size.width.coerceAtLeast(1)))
                }
            }) {
            val radius = 9.dp.toPx()
            val trackHeight = 4.dp.toPx()
            val trackWidth = size.width
            val filled = trackWidth * (percent - 100) / 100f
            val top = (size.height - trackHeight) / 2
            drawRoundRect(trackColor, Offset(0f, top), Size(trackWidth, trackHeight), CornerRadius(trackHeight / 2))
            drawRoundRect(ChaekActionSurface, Offset(0f, top), Size(filled, trackHeight), CornerRadius(trackHeight / 2))
            drawCircle(ChaekActionSurface, radius, Offset(filled, size.height / 2))
        }
    }
}
