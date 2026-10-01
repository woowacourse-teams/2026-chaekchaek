package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.gowun_dodum_regular
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import org.jetbrains.compose.resources.Font

@Composable
internal fun LibraryBookSpine(
    book: ArchiveBookUiModel,
    modifier: Modifier = Modifier,
    showGeneratedSpine: Boolean = true,
) {
    var imageFailed by remember(book.spineUrl) { mutableStateOf(false) }
    if (book.spineUrl.isNullOrBlank() || imageFailed) {
        if (showGeneratedSpine) GeneratedBookSpine(book, modifier.width(30.dp))
    } else {
        AsyncImage(
            model = book.spineUrl,
            contentDescription = "${book.title} 책등",
            modifier = modifier,
            contentScale = ContentScale.Fit,
            onError = { imageFailed = true },
        )
    }
}

@Composable
private fun GeneratedBookSpine(book: ArchiveBookUiModel, modifier: Modifier) {
    val context = LocalPlatformContext.current
    val request = remember(context, book.coverUrl) {
        ImageRequest.Builder(context).data(book.coverUrl).size(48, 48).readableCover().build()
    }
    val painter = rememberAsyncImagePainter(request)
    val state by painter.state.collectAsState()
    var color by remember(book.coverUrl) { mutableStateOf(NeutralSpineColor) }
    LaunchedEffect(state) {
        val success = state as? AsyncImagePainter.State.Success ?: return@LaunchedEffect
        color = runCatching {
            val bitmap = ImageBitmap(48, 48)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(48f, 48f)) {
                with(success.painter) { draw(size) }
            }
            val pixels = bitmap.toPixelMap()
            dominantCoverColor(List(48 * 48) { index -> pixels[index % 48, index / 48] })
        }.getOrDefault(NeutralSpineColor)
    }
    val font = FontFamily(Font(Res.font.gowun_dodum_regular, FontWeight.Normal))
    val ink = if (color.luminance() > 0.179f) Color(0xFF171713) else Color.White
    val bandColor = lerp(Color(0xFFFFFAF0), color, 0.15f)
    BoxWithConstraints(
        modifier.background(color).clearAndSetSemantics { contentDescription = "${book.title} 책등" },
    ) {
        val bandHeight = maxHeight * 0.235f
        val fontScale = LocalDensity.current.fontScale
        val rowHeight = 13f * fontScale
        val capacity = ((maxHeight.value - bandHeight.value - 24f) / rowHeight).toInt().coerceAtLeast(1)
        val columns = remember(book.title, capacity) { spineTitleColumns(book.title, capacity) }
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            columns.reversed().forEach { letters ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    letters.forEach { letter ->
                        Text(
                            letter.toString(),
                            modifier = Modifier.height(rowHeight.dp),
                            style = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal,
                                fontSize = 10.sp, lineHeight = 12.sp, color = ink),
                        )
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bandHeight).background(bandColor)) {
            Text(
                book.creator.substringBefore(","),
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 3.dp),
                fontFamily = font, fontSize = 6.sp, lineHeight = 8.sp,
                color = lerp(Color.Black, color, 0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    .width(10.dp).height(0.5.dp).background(color.copy(alpha = 0.5f)),
            )
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.06f), Color.Transparent, Color.Black.copy(alpha = 0.06f))),
            ),
        )
    }
}

internal val NeutralSpineColor = Color(0xFFAEA999)

internal fun spineTitleColumns(title: String, capacity: Int): List<List<Char>> {
    val letters = title.filterNot(Char::isWhitespace).toList()
    val limit = capacity.coerceAtLeast(1) * 2
    val visible = if (letters.size > limit) letters.take(limit - 1) + '…' else letters
    return visible.chunked(capacity.coerceAtLeast(1))
}

/** 밝은 종이 바탕과 검은 글자를 제외한 가장 큰 색군을 표지의 대표색으로 사용한다. */
internal fun dominantCoverColor(pixels: List<Color>): Color {
    val groups = mutableMapOf<Int, MutableList<Color>>()
    pixels.filter { it.alpha > 0.5f }.forEach { color ->
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        if (max - min >= 24 && !(max > 244 && min > 215) && max >= 28) {
            val key = ((r / 32) shl 6) or ((g / 32) shl 3) or (b / 32)
            groups.getOrPut(key) { mutableListOf() }.add(color)
        }
    }
    val colors = groups.values.maxByOrNull { it.size }
        ?: pixels.filter { it.alpha > 0.5f }.takeIf { it.isNotEmpty() }
        ?: return NeutralSpineColor
    return Color(
        colors.map { it.red }.average().toFloat(),
        colors.map { it.green }.average().toFloat(),
        colors.map { it.blue }.average().toFloat(),
    )
}

internal expect fun ImageRequest.Builder.readableCover(): ImageRequest.Builder
