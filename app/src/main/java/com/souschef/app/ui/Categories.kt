package com.souschef.app.ui

import androidx.annotation.StringRes
import com.souschef.app.R
import com.souschef.app.data.RecipeCategory

val RecipeCategory.label: Int
    @StringRes get() = when (this) {
        RecipeCategory.Appetizers -> R.string.category_appetizers
        RecipeCategory.Sides -> R.string.category_sides
        RecipeCategory.Mains -> R.string.category_mains
        RecipeCategory.Dessert -> R.string.category_dessert
        RecipeCategory.Snacks -> R.string.category_snacks
    }

fun categoryName(name: String?): RecipeCategory? =
    RecipeCategory.entries.firstOrNull { it.name == name }
