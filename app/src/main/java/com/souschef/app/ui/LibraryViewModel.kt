package com.souschef.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(store: RecipeStore) : ViewModel() {

    val recipes: StateFlow<List<Recipe>> = store.recipes
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uncheckedShoppingCount: StateFlow<Int> = store.shopping
        .catch { emit(emptyList()) }
        .map { items -> items.count { !it.checked } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
