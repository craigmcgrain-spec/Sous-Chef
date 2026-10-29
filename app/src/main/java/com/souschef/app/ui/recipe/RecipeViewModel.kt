package com.souschef.app.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeExtractor
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.ShoppingItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val recipeId: String,
    private val store: RecipeStore,
    private val extractor: RecipeExtractor,
) : ViewModel() {

    val recipe: StateFlow<Recipe?> = store.recipes
        .catch { emit(emptyList()) }
        .map { recipes -> recipes.firstOrNull { it.id == recipeId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val shopping: StateFlow<List<ShoppingItem>> = store.shopping
        .catch { emit(emptyList()) }
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

    fun setCategory(category: String?) {
        viewModelScope.launch {
            store.update(recipeId) { it.copy(category = category) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            store.delete(recipeId)
            onDeleted()
        }
    }

    /**
     * Refreshes temperatures from the edited text, then saves. [onResult] gets
     * null on success or the message to show when only the text could be saved.
     * When the editor changed temperatures by hand, pass false to keep them.
     */
    fun saveEdited(
        edited: Recipe,
        refreshTemperatures: Boolean = true,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            val temperatures = if (refreshTemperatures) {
                try {
                    extractor.temperatures(edited.steps.map { it.text }) +
                        extractor.temperatures(edited.ingredients.map { it.text }).map { it.copy(stepIndex = null) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            } else {
                edited.temperatures
            }
            try {
                store.update(edited.copy(temperatures = temperatures.orEmpty()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Let the editor re-enable Save instead of trapping the screen on a failed write.
                onResult(SAVE_FAILED)
                return@launch
            }
            onResult(if (refreshTemperatures && temperatures == null) TEMPERATURES_STALE else null)
        }
    }

    companion object {
        // Copy also lives in strings.xml; the view model has no Context for it.
        const val TEMPERATURES_STALE = "Saved, but temperatures couldn't be refreshed."
        const val SAVE_FAILED = "Couldn't save your changes. Try again."
    }

    private fun toggle(transform: (Recipe) -> Recipe) {
        viewModelScope.launch { store.update(recipeId, transform) }
    }
}
