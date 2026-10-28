package com.souschef.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.souschef.app.data.ShoppingItem
import com.souschef.app.ui.theme.SousChefTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun shoppingItem(
    id: String,
    text: String,
    checked: Boolean = false,
    recipeId: String = "r-1",
    recipeTitle: String = "Sunday Ragù",
) = ShoppingItem(
    id = id,
    text = text,
    checked = checked,
    recipeId = recipeId,
    recipeTitle = recipeTitle,
    addedAtEpochMs = 0L,
)

@RunWith(AndroidJUnit4::class)
class ShoppingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        items: List<ShoppingItem>,
        onBack: () -> Unit = {},
        onToggle: (String) -> Unit = {},
        onClearChecked: () -> Unit = {},
        onClearAll: () -> Unit = {},
    ) {
        composeRule.setContent {
            SousChefTheme {
                ShoppingScreen(
                    items = items,
                    onBack = onBack,
                    onToggle = onToggle,
                    onClearChecked = onClearChecked,
                    onClearAll = onClearAll,
                )
            }
        }
    }

    @Test
    fun groups_by_recipe_title() {
        setScreen(
            listOf(
                shoppingItem("a-1", "2 tbsp olive oil"),
                shoppingItem(
                    "b-1",
                    "1 onion",
                    recipeId = "r-2",
                    recipeTitle = "Roast Tomato Soup",
                ),
                shoppingItem("a-2", "1kg beef shin"),
            ),
        )
        composeRule.onNodeWithText("SUNDAY RAGÙ").assertIsDisplayed()
        composeRule.onNodeWithText("ROAST TOMATO SOUP").assertIsDisplayed()
        composeRule.onNodeWithText("2 tbsp olive oil").assertIsDisplayed()
        composeRule.onNodeWithText("1 onion").assertIsDisplayed()
        composeRule.onNodeWithText("1kg beef shin").assertIsDisplayed()

        val firstHeading = composeRule.onNodeWithText("SUNDAY RAGÙ").fetchSemanticsNode().boundsInRoot
        val secondHeading = composeRule.onNodeWithText("ROAST TOMATO SOUP").fetchSemanticsNode().boundsInRoot
        assertTrue(firstHeading.top < secondHeading.top)
    }

    @Test
    fun checked_item_shows_done_and_toggle_reports_id() {
        var toggled: String? = null
        setScreen(
            listOf(shoppingItem("a-1", "2 tbsp olive oil", checked = true)),
            onToggle = { toggled = it },
        )

        composeRule.onNodeWithText("2 tbsp olive oil").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Done"),
        )
        composeRule.onNodeWithText("2 tbsp olive oil").performClick()
        assertEquals("a-1", toggled)
    }

    @Test
    fun empty_state_copy() {
        setScreen(emptyList())
        composeRule.onNodeWithText("Nothing to buy").assertIsDisplayed()
        composeRule.onNodeWithText("Add ingredients from a recipe's Shopping tab.").assertIsDisplayed()
    }

    @Test
    fun clear_checked_is_immediate_and_clear_all_confirms() {
        var clearChecked = 0
        var clearAll = 0
        setScreen(
            listOf(shoppingItem("a-1", "2 tbsp olive oil", checked = true)),
            onClearChecked = { clearChecked++ },
            onClearAll = { clearAll++ },
        )

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Clear checked").performClick()
        assertEquals(1, clearChecked)

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Clear all").performClick()
        composeRule.onNodeWithText("Clear the shopping list?").assertIsDisplayed()
        assertEquals(0, clearAll)

        composeRule.onNodeWithText("Clear all").performClick()
        assertEquals(1, clearAll)
    }
}
