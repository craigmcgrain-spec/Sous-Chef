package com.souschef.app.ui.recipe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.souschef.app.R
import com.souschef.app.data.Recipe
import com.souschef.app.data.formatServings
import com.souschef.app.ui.components.CheckRow
import com.souschef.app.ui.components.SectionLabel

@Composable
fun IngredientsTab(recipe: Recipe, onToggle: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        formatServings(recipe.servings)?.let { servings ->
            SectionLabel(servings)
            Spacer(Modifier.height(12.dp))
        }
        if (recipe.ingredients.isEmpty()) {
            QuietNote(stringResource(R.string.ingredients_empty))
        } else {
            recipe.ingredients.forEach { ingredient ->
                CheckRow(
                    text = ingredient.text,
                    checked = ingredient.checked,
                    onCheckedChange = { onToggle(ingredient.id) },
                )
            }
        }
    }
}
