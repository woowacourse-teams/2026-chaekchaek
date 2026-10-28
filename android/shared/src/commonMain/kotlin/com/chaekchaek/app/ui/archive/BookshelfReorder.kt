package com.chaekchaek.app.ui.archive

/** A drag edits a candidate order. Only a completed, changed drag produces a save. */
internal class BookshelfReorder {
    var heldId: Long? = null
        private set
    var original: List<Long> = emptyList()
        private set
    var candidate: List<Long> = emptyList()
        private set

    fun begin(ids: List<Long>, id: Long): Boolean {
        if (heldId != null || id !in ids || ids.distinct().size != ids.size) return false
        original = ids.toList()
        candidate = original
        heldId = id
        return true
    }

    fun preview(targetId: Long): Boolean {
        val held = heldId ?: return false
        val from = candidate.indexOf(held)
        val to = candidate.indexOf(targetId)
        if (from < 0 || to < 0 || from == to) return false
        candidate = candidate.toMutableList().apply { add(to, removeAt(from)) }
        return true
    }

    fun finish(cancelled: Boolean = false): List<Long>? {
        val result = candidate.takeIf { heldId != null && !cancelled && it != original }
        heldId = null
        original = emptyList()
        candidate = emptyList()
        return result
    }
}

/** Uses measured item bounds, so variable widths and heights do not change the drop semantics. */
internal fun reorderTarget(position: Float, bounds: List<ReorderItemBounds>): Long? =
    bounds.firstOrNull { position >= it.start && position <= it.end }?.id
        ?: bounds.minByOrNull { kotlin.math.abs(position - (it.start + it.end) / 2f) }?.id

internal data class ReorderItemBounds(val id: Long, val start: Float, val end: Float)

internal fun reorderEdgeScroll(position: Float, start: Float, end: Float, edge: Float, maximum: Float): Float = when {
    position < start + edge -> -((start + edge - position) / edge).coerceIn(0f, 1f) * maximum
    position > end - edge -> ((position - end + edge) / edge).coerceIn(0f, 1f) * maximum
    else -> 0f
}
