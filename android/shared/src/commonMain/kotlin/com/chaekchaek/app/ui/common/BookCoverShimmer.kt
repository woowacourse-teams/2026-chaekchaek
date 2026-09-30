package com.chaekchaek.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal const val BookCoverShimmerDelayMillis = 500L
private const val BookCoverShimmerDurationMillis = 1_200

private val BookCoverShimmerBase = Color(0xFFE7E5E0)
private val BookCoverShimmerHighlight = Color.White.copy(alpha = 0.74f)

@Composable
internal fun BookCoverShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "book cover shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = BookCoverShimmerDurationMillis,
                easing = LinearEasing,
            ),
        ),
        label = "book cover shimmer phase",
    )

    Canvas(modifier) {
        drawRect(BookCoverShimmerBase)
        val gradientWidth = size.width
        val centerX = -gradientWidth / 2f + phase * gradientWidth * 2f
        drawRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.34f to Color.Transparent,
                    0.5f to BookCoverShimmerHighlight,
                    0.66f to Color.Transparent,
                    1f to Color.Transparent,
                ),
                start = Offset(centerX - gradientWidth / 2f, 0f),
                end = Offset(centerX + gradientWidth / 2f, 0f),
            ),
        )
    }
}
