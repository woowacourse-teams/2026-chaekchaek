package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.chaekchaek.app.data.remote.BookshelfBook
import com.chaekchaek.app.data.remote.BookshelfDetail
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import com.chaekchaek.app.ui.common.*
import com.chaekchaek.app.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BookshelfImageSheet(detail: BookshelfDetail, onDismiss: () -> Unit) {
    val platform = rememberBookshelfPlatform()
    val errors = remember { ErrorRecoveryState() }
    val scope = rememberCoroutineScope()
    var percent by remember { mutableIntStateOf(140) }
    var exporting by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val readyBooks = remember(detail) { mutableStateMapOf<Long, Boolean>() }
    val layers = remember(detail) { mutableMapOf<Int, GraphicsLayer>() }
    val ready = detail.items.all { readyBooks[it.bookId] == true }
    ErrorRecoveryEffect(errors)

    fun export(save: Boolean) {
        if (exporting || !ready) return
        // Lock synchronously so another tap cannot launch a second capture before the coroutine starts.
        exporting = true
        saved = false
        scope.launch {
            try {
                withDelayedApiLoading({ showLoading = it }) {
                    // Record the selected scale and loaded fallback colours before capturing.
                    withFrameNanos { }
                    withFrameNanos { }
                    val bitmap = captureShelfSections(layers.entries.sortedBy { it.key }.map { it.value })
                    if (save) saved = platform.save(bitmap) else platform.share(bitmap)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                errors.report(error, ErrorOperation.ImageExport) { export(save) }
            } finally {
                exporting = false
            }
        }
    }

    ShelfSheet("책장을 이미지로", onDismiss, dismissEnabled = !exporting) {
        BookshelfImageScale(percent, enabled = !exporting) { percent = it }
        Box(Modifier.fillMaxWidth().heightIn(max = 380.dp).background(ChaekSurfaceMuted)
            .verticalScroll(rememberScrollState(), enabled = !exporting)) {
            Column(Modifier.fillMaxWidth().background(ChaekSurface).testTag("bookshelf-image-preview")) {
                ImageSection(0, layers) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(detail.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        Text("책췍", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("${detail.totalCount}권", style = MaterialTheme.typography.bodySmall)
                }
                ImageSection(1, layers) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        detail.items.forEachIndexed { index, book ->
                            ExportSpine(book, index, percent / 100f, Modifier.align(Alignment.Bottom)) {
                                readyBooks[book.bookId] = true
                            }
                        }
                    }
                }
            }
        }
        if (saved) Text("이미지 저장을 완료했어요", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChaekOverlayButton("이미지 저장", { export(true) }, enabled = ready && !exporting, secondary = true, modifier = Modifier.weight(1f))
            ChaekOverlayButton("이미지 공유", { export(false) }, enabled = ready && !exporting, modifier = Modifier.weight(1f).testTag("bookshelf-image-share"))
        }
        if (exporting) ExportInputBlocker(showLoading)
    }
}

@Composable
private fun ExportSpine(book: BookshelfBook, index: Int, scale: Float, modifier: Modifier, onReady: () -> Unit) {
    val painter = rememberAsyncImagePainter(book.spineImageUrl)
    val state by painter.state.collectAsState()
    val ready by rememberUpdatedState(onReady)
    val height = bookshelfSpineHeight(index, scale)
    if (book.spineImageUrl.isNullOrBlank() || state is AsyncImagePainter.State.Error) {
        // Reuse the same generated spine used in the owner's shelf, including its cover colour.
        LibraryBookSpine(book.asArchive().copy(spineUrl = null), modifier.width(30.dp * (scale / 1.4f)).height(height), onReady = { ready() })
    } else {
        val intrinsic = painter.intrinsicSize
        val ratio = if (intrinsic.width.isFinite() && intrinsic.height.isFinite() && intrinsic.width > 0 && intrinsic.height > 0) {
            intrinsic.width / intrinsic.height
        } else 30f / height.value
        Image(painter, "${book.title} 책등", modifier.width(height * ratio).height(height), contentScale = ContentScale.Fit)
        LaunchedEffect(state) { if (state is AsyncImagePainter.State.Success) ready() }
    }
}

/** Figma ExportBlockingProgress: input blocks immediately; the shared ring appears after 500ms. */
@Composable
private fun ExportInputBlocker(showLoading: Boolean) {
    Popup(properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Box(Modifier.fillMaxSize().testTag("bookshelf-export-busy")
            .background(if (showLoading) Color.Black.copy(alpha = .25f) else Color.Transparent)
            .semantics { contentDescription = "책장 이미지 준비 중" }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }, contentAlignment = Alignment.Center) {
            if (showLoading) ChaekInlineProgress()
        }
    }
}

@Composable
private fun ImageSection(index: Int, layers: MutableMap<Int, GraphicsLayer>, content: @Composable ColumnScope.() -> Unit) {
    val layer = rememberGraphicsLayer()
    DisposableEffect(layer) { layers[index] = layer; onDispose { layers.remove(index) } }
    Column(Modifier.fillMaxWidth().drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }.background(ChaekSurface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

private suspend fun captureShelfSections(layers: List<GraphicsLayer>): ImageBitmap {
    check(layers.isNotEmpty() && layers.all { it.size.width > 0 && it.size.height > 0 })
    val image = ImageBitmap(layers.maxOf { it.size.width }, layers.sumOf { it.size.height })
    val canvas = Canvas(image)
    var y = 0f
    for (layer in layers) {
        val part = layer.toImageBitmap().forSoftwareCanvas()
        canvas.drawImage(part, Offset(0f, y), Paint())
        y += part.height
    }
    return image
}
