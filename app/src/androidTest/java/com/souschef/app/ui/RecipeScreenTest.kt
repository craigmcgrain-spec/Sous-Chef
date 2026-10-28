package com.souschef.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.souschef.app.data.Recipe
import com.souschef.app.sampleRecipe
import com.souschef.app.ui.recipe.RecipeScreen
import com.souschef.app.ui.recipe.RecipeTab
import com.souschef.app.ui.theme.SousChefTheme
import org.junit.Assert.assertEquals
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

    private fun setRecipeScreen(
        recipe: Recipe,
        onToggleIngredient: (String) -> Unit = {},
        onToggleStep: (String) -> Unit = {},
        onDelete: () -> Unit = {},
        initialTab: RecipeTab = RecipeTab.Ingredients,
    ) {
        composeRule.setContent {
            SousChefTheme {
                var current by remember { mutableStateOf(recipe) }
                var tab by rememberSaveable { mutableStateOf(initialTab.name) }
                RecipeScreen(
                    recipe = current,
                    activeTab = RecipeTab.valueOf(tab),
                    onTabSelected = { tab = it.name },
                    onBack = {},
                    onEdit = {},
                    onDelete = onDelete,
                    onToggleIngredient = { id ->
                        current = current.copy(
                            ingredients = current.ingredients.map {
                                if (it.id == id) it.copy(checked = !it.checked) else it
                            },
                        )
                        onToggleIngredient(id)
                    },
                    onToggleStep = { id ->
                        current = current.copy(
                            steps = current.steps.map {
                                if (it.id == id) it.copy(checked = !it.checked) else it
                            },
                        )
                        onToggleStep(id)
                    },
                    onJumpToStep = {},
                    addedShoppingTexts = emptySet<String>(),
                    onAddToShopping = {},
                    onOpenShopping = {},
                )
            }
        }
    }
}
