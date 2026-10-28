package com.souschef.app.ui.recipe

import com.souschef.app.MainDispatcherRule
import com.souschef.app.data.ExtractedRecipe
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeExtractor
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.ShoppingItem
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

private object FakeExtractor : RecipeExtractor {
    override suspend fun extract(html: String, url: String): ExtractedRecipe = error("not used")
    override suspend fun temperatures(texts: List<String>): List<Temperature> = error("not used")
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `two rapid toggles both land`() = runTest {
        val store = FakeStore(sampleRecipe())
        val vm = RecipeViewModel("id-1", store, FakeExtractor)
        backgroundScope.launch { vm.recipe.collect {} }
        advanceUntilIdle()

        vm.toggleIngredient("i-1")
        vm.toggleIngredient("i-2")
        advanceUntilIdle()

        val saved = store.recipes.first().single()
        assertEquals(listOf(false, true), saved.ingredients.map { it.checked })
    }
}
