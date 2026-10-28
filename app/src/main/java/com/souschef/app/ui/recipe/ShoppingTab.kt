package com.souschef.app.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.souschef.app.R
import com.souschef.app.data.Recipe
import com.souschef.app.ui.components.HairlineRule

@Composable
fun ShoppingTab(
    recipe: Recipe,
    addedTexts: Set<String>,
    onAdd: (List<String>) -> Unit,
    onOpenShopping: () -> Unit,
    shoppingCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        val pending = recipe.ingredients
            .map { it.text.trim() }
            .filter { it.isNotEmpty() && it !in addedTexts }
            .distinct()
        TextButton(
            onClick = { onAdd(pending) },
            enabled = pending.isNotEmpty(),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            Text(stringResource(R.string.shopping_add_all))
        }
        if (recipe.ingredients.isEmpty()) {
            QuietNote(stringResource(R.string.ingredients_empty))
        } else {
            recipe.ingredients.forEach { ingredient ->
                val text = ingredient.text.trim()
                ShoppingRow(
                    text = ingredient.text,
                    added = text in addedTexts,
                    onAdd = { onAdd(listOf(text)) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HairlineRule()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.shopping_list_count,
                    shoppingCount,
                    shoppingCount,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenShopping) {
                Text(stringResource(R.string.shopping_view_list))
            }
        }
    }
}

@Composable
private fun ShoppingRow(text: String, added: Boolean, onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = !added, role = Role.Button, onClick = onAdd)
            .semantics { if (added) disabled() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (added) colors.onSurfaceVariant else colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (added) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.shopping_added),
                tint = colors.secondary,
            )
        }
    }
}
