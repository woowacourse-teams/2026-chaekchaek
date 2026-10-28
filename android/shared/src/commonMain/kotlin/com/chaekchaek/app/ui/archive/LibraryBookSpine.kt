package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.gowun_dodum_regular
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import org.jetbrains.compose.resources.Font
import kotlin.math.floor

@Composable
internal fun LibraryBookSpine(
    book: ArchiveBookUiModel,
    modifier: Modifier = Modifier,
    fixedHeight: Dp? = null,
    showGeneratedSpine: Boolean = true,
    onSpineImageLoadFailed: (String) -> Unit = {},
    onReady: () -> Unit = {},
    generatedScale: Float = 1f,
) {
    val ready by rememberUpdatedState(onReady)
    val painter = rememberAsyncImagePainter(book.spineUrl)
    val state by painter.state.collectAsState()
    var imageFailed by remember(book.spineUrl) { mutableStateOf(false) }
    val dimensions = fixedHeight?.let { height ->
        val intrinsic = painter.intrinsicSize
        val fallback = 30.dp * generatedScale
        Modifier.width(bookshelfSpineThickness(height, intrinsic.width, intrinsic.height, fallback)).height(height)
    } ?: Modifier
    if (book.spineUrl.isNullOrBlank() || imageFailed) {
        if (showGeneratedSpine) GeneratedBookSpine(
            book,
            modifier.then(dimensions).width(30.dp * generatedScale),
            generatedScale,
            onReady = { ready() },
        )
    } else {
        Image(
            painter = painter,
            contentDescription = "${book.title} 책등",
            modifier = modifier.then(dimensions),
            contentScale = ContentScale.Fit,
        )
        LaunchedEffect(state, book.spineUrl) {
            when (state) {
                is AsyncImagePainter.State.Success -> ready()
                is AsyncImagePainter.State.Error -> {
                    imageFailed = true
                    onSpineImageLoadFailed(book.spineUrl)
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun GeneratedBookSpine(book: ArchiveBookUiModel, modifier: Modifier, scale: Float, onReady: () -> Unit) {
    val ready by rememberUpdatedState(onReady)
    val context = LocalPlatformContext.current
    val request = remember(context, book.coverUrl) {
        ImageRequest.Builder(context).data(book.coverUrl).size(48, 48).readableCover().build()
    }
    val painter = rememberAsyncImagePainter(request)
    val state by painter.state.collectAsState()
    var palette by remember(book.coverUrl) { mutableStateOf(NeutralSpinePalette) }
    LaunchedEffect(state, book.bookId, book.coverUrl) {
        if (book.coverUrl.isBlank() || state is AsyncImagePainter.State.Error) {
            palette = NeutralSpinePalette
            ready()
            return@LaunchedEffect
        }
        val success = state as? AsyncImagePainter.State.Success ?: return@LaunchedEffect
        palette = runCatching {
            val bitmap = ImageBitmap(48, 48)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(48f, 48f)) {
                with(success.painter) { draw(size) }
            }
            val pixels = bitmap.toPixelMap()
            coverSpinePalette(List(48 * 48) { index -> pixels[index % 48, index / 48] }, 48, 48)
        }.getOrDefault(NeutralSpinePalette)
        ready()
    }
    val font = FontFamily(Font(Res.font.gowun_dodum_regular, FontWeight.Normal))
    BoxWithConstraints(
        modifier.background(Brush.verticalGradient(colorStops = palette.stops))
            .clearAndSetSemantics { contentDescription = "${book.title} 책등" },
    ) {
        val spineHeight = maxHeight.value
        val fontScale = LocalDensity.current.fontScale
        val padding = 14f * scale
        val rowHeight = 21f * scale * fontScale
        val glyphWidth = 14f * scale * fontScale
        val capacity = if (maxWidth.value < glyphWidth) 0 else
            floor((spineHeight - padding * 2) / rowHeight).toInt().coerceAtLeast(0)
        val glyphs = remember(book.title, capacity) { spineTitleGlyphs(book.title, capacity) }
        Column(Modifier.align(Alignment.TopCenter).padding(top = padding.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            glyphs.forEachIndexed { index, glyph ->
                val center = (padding + rowHeight * (index + .5f)) / spineHeight
                Text(
                    glyph,
                    modifier = Modifier.width(glyphWidth.dp).height(rowHeight.dp),
                    style = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal,
                        fontSize = (14f * scale).sp, lineHeight = (21f * scale).sp,
                        color = spineGlyphInk(palette.backgroundAt(center))),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

internal expect fun ImageRequest.Builder.readableCover(): ImageRequest.Builder
