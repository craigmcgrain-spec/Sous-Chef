package com.souschef.app.ui.edit

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {

    @Test
    fun moveItem_moves_down_and_up() {
        assertEquals(listOf("b", "a", "c"), moveItem(listOf("a", "b", "c"), 0, 1))
        assertEquals(listOf("b", "a", "c"), moveItem(listOf("a", "b", "c"), 1, 0))
    }

    @Test
    fun moveItem_handles_same_index_and_longer_moves() {
        assertEquals(listOf("a", "b", "c"), moveItem(listOf("a", "b", "c"), 1, 1))
        assertEquals(listOf("b", "c", "a"), moveItem(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("c", "a", "b"), moveItem(listOf("a", "b", "c"), 2, 0))
        assertEquals(listOf("only"), moveItem(listOf("only"), 0, 0))
    }

    @Test
    fun drag_moves_a_row_once_its_centre_crosses_the_next() {
        val moves = mutableListOf<Pair<Int, Int>>()
        val state = ReorderState { from, to -> moves += from to to }
        val keys = listOf("a", "b", "c")
        state.record("a", Rect(0f, 0f, 100f, 60f))
        state.record("b", Rect(0f, 60f, 100f, 120f))
        state.record("c", Rect(0f, 120f, 100f, 180f))
        state.start("a")

        state.drag("a", keys, 30f)
        assertEquals(emptyList<Pair<Int, Int>>(), moves)
        assertEquals(30f, state.offsetFor("a"))

        state.drag("a", keys, 30f)
        assertEquals(listOf(0 to 1), moves)
        // The row keeps only the distance past its new slot's centre.
        assertEquals(0f, state.offsetFor("a"))
    }
}
