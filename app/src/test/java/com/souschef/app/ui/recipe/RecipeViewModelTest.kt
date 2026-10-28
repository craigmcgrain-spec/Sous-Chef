package com.souschef.app.ui.recipe

import com.souschef.app.MainDispatcherRule
import com.souschef.app.data.ExtractedRecipe
import com.souschef.app.data.Ingredient
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeExtractor
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.ShoppingItem
import com.souschef.app.data.Step
import com.souschef.app.data.Temperature
import com.souschef.app.sampleRecipe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Whole-list rewrites, like [com.souschef.app.data.DataStoreRecipeStore]. */
private class FakeStore(initial: Recipe) : RecipeStore {
    private val state = MutableStateFlow(listOf(initial))

    override val recipes: Flow<List<Recipe>> = state
    override val shopping: Flow<List<ShoppingItem>> = MutableStateFlow(emptyList())

    override suspend fun save(recipe: Recipe) {
        state.value = state.value.filterNot { it.id == recipe.id } + recipe
    }

    override suspend fun update(recipe: Recipe) {
        state.value = state.value.map { if (it.id == recipe.id) recipe else it }
    }

    override suspend fun update(id: String, transform: (Recipe) -> Recipe) {
        state.value = state.value.map { if (it.id == id) transform(it) else it }
    }

    override suspend fun delete(id: String) = error("not used")
    override suspend fun findByUrl(url: String): Recipe? = error("not used")
    override suspend fun addToShopping(recipe: Recipe, texts: List<String>) = error("not used")
    override suspend fun toggleShoppingItem(id: String) = error("not used")
    override suspend fun clearCheckedShopping() = error("not used")
    override suspend fun clearAllShopping() = error("not used")
}

private class FakeExtractor(
    private val result: List<Temperature> = emptyList(),
    private val fail: Boolean = false,
) : RecipeExtractor {
    val calls = mutableListOf<List<String>>()

    override suspend fun extract(html: String, url: String): ExtractedRecipe = error("not used")

    override suspend fun temperatures(texts: List<String>): List<Temperature> {
        calls += texts
        if (fail) throw IllegalStateException("extraction failed")
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `two rapid toggles both land`() = runTest {
        val store = FakeStore(sampleRecipe())
        val vm = RecipeViewModel("id-1", store, FakeExtractor())
        backgroundScope.launch { vm.recipe.collect {} }
        advanceUntilIdle()

        vm.toggleIngredient("i-1")
        vm.toggleIngredient("i-2")
        advanceUntilIdle()

        val saved = store.recipes.first().single()
        assertEquals(listOf(false, true), saved.ingredients.map { it.checked })
    }

    @Test
    fun `saveEdited recomputes temperatures for steps and ingredients`() = runTest {
        val store = FakeStore(sampleRecipe())
        val extractor = FakeExtractor(
            result = listOf(Temperature(200, 392, "200°C", "Heat the oven to 200°C.", 0)),
        )
        val vm = RecipeViewModel("id-1", store, extractor)
        val edited = sampleRecipe().copy(
            title = "Sunday Ragù, revised",
            steps = listOf(Step("s-1", "Heat the oven to 200°C.", checked = false)),
            ingredients = listOf(Ingredient("i-1", "2 tbsp olive oil", checked = true)),
        )
        var message: String? = "not called"

        vm.saveEdited(edited) { message = it }
        advanceUntilIdle()

        val saved = store.recipes.first().single()
        assertEquals("Sunday Ragù, revised", saved.title)
        assertEquals(
            listOf(
                Temperature(200, 392, "200°C", "Heat the oven to 200°C.", 0),
                Temperature(200, 392, "200°C", "Heat the oven to 200°C.", null),
            ),
            saved.temperatures,
        )
        assertEquals(
            listOf(listOf("Heat the oven to 200°C."), listOf("2 tbsp olive oil")),
            extractor.calls,
        )
        assertNull(message)
    }

    @Test
    fun `saveEdited keeps text and clears temperatures when extraction fails`() = runTest {
        val store = FakeStore(sampleRecipe())
        val vm = RecipeViewModel("id-1", store, FakeExtractor(fail = true))
        val edited = sampleRecipe().copy(
            steps = listOf(Step("s-1", "Sear the beef.", checked = true)),
        )
        var message: String? = null

        vm.saveEdited(edited) { message = it }
        advanceUntilIdle()

        val saved = store.recipes.first().single()
        assertEquals("Sear the beef.", saved.steps.single().text)
        assertTrue(saved.steps.single().checked)
        assertTrue(saved.temperatures.isEmpty())
        assertEquals("Saved, but temperatures couldn't be refreshed.", message)
    }
}
