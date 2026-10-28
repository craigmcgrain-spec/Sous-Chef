package com.souschef.app.ui

import com.souschef.app.MainDispatcherRule
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.ShoppingItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Mirrors the whole-list shopping rewrites of [com.souschef.app.data.DataStoreRecipeStore]. */
private class FakeShoppingStore(initial: List<ShoppingItem>) : RecipeStore {
    val shoppingState = MutableStateFlow(initial)
    var clearCheckedCalls = 0
    var clearAllCalls = 0

    override val recipes: Flow<List<Recipe>> = MutableStateFlow(emptyList())
    override val shopping: Flow<List<ShoppingItem>> = shoppingState

    override suspend fun save(recipe: Recipe) = error("not used")
    override suspend fun update(recipe: Recipe) = error("not used")
    override suspend fun update(id: String, transform: (Recipe) -> Recipe): Unit = error("not used")
    override suspend fun delete(id: String) = error("not used")
    override suspend fun findByUrl(url: String): Recipe? = error("not used")
    override suspend fun addToShopping(recipe: Recipe, texts: List<String>) = error("not used")

    override suspend fun toggleShoppingItem(id: String) {
        shoppingState.value = shoppingState.value.map {
            if (it.id == id) it.copy(checked = !it.checked) else it
        }
    }

    override suspend fun clearCheckedShopping() {
        clearCheckedCalls++
        shoppingState.value = shoppingState.value.filterNot { it.checked }
    }

    override suspend fun clearAllShopping() {
        clearAllCalls++
        shoppingState.value = emptyList()
    }
}

private fun item(id: String, text: String, checked: Boolean = false, recipeTitle: String = "Sunday Ragù") =
    ShoppingItem(
        id = id,
        text = text,
        checked = checked,
        recipeId = "recipe-$recipeTitle",
        recipeTitle = recipeTitle,
        addedAtEpochMs = 0L,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `items follow the store and toggle reports the id`() = runTest {
        val store = FakeShoppingStore(listOf(item("s-1", "2 tbsp olive oil")))
        val vm = ShoppingViewModel(store)
        backgroundScope.launch { vm.items.collect {} }
        advanceUntilIdle()

        assertEquals(listOf("s-1"), vm.items.value.map { it.id })

        vm.toggle("s-1")
        advanceUntilIdle()

        assertEquals(listOf(true), vm.items.value.map { it.checked })
    }

    @Test
    fun clear_checked_only_removes_checked() = runTest {
        val store = FakeShoppingStore(
            listOf(
                item("s-1", "2 tbsp olive oil", checked = true),
                item("s-2", "1kg beef shin"),
            ),
        )
        val vm = ShoppingViewModel(store)
        backgroundScope.launch { vm.items.collect {} }
        advanceUntilIdle()

        vm.clearChecked()
        advanceUntilIdle()

        assertEquals(1, store.clearCheckedCalls)
        assertEquals(listOf("s-2"), vm.items.value.map { it.id })
    }

    @Test
    fun clear_all_empties_the_list() = runTest {
        val store = FakeShoppingStore(
            listOf(item("s-1", "2 tbsp olive oil", checked = true), item("s-2", "1kg beef shin")),
        )
        val vm = ShoppingViewModel(store)
        backgroundScope.launch { vm.items.collect {} }
        advanceUntilIdle()

        vm.clearAll()
        advanceUntilIdle()

        assertEquals(1, store.clearAllCalls)
        assertEquals(emptyList<ShoppingItem>(), vm.items.value)
    }
}
