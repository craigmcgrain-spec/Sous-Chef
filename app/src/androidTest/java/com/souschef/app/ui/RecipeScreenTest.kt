package com.souschef.app.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.souschef.app.data.Recipe
import com.souschef.app.data.Step
import com.souschef.app.data.Temperature
import com.souschef.app.sampleRecipe
import com.souschef.app.ui.recipe.RecipeScreen
import com.souschef.app.ui.recipe.RecipeTab
import com.souschef.app.ui.theme.SousChefTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shows_title_meta_and_four_tabs() {
        setRecipeScreen(sampleRecipe())
        composeRule.onNodeWithText("Sunday Ragù").assertIsDisplayed()
        composeRule.onNodeWithText("BBC Good Food · 3h 15m · Serves 4").assertIsDisplayed()
        listOf("INGREDIENTS", "STEPS", "TIMES", "SHOPPING").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun tapping_tab_switches_content() {
        setRecipeScreen(sampleRecipe())
        composeRule.onNodeWithText("STEPS").performClick()
        composeRule.onNodeWithText("Heat the oven to 160°C.").assertIsDisplayed()
    }

    @Test
    fun ingredient_click_reports_id() {
        var toggled: String? = null
        setRecipeScreen(sampleRecipe(), onToggleIngredient = { toggled = it })
        composeRule.onNodeWithText("2 tbsp olive oil").performClick()
        assertEquals("i-1", toggled)
    }

    @Test
    fun step_click_reports_id_and_progress_updates() {
        var toggled: String? = null
        setRecipeScreen(sampleRecipe(), onToggleStep = { toggled = it })
        composeRule.onNodeWithText("STEPS").performClick()
        composeRule.onNodeWithText("1 of 2 done").assertIsDisplayed()
        composeRule.onNodeWithText("Heat the oven to 160°C.").performClick()
        assertEquals("s-1", toggled)
        composeRule.onNodeWithText("2 of 2 done").assertIsDisplayed()
    }

    @Test
    fun empty_ingredients_and_steps_show_a_note() {
        setRecipeScreen(sampleRecipe().copy(ingredients = emptyList(), steps = emptyList()))
        composeRule.onNodeWithText("No ingredients saved for this recipe.").assertIsDisplayed()
        composeRule.onNodeWithText("STEPS").performClick()
        composeRule.onNodeWithText("No steps saved for this recipe.").assertIsDisplayed()
    }

    @Test
    fun overflow_menu_uses_the_palette_surface() {
        setRecipeScreen(sampleRecipe())
        composeRule.onNodeWithContentDescription("More options").performClick()
        val menu = composeRule.onNode(isPopup()).captureToImage().toPixelMap()
        val rgb = menu[menu.width - 8, menu.height / 2].toArgb() and 0xFFFFFF
        assertEquals(0xFFFDF8, rgb)
    }

    @Test
    fun delete_asks_for_confirmation() {
        var deleted = false
        setRecipeScreen(sampleRecipe(), onDelete = { deleted = true })
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Delete recipe").performClick()
        composeRule.onNodeWithText("Delete this recipe?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        assertTrue(deleted)
    }

    @Test
    fun checked_step_is_displayed_as_done() {
        setRecipeScreen(sampleRecipe())
        composeRule.onNodeWithText("STEPS").performClick()
        composeRule.onNodeWithText("Brown the beef.").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Done"),
        )
    }

    @Test
    fun times_show_prep_cook_total_and_dashes() {
        val current = setRecipeScreen(sampleRecipe(), initialTab = RecipeTab.Times)
        composeRule.onNodeWithText("PREP").assertIsDisplayed()
        composeRule.onNodeWithText("15m").assertIsDisplayed()
        composeRule.onNodeWithText("COOK").assertIsDisplayed()
        composeRule.onNodeWithText("3h").assertIsDisplayed()
        composeRule.onNodeWithText("TOTAL").assertIsDisplayed()
        composeRule.onNodeWithText("3h 15m").assertIsDisplayed()

        composeRule.runOnUiThread { current.value = sampleRecipe().copy(cookMinutes = null) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("—").assertIsDisplayed()

        composeRule.runOnUiThread {
            current.value = sampleRecipe().copy(prepMinutes = null, cookMinutes = null, totalMinutes = null)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("PREP").assertDoesNotExist()
    }

    @Test
    fun temperature_card_jumps_to_step() {
        var jumped: Int? = null
        setRecipeScreen(
            sampleRecipe(),
            initialTab = RecipeTab.Times,
            onJumpToStep = { jumped = it },
        )
        composeRule.onNodeWithText("Step 1").performClick()
        assertEquals(0, jumped)
        composeRule.onNodeWithText("STEPS").assertIsSelected()
        composeRule.onNodeWithText("Heat the oven to 160°C.").assertIsDisplayed()
        composeRule.onNodeWithText("01").assertIsDisplayed()
    }

    @Test
    fun temperature_jump_scrolls_to_a_later_step() {
        val steps = (1..12).map { Step("s-$it", "Step number $it does something.", checked = false) }
        setRecipeScreen(
            sampleRecipe().copy(
                steps = steps,
                temperatures = listOf(Temperature(150, 300, "150°C", "Set the oven to 150°C.", 9)),
            ),
            initialTab = RecipeTab.Times,
        )
        composeRule.onNodeWithText("Step 10").performClick()
        composeRule.onNodeWithText("Step number 10 does something.").assertIsDisplayed()
    }

    @Test
    fun no_temperatures_shows_empty_copy() {
        setRecipeScreen(sampleRecipe().copy(temperatures = emptyList()), initialTab = RecipeTab.Times)
        composeRule.onNodeWithText("No oven temperatures found in this recipe.").assertIsDisplayed()
    }

    @Test
    fun shopping_tab_adds_all() {
        var added: List<String>? = null
        var opened = false
        setRecipeScreen(
            sampleRecipe(),
            initialTab = RecipeTab.Shopping,
            onAddToShopping = { added = it },
            onOpenShopping = { opened = true },
        )
        composeRule.onNodeWithText("Add all ingredients").performClick()
        assertEquals(listOf("2 tbsp olive oil", "1kg beef shin"), added)
        composeRule.onNodeWithText("View list").performClick()
        assertTrue(opened)
    }

    @Test
    fun added_rows_are_disabled() {
        var added: List<String>? = null
        setRecipeScreen(
            sampleRecipe(),
            initialTab = RecipeTab.Shopping,
            addedShoppingTexts = setOf("2 tbsp olive oil"),
            onAddToShopping = { added = it },
        )
        composeRule.onNodeWithText("1kg beef shin").assertIsDisplayed()
        composeRule.onNodeWithText("2 tbsp olive oil").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("On the shopping list").assertIsDisplayed()
        composeRule.onNodeWithText("2 tbsp olive oil").performClick()
        assertNull(added)
    }

    private fun setRecipeScreen(
        recipe: Recipe,
        onToggleIngredient: (String) -> Unit = {},
        onToggleStep: (String) -> Unit = {},
        onDelete: () -> Unit = {},
        initialTab: RecipeTab = RecipeTab.Ingredients,
        onJumpToStep: (Int) -> Unit = {},
        addedShoppingTexts: Set<String> = emptySet(),
        onAddToShopping: (List<String>) -> Unit = {},
        onOpenShopping: () -> Unit = {},
    ): MutableState<Recipe> {
        val current = mutableStateOf(recipe)
        composeRule.setContent {
            SousChefTheme {
                var tab by rememberSaveable { mutableStateOf(initialTab.name) }
                RecipeScreen(
                    recipe = current.value,
                    activeTab = RecipeTab.valueOf(tab),
                    onTabSelected = { tab = it.name },
                    onBack = {},
                    onEdit = {},
                    onDelete = onDelete,
                    onToggleIngredient = { id ->
                        current.value = current.value.copy(
                            ingredients = current.value.ingredients.map {
                                if (it.id == id) it.copy(checked = !it.checked) else it
                            },
                        )
                        onToggleIngredient(id)
                    },
                    onToggleStep = { id ->
                        current.value = current.value.copy(
                            steps = current.value.steps.map {
                                if (it.id == id) it.copy(checked = !it.checked) else it
                            },
                        )
                        onToggleStep(id)
                    },
                    onJumpToStep = { index ->
                        tab = RecipeTab.Steps.name
                        onJumpToStep(index)
                    },
                    addedShoppingTexts = addedShoppingTexts,
                    onAddToShopping = onAddToShopping,
                    onOpenShopping = onOpenShopping,
                )
            }
        }
        return current
    }
}
