package com.souschef.app.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

inline fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> =
    list.toMutableList().apply { add(to, removeAt(from)) }

/**
 * Tracks each row's bounds in the root and the accumulated drag, then moves the
 * dragged row once its centre crosses a neighbour.
 *
 * shortcut: dragging to the edge does not scroll the form; add edge auto-scroll
 * for recipes with more lines than fit on screen.
 */
class ReorderState(private val onMove: (Int, Int) -> Unit) {
    private val bounds = mutableStateMapOf<String, Rect>()
    private val offsets = mutableStateMapOf<String, Float>()
    var dragging by mutableStateOf<String?>(null)
        private set

    fun record(key: String, rect: Rect) {
        bounds[key] = rect
    }

    fun offsetFor(key: String): Float = offsets[key] ?: 0f

    fun start(key: String) {
        dragging = key
        offsets[key] = 0f
    }

    fun end(key: String) {
        dragging = null
        offsets.remove(key)
    }

    fun move(from: Int, to: Int) {
        onMove(from, to)
    }

    fun drag(key: String, keys: List<String>, dy: Float) {
        val offset = offsetFor(key) + dy
        offsets[key] = offset
        val from = keys.indexOf(key)
        val rect = bounds[key] ?: return
        if (from < 0 || offset == 0f) return
        val step = if (offset > 0) 1 else -1
        val to = from + step
        val neighbour = bounds[keys.getOrNull(to) ?: return] ?: return
        if (neighbour.height <= 0f) return
        val centre = rect.center.y + offset
        val crossed = if (step > 0) centre >= neighbour.center.y else centre <= neighbour.center.y
        if (!crossed) return
        // Keep the finger offset relative to the row's new slot.
        offsets[key] = offset - (neighbour.center.y - rect.center.y)
        onMove(from, to)
    }
}

@Composable
fun rememberReorderState(onMove: (Int, Int) -> Unit): ReorderState =
    remember { ReorderState(onMove) }
