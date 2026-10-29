package com.souschef.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.souschef.app.R
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeCategory
import com.souschef.app.data.formatLibraryMeta
import com.souschef.app.ui.components.EmptyState
import com.souschef.app.ui.components.HairlineRule
import com.souschef.app.ui.components.RecipeThumb

@Composable
fun LibraryScreen(
    recipes: List<Recipe>,
    uncheckedShoppingCount: Int,
    onOpenRecipe: (String) -> Unit,
    onOpenShopping: () -> Unit,
    onPasteLink: () -> Unit,
    customCategories: List<String> = emptyList(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
        LibraryHeader(recipes.size, uncheckedShoppingCount, onOpenShopping, onPasteLink)
        if (recipes.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyState(
                    title = stringResource(R.string.library_empty_title),
                    body = stringResource(R.string.library_empty_body),
                    actionLabel = stringResource(R.string.paste_link),
                    onAction = onPasteLink,
                )
            }
        } else {
            CategoryFilters(categoryFilter, customCategories) { categoryFilter = it }
            val visible = recipes.filter { categoryFilter == null || it.category == categoryFilter }
            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.category_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(260.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(visible, key = { it.id }) { recipe ->
                        Column {
                            RecipeRow(recipe, onOpenRecipe)
                            HairlineRule()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilters(
    selected: String?,
    customCategories: List<String>,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.category_all)) },
        )
        RecipeCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category.name,
                onClick = { onSelect(category.name) },
                label = { Text(stringResource(category.label)) },
            )
        }
        customCategories
            .filterNot { custom -> RecipeCategory.entries.any { it.name.equals(custom, ignoreCase = true) } }
            .forEach { name ->
                FilterChip(
                    selected = selected == name,
                    onClick = { onSelect(name) },
                    label = { Text(name) },
                )
            }
    }
}

@Composable
private fun LibraryHeader(
    recipeCount: Int,
    uncheckedShoppingCount: Int,
    onOpenShopping: () -> Unit,
    onPasteLink: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val badgeDescription = pluralStringResource(
        R.plurals.shopping_badge,
        uncheckedShoppingCount,
        uncheckedShoppingCount,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = pluralStringResource(R.plurals.recipe_count, recipeCount, recipeCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BadgedBox(
            badge = {
                if (uncheckedShoppingCount > 0) {
                    Badge(modifier = Modifier.semantics { contentDescription = badgeDescription }) {
                        Text(uncheckedShoppingCount.toString())
                    }
                }
            },
        ) {
            IconButton(onClick = onOpenShopping) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = stringResource(R.string.shopping_list),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.paste_link)) },
                    onClick = {
                        menuOpen = false
                        onPasteLink()
                    },
                )
            }
        }
    }
}

@Composable
private fun RecipeRow(recipe: Recipe, onOpenRecipe: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clickable { onOpenRecipe(recipe.id) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeThumb(recipe)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = formatLibraryMeta(recipe)
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
