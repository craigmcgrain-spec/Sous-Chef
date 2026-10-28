package com.souschef.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souschef.app.data.RecipeStore
import com.souschef.app.data.ShoppingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(private val store: RecipeStore) : ViewModel() {

    val items: StateFlow<List<ShoppingItem>> = store.shopping
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun toggle(id: String) {
        viewModelScope.launch { store.toggleShoppingItem(id) }
    }

    fun clearChecked() {
        viewModelScope.launch { store.clearCheckedShopping() }
    }

    fun clearAll() {
        viewModelScope.launch { store.clearAllShopping() }
    }
}
