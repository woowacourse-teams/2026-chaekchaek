package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chaekchaek.app.data.remote.*
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.ui.common.*
import com.chaekchaek.app.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.chaekchaek.app.presentation.common.withDelayedApiLoading

/** 소유자 순서대로 각 책의 최신 본인 감상 한 개만 조회한다. 빈 감상은 null로 유지한다. */
internal suspend fun latestShelfReviews(detail: BookshelfDetail, token: String, repository: BookDetailRemoteRepository): Map<Long, BookReview?> {
    val result = linkedMapOf<Long, BookReview?>()
    for (book in detail.items) {
        var next: Int? = 1
        var review: BookReview? = null
        val seen = mutableSetOf<Int>()
        while (next != null && review == null && seen.add(next)) {
            val page = repository.reviews(book.bookId, ReviewScope.MINE, ReviewSort.LATEST, WriteCredential.Member(token), next)
            review = page.items.firstOrNull { !it.deleted }
            next = page.nextPage
        }
        result[book.bookId] = review
    }
    return result
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BookshelfImageSheet(detail: BookshelfDetail, token: String, onDismiss: () -> Unit) {
    val repository = remember { BookDetailRemoteRepository() }
    val platform = rememberBookshelfPlatform()
    val errors = remember { ErrorRecoveryState() }
    val scope = rememberCoroutineScope()
    var reviews by remember(detail) { mutableStateOf<Map<Long, BookReview?>?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1.4f) }
    var exporting by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val readyImages = remember(detail) { mutableStateMapOf<String, Boolean>() }
    val imageUrls = remember(detail) { detail.items.flatMap { listOfNotNull(it.coverImageUrl.takeIf(String::isNotBlank), it.spineImageUrl?.takeIf(String::isNotBlank)) }.toSet() }
    val layers = remember(detail) { mutableMapOf<Int, GraphicsLayer>() }
    ErrorRecoveryEffect(errors)
    LaunchedEffect(detail, token, revision) {
        try { reviews = withDelayedApiLoading({ showLoading = it }) { latestShelfReviews(detail, token, repository) } }
        catch (e: CancellationException) { throw e }
        catch (e: Throwable) { errors.report(e, ErrorOperation.Request) { revision++ } }
    }
    val export: (Boolean) -> Unit = { save ->
        if (!exporting && reviews != null && imageUrls.all { readyImages[it] == true }) scope.launch {
            exporting = true
            try {
                val bitmap = captureShelfSections(layers.entries.sortedBy { it.key }.map { it.value })
                if (save) saved = platform.save(bitmap) else platform.share(bitmap)
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) { errors.report(e, ErrorOperation.Request) }
            finally { exporting = false }
        }
    }
    ShelfSheet("책장을 이미지로", { if (!exporting) onDismiss() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("배율 ${(scale * 100).toInt()}%")
            Slider(scale, { scale = it }, valueRange = 1f..2f, steps = 19, modifier = Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().heightIn(max = 380.dp).background(ChaekSurfaceMuted).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().background(ChaekSurface)) {
                ImageSection(0, layers) {
                Row(Modifier.fillMaxWidth()) {
                    Text(detail.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Text("책췍", style = MaterialTheme.typography.titleMedium)
                }
                Text("${detail.totalCount}권", style = MaterialTheme.typography.bodySmall)
                }
                ImageSection(1, layers) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    detail.items.forEach { book ->
                        if (!book.spineImageUrl.isNullOrBlank()) ExportImage(book.spineImageUrl, book.title, Modifier.size((24 * scale).dp, (160 * scale).dp), readyImages)
                        else Box(Modifier.size((24 * scale).dp, (160 * scale).dp).background(ChaekSurfaceMuted).padding(3.dp)) {
                            Text(book.title, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                }
                detail.items.forEachIndexed { index, book ->
                    ImageSection(index + 2, layers) {
                    HorizontalDivider(color = ChaekBorder)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExportImage(book.coverImageUrl, "${book.title} 표지", Modifier.size(54.dp, 78.dp), readyImages)
                        Column(Modifier.weight(1f)) { Text(book.title); Text(book.authors.joinToString(", "), style = MaterialTheme.typography.bodySmall) }
                    }
                    val review = reviews?.get(book.bookId)
                    Box(Modifier.fillMaxWidth().heightIn(min = 100.dp).background(ChaekSurfaceMuted, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        if (review != null) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            review.quote?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(review.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    }
                }
            }
        }
        val ready = reviews != null && imageUrls.all { readyImages[it] == true } && !exporting
        if (showLoading || exporting) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (saved) Text("이미지 저장을 완료했어요", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChaekOverlayButton("이미지 저장", { export(true) }, enabled = ready, secondary = true, modifier = Modifier.weight(1f))
            ChaekOverlayButton("이미지 공유", { export(false) }, enabled = ready, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExportImage(url: String?, description: String, modifier: Modifier, ready: MutableMap<String, Boolean>) {
    if (url.isNullOrBlank()) Box(modifier.background(ChaekSurfaceMuted)) else AsyncImage(
        url, description, modifier, contentScale = ContentScale.Fit,
        onSuccess = { ready[url] = true }, onError = { ready[url] = true },
    )
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

/** 긴 책장 전체를 한 GPU 텍스처에 올리지 않고 책별 캡처를 순서대로 합친다. */
private suspend fun captureShelfSections(layers: List<GraphicsLayer>): ImageBitmap {
    check(layers.isNotEmpty() && layers.all { it.size.width > 0 && it.size.height > 0 })
    val image = ImageBitmap(layers.maxOf { it.size.width }, layers.sumOf { it.size.height })
    val canvas = Canvas(image)
    var y = 0f
    for (layer in layers) {
        val part = layer.toImageBitmap()
        canvas.drawImage(part, Offset(0f, y), Paint())
        y += part.height
    }
    return image
}
