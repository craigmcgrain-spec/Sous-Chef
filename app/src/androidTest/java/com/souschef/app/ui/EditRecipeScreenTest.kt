package com.souschef.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.souschef.app.data.Recipe
import com.souschef.app.data.Step
import com.souschef.app.sampleRecipe
import com.souschef.app.ui.edit.EditRecipeScreen
import com.souschef.app.ui.theme.SousChefTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class EditRecipeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun blank_title_blocks_save() {
        var saved: Recipe? = null
        setEditScreen(sampleRecipe(), onSave = { saved = it })

        field(0).performTextClearance()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText("Give this recipe a title.").assertIsDisplayed()
        assertNull(saved)
    }

    @Test
    fun edits_title_and_steps() {
        var saved: Recipe? = null
        setEditScreen(sampleRecipe(), onSave = { saved = it })

        field(0).performTextClearance()
        field(0).performTextInput("Sunday Ragù, revised")
        // The fixture already has 195 minutes; clearing it must recompute from 15 + 180.
        field(4).performScrollTo().performTextClearance()
        composeRule.onNodeWithText("+ Add step").performScrollTo().performClick()
        field(9).performScrollTo().performTextInput("Add the wine")
        composeRule.onNodeWithText("Save").performClick()

        val draft = saved
        assertEquals("Sunday Ragù, revised", draft?.title)
        assertEquals(195, draft?.totalMinutes)
        assertEquals(3, draft?.steps?.size)
        assertEquals(
            listOf("Heat the oven to 160°C.", "Brown the beef.", "Add the wine"),
            draft?.steps?.map { it.text },
        )
        assertEquals(listOf("s-1", "s-2"), draft?.steps?.take(2)?.map { it.id })
        UUID.fromString(draft?.steps?.get(2)?.id)
    }

    @Test
    fun reorder_keeps_checked_state() {
        val recipe = sampleRecipe().copy(
            steps = listOf(
                Step("s-a", "A", checked = true),
                Step("s-b", "B", checked = false),
            ),
        )
        var saved: Recipe? = null
        setEditScreen(recipe, onSave = { saved = it })

        composeRule.onNodeWithContentDescription("Reorder step A")
            .performScrollTo()
            .performCustomAccessibilityActionWithLabel("Move down")
        composeRule.onNodeWithText("Save").performClick()

        val draft = saved
        assertEquals(listOf("B", "A"), draft?.steps?.map { it.text })
        assertEquals(listOf("s-b", "s-a"), draft?.steps?.map { it.id })
        assertTrue(draft?.steps?.get(1)?.checked == true)
    }

    @Test
    fun drag_handle_reorders_rows() {
        var saved: Recipe? = null
        setEditScreen(sampleRecipe(), onSave = { saved = it })

        composeRule.onNodeWithContentDescription("Reorder step Heat the oven to 160°C.")
            .performScrollTo()
            .performTouchInput {
                down(center)
                // The long-press timeout is judged on event time, so this move
                // is what turns the press into a drag.
                advanceEventTime(700)
                moveBy(Offset(0f, 1f))
                advanceEventTime(50)
                moveBy(Offset(0f, 120f))
                up()
            }
        composeRule.onNodeWithText("Save").performClick()

        assertEquals(
            listOf("Brown the beef.", "Heat the oven to 160°C."),
            saved?.steps?.map { it.text },
        )
    }

    @Test
    fun drag_reorders_the_first_row_two_slots() {
        val recipe = sampleRecipe().copy(
            steps = listOf(
                Step("s-1", "A", checked = false),
                Step("s-2", "B", checked = false),
                Step("s-3", "C", checked = false),
            ),
        )
        var saved: Recipe? = null
        setEditScreen(recipe, onSave = { saved = it })

        val handle = composeRule.onNodeWithContentDescription("Reorder step A").performScrollTo()
        handle.performTouchInput { down(center) }
        handle.performTouchInput {
            advanceEventTime(700)
            moveBy(Offset(0f, 1f))
        }
        composeRule.waitForIdle()
        handle.performTouchInput { moveBy(Offset(0f, 90f)) }
        composeRule.waitForIdle()
        handle.performTouchInput { moveBy(Offset(0f, 90f)) }
        composeRule.waitForIdle()
        handle.performTouchInput { up() }
        composeRule.onNodeWithText("Save").performClick()

        assertEquals(listOf("B", "C", "A"), saved?.steps?.map { it.text })
    }

    @Test
    fun back_cannot_discard_while_saving() {
        var cancelled = false
        setEditScreen(sampleRecipe(), saving = true, onCancel = { cancelled = true })

        field(0).performTextInput("!")
        composeRule.onNodeWithContentDescription("Back").assertIsNotEnabled()

        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertFalse(cancelled)
    }

    @Test
    fun system_back_cannot_discard_while_saving() {
        var cancelled = false
        var backReached = false
        setEditScreen(
            sampleRecipe(),
            saving = true,
            onCancel = { cancelled = true },
            onBackReached = { backReached = true },
        )

        field(0).performTextInput("!")
        pressBack()

        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertFalse(cancelled)
        // The screen, not the NavHost/activity, must swallow the press.
        assertFalse(backReached)
    }

    @Test
    fun system_back_asks_to_discard_when_not_saving() {
        setEditScreen(sampleRecipe())

        field(0).performTextInput("!")
        pressBack()

        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
    }

    @Test
    fun back_with_changes_asks_to_discard() {
        var cancelled = false
        setEditScreen(sampleRecipe(), onCancel = { cancelled = true })

        field(0).performTextInput("!")
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeRule.onNodeWithText("Discard").performClick()
        assertTrue(cancelled)
    }

    @Test
    fun back_without_changes_cancels_without_asking() {
        var cancelled = false
        setEditScreen(sampleRecipe(), onCancel = { cancelled = true })

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Discard changes?").assertDoesNotExist()
        assertTrue(cancelled)
    }

    private fun field(index: Int) = composeRule.onAllNodes(hasSetTextAction())[index]

    /** Drives the activity-level back path that navigation-compose also consumes. */
    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun setEditScreen(
        recipe: Recipe,
        saving: Boolean = false,
        onCancel: () -> Unit = {},
        onSave: (Recipe) -> Unit = {},
        onBackReached: (() -> Unit)? = null,
    ) {
        composeRule.setContent {
            SousChefTheme {
                // Registered before the screen, so it only runs when the screen's
                // own BackHandler is disabled and lets the press fall through.
                if (onBackReached != null) BackHandler { onBackReached() }
                EditRecipeScreen(
                    recipe = recipe,
                    saving = saving,
                    onCancel = onCancel,
                    onSave = onSave,
                )
            }
        }
    }
}
