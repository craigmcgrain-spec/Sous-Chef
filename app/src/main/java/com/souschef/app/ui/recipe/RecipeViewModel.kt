package com.souschef.app.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeExtractor
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.ShoppingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val recipeId: String,
    private val store: RecipeStore,
    // Task 11 recomputes temperatures through this on save.
    @Suppress("unused") private val extractor: RecipeExtractor,
) : ViewModel() {

    val recipe: StateFlow<Recipe?> = store.recipes
        .map { recipes -> recipes.firstOrNull { it.id == recipeId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val shopping: StateFlow<List<ShoppingItem>> = store.shopping
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun toggleIngredient(id: String) = toggle { recipe ->
        recipe.copy(
            ingredients = recipe.ingredients.map {
                if (it.id == id) it.copy(checked = !it.checked) else it
            },
        )
    }

    fun toggleStep(id: String) = toggle { recipe ->
        recipe.copy(
            steps = recipe.steps.map {
                if (it.id == id) it.copy(checked = !it.checked) else it
            },
        )
    }

    fun addToShopping(texts: List<String>) {
        val current = recipe.value ?: return
        viewModelScope.launch { store.addToShopping(current, texts) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            store.delete(recipeId)
            onDeleted()
        }
    }

    private fun toggle(transform: (Recipe) -> Recipe) {
        viewModelScope.launch { store.update(recipeId, transform) }
    }
}
