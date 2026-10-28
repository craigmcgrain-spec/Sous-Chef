package com.souschef.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(store: RecipeStore) : ViewModel() {

    val recipes: StateFlow<List<Recipe>> = store.recipes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val uncheckedShoppingCount: StateFlow<Int> = store.shopping
        .map { items -> items.count { !it.checked } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
