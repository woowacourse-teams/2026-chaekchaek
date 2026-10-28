package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekSurface
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** The host owns the gesture while lazy items are rearranged beneath its floating copy. */
@Composable
internal fun <T> BookshelfReorderList(
    values: List<T>, key: (T) -> Long, enabled: Boolean, state: LazyListState,
    onOrder: (List<Long>, (Boolean) -> Unit) -> Unit, modifier: Modifier = Modifier,
    horizontal: Boolean = false, longPress: Boolean = false, tagPrefix: String = "books",
    header: @Composable () -> Unit = {}, footer: @Composable () -> Unit = {},
    item: @Composable (T, Int, Modifier) -> Unit,
) {
    val sourceIds = values.map(key)
    val session = remember { BookshelfReorder() }
    val submission = remember { BookshelfOrderSubmission() }
    var submittedOrder by remember { mutableStateOf<List<Long>?>(null) }
    var heldId by remember { mutableStateOf<Long?>(null) }
    var preview by remember { mutableStateOf(sourceIds) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var grabOffset by remember { mutableStateOf(Offset.Zero) }
    var heldBounds by remember { mutableStateOf(Rect.Zero) }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    val bounds = remember { mutableMapOf<Long, Rect>() }
    val handles = remember { mutableMapOf<Long, Rect>() }
    val latestIds by rememberUpdatedState(sourceIds)
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOrder by rememberUpdatedState(onOrder)
    val density = LocalDensity.current
    val byId = values.associateBy(key)
    val order = if (heldId != null) preview else if (submittedOrder != null) submission.displayOrder(sourceIds) else sourceIds
    fun movePointer(position: Offset) {
        pointer = position
        val available = bounds.filterKeys { it in preview }.map { (id, rect) ->
            ReorderItemBounds(id, if (horizontal) rect.left else rect.top, if (horizontal) rect.right else rect.bottom)
        }
        reorderTarget(if (horizontal) position.x else position.y, available)?.let {
            if (session.preview(it)) preview = session.candidate
        }
    }
    fun finish(cancelled: Boolean) {
        val original = session.original
        val result = session.finish(cancelled)
        heldId = null
        if (result != null) {
            val request = submission.submit(original, result)
            submittedOrder = submission.order
            latestOrder(result) { success ->
                if (submission.complete(request, success)) submittedOrder = submission.order
            }
        }
    }
    LaunchedEffect(sourceIds, enabled) {
        if (heldId != null && (!enabled || sourceIds != session.original)) finish(true)
        submission.reconcile(sourceIds)
        submittedOrder = submission.order
    }
    DisposableEffect(Unit) { onDispose { session.finish(cancelled = true); submission.clear() } }
    LaunchedEffect(heldId) {
        if (heldId == null) return@LaunchedEffect
        val edge = with(density) { (if (horizontal) 36.dp else 48.dp).toPx() }
        while (heldId != null) {
            val distance = reorderEdgeScroll(
                if (horizontal) pointer.x else pointer.y,
                if (horizontal) viewport.left else viewport.top,
                if (horizontal) viewport.right else viewport.bottom, edge, with(density) { (if (horizontal) 8.dp else 10.dp).toPx() },
            )
            if (distance != 0f && state.scrollBy(distance) != 0f) movePointer(pointer)
            delay(16)
        }
    }
    Box(modifier.onGloballyPositioned { viewport = it.boundsInRoot() }.pointerInput(horizontal, longPress) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!latestEnabled || submittedOrder != null || down.isConsumed) return@awaitEachGesture
            val position = down.position + viewport.topLeft
            val hit = (if (longPress) bounds else handles).entries.firstOrNull { (id, rect) -> id in latestIds && rect.contains(position) }
                ?: return@awaitEachGesture
            val pressed = if (longPress) awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture else down
            val rect = bounds[hit.key] ?: return@awaitEachGesture
            if (!session.begin(latestIds, hit.key)) return@awaitEachGesture
            pressed.consume()
            heldBounds = rect
            pointer = pressed.position + viewport.topLeft
            grabOffset = pointer - rect.topLeft
            preview = session.candidate
            heldId = hit.key
            try {
                var activePointer = pressed.id
                var completed = false
                while (heldId != null) {
                    // Initial runs before the lazy child's scroll recognizer, including a fast first move.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == activePointer } ?: break
                    if (change.isConsumed) break
                    if (!change.pressed) {
                        val replacement = event.changes.firstOrNull { it.pressed }
                        event.changes.forEach { it.consume() }
                        if (replacement == null) { completed = true; break }
                        activePointer = replacement.id
                        movePointer(replacement.position + viewport.topLeft)
                    } else {
                        event.changes.forEach { it.consume() }
                        movePointer(change.position + viewport.topLeft)
                    }
                }
                if (heldId != null) finish(!completed)
            } finally {
                if (heldId != null) finish(true)
            }
        }
    }) {
        @Composable fun lazyItem(id: Long) {
            val value = byId[id] ?: return
            val index = sourceIds.indexOf(id)
            DisposableEffect(id) { onDispose { bounds.remove(id); handles.remove(id) } }
            Box(Modifier.onGloballyPositioned { bounds[id] = it.boundsInRoot() }
                .testTag("bookshelf-drag-source-$tagPrefix-$id")
                .semantics { if (heldId == id) stateDescription = "놓을 위치 미리보기" }
                .alpha(if (heldId == id) .25f else 1f)) {
                item(value, index, Modifier.onGloballyPositioned { handles[id] = it.boundsInRoot() })
            }
        }
        if (horizontal) LazyRow(state = state, modifier = Modifier.fillMaxSize(), userScrollEnabled = heldId == null, verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            items(order, key = { it }) { lazyItem(it) }
        } else LazyColumn(state = state, modifier = Modifier.fillMaxSize(), userScrollEnabled = heldId == null) {
            item(key = "bookshelf-header") { header() }
            items(order, key = { it }) { lazyItem(it) }
            item(key = "bookshelf-footer") { footer() }
        }
        heldId?.let { id ->
            val value = byId[id]
            if (value != null) {
                val origin = pointer - grabOffset - viewport.topLeft
                Box(Modifier.offset { IntOffset(origin.x.roundToInt(), origin.y.roundToInt()) }
                    .width(with(density) { heldBounds.width.toDp() })
                    .height(with(density) { heldBounds.height.toDp() })
                    .graphicsLayer { if (horizontal) { translationY = -9.dp.toPx(); rotationZ = -2f } }
                    .alpha(.96f).background(ChaekSurface, RoundedCornerShape(if (horizontal) 0.dp else 12.dp))
                    .border(1.dp, ChaekAccent, RoundedCornerShape(if (horizontal) 0.dp else 12.dp))
                    .testTag("bookshelf-drag-ghost-$tagPrefix-$id")
                    .clearAndSetSemantics { stateDescription = "드래그 중" }) {
                    item(value, sourceIds.indexOf(id), Modifier)
                }
            }
        }
    }
}
