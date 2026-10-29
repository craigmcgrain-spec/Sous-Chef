@file:OptIn(ExperimentalFoundationApi::class)

package com.souschef.app.ui.recipe

import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.souschef.app.R
import com.souschef.app.data.Recipe
import com.souschef.app.data.RecipeCategory
import com.souschef.app.data.formatRecipeMeta
import com.souschef.app.ui.categoryName
import com.souschef.app.ui.components.HairlineRule
import com.souschef.app.ui.label

enum class RecipeTab { Ingredients, Steps, Times, Shopping }

@Composable
fun RecipeScreen(
    recipe: Recipe,
    activeTab: RecipeTab,
    onTabSelected: (RecipeTab) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleIngredient: (String) -> Unit,
    onToggleStep: (String) -> Unit,
    onJumpToStep: (Int) -> Unit,
    addedShoppingTexts: Set<String>,
    onAddToShopping: (List<String>) -> Unit,
    onOpenShopping: () -> Unit,
    shoppingCount: Int,
    keepScreenAwake: Boolean = true,
    onKeepScreenAwakeChange: (Boolean) -> Unit = {},
    customCategories: List<String> = emptyList(),
    onCategoryChange: (String?) -> Unit = {},
    onAddCategory: (String) -> Unit = {},
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var highlightStep by rememberSaveable { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    var listTop by remember { mutableFloatStateOf(0f) }
    var stickyHeight by remember { mutableFloatStateOf(0f) }
    val animations = rememberAnimationsEnabled()
    val slide = with(LocalDensity.current) { 8.dp.roundToPx() }
    val view = LocalView.current
    DisposableEffect(view, keepScreenAwake) {
        view.keepScreenOn = keepScreenAwake
        onDispose { view.keepScreenOn = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
        ) {
            RecipeActions(
                onBack = onBack,
                onEdit = onEdit,
                onDelete = { confirmDelete = true },
                keepScreenAwake = keepScreenAwake,
                onKeepScreenAwakeChange = onKeepScreenAwakeChange,
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { listTop = it.boundsInRoot().top },
            ) {
                item(key = "header") {
                    RecipeHeader(
                        recipe = recipe,
                        customCategories = customCategories,
                        onCategoryChange = onCategoryChange,
                        onAddCategory = onAddCategory,
                    )
                }
                stickyHeader(key = "tabs") {
                    Box(
                        Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .onGloballyPositioned { stickyHeight = it.size.height.toFloat() },
                    ) {
                        Column {
                            RecipeTabs(activeTab, onTabSelected)
                            HairlineRule()
                        }
                    }
                }
                item(key = "content") {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            if (animations) {
                                ContentTransform(
                                    targetContentEnter = fadeIn(tween(220)) +
                                        slideInVertically(tween(220)) { slide },
                                    initialContentExit = fadeOut(tween(220)) +
                                        slideOutVertically(tween(220)) { -slide },
                                    // Content size jumps so a step jump can use the full scroll range.
                                    sizeTransform = null,
                                )
                            } else {
                                EnterTransition.None togetherWith ExitTransition.None
                            }
                        },
                        label = "recipeTab",
                    ) { tab ->
                        when (tab) {
                            RecipeTab.Ingredients -> IngredientsTab(recipe, onToggleIngredient)
                            RecipeTab.Steps -> StepsTab(
                                recipe,
                                highlightStep = highlightStep,
                                onToggle = onToggleStep,
                                scrollState = listState,
                                contentTop = listTop + stickyHeight,
                            )
                            RecipeTab.Times -> TimesTab(recipe) { index ->
                                highlightStep = index
                                onJumpToStep(index)
                            }
                            RecipeTab.Shopping -> ShoppingTab(
                                recipe = recipe,
                                addedTexts = addedShoppingTexts,
                                onAdd = onAddToShopping,
                                onOpenShopping = onOpenShopping,
                                shoppingCount = shoppingCount,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        DeleteDialog(
            title = recipe.title,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
        )
    }
}

@Composable
private fun RecipeActions(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    keepScreenAwake: Boolean,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Box(modifier = Modifier.weight(1f))
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
                    text = { Text(stringResource(R.string.keep_screen_awake)) },
                    onClick = {
                        menuOpen = false
                        onKeepScreenAwakeChange(!keepScreenAwake)
                    },
                    trailingIcon = {
                        if (keepScreenAwake) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_recipe)) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_recipe)) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun RecipeHeader(
    recipe: Recipe,
    customCategories: List<String>,
    onCategoryChange: (String?) -> Unit,
    onAddCategory: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column {
        RecipeImage(recipe)
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val meta = formatRecipeMeta(recipe)
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (recipe.url.isNotBlank()) {
                Text(
                    text = recipe.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { uriHandler.openUri(recipe.url) },
                )
            }
            CategoryPicker(
                selected = recipe.category,
                customCategories = customCategories,
                onSelect = onCategoryChange,
                onAddCategory = onAddCategory,
            )
        }
    }
}

@Composable
private fun CategoryPicker(
    selected: String?,
    customCategories: List<String>,
    onSelect: (String?) -> Unit,
    onAddCategory: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val selectedLabel = categoryName(selected)?.let { stringResource(it.label) }
        ?: selected?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.category_none)
    val customs = customCategories.filterNot { custom ->
        RecipeCategory.entries.any { it.name.equals(custom, ignoreCase = true) }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.field_category),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            TextButton(onClick = { menuOpen = true }) {
                Text(selectedLabel)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.category_none)) },
                    onClick = {
                        onSelect(null)
                        menuOpen = false
                    },
                )
                RecipeCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(stringResource(category.label)) },
                        onClick = {
                            onSelect(category.name)
                            menuOpen = false
                        },
                    )
                }
                customs.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelect(name)
                            menuOpen = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.category_new)) },
                    onClick = {
                        menuOpen = false
                        newName = ""
                        dialogOpen = true
                    },
                )
            }
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.new_category_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.new_category_field)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            val fixed = RecipeCategory.entries.firstOrNull {
                                it.name.equals(name, ignoreCase = true)
                            }
                            val existing = customs.firstOrNull {
                                it.equals(name, ignoreCase = true)
                            }
                            when {
                                fixed != null -> onSelect(fixed.name)
                                existing != null -> onSelect(existing)
                                else -> {
                                    onAddCategory(name)
                                    onSelect(name)
                                }
                            }
                        }
                        dialogOpen = false
                    },
                    enabled = newName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun RecipeImage(recipe: Recipe) {
    val shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    val fallback: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = recipe.title.trim().take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    val modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(shape)
    if (recipe.imageUrl == null) {
        Box(modifier) { fallback() }
    } else {
        SubcomposeAsyncImage(
            model = recipe.imageUrl,
            contentDescription = recipe.title,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = { fallback() },
            error = { fallback() },
        )
    }
}

@Composable
private fun RecipeTabs(activeTab: RecipeTab, onTabSelected: (RecipeTab) -> Unit) {
    val underline = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipeTab.entries.forEach { tab ->
            val selected = tab == activeTab
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .selectable(selected = selected, role = Role.Tab) { onTabSelected(tab) }
                    .padding(horizontal = 8.dp)
                    // horizontalScroll leaves width unbounded, so the underline is drawn.
                    .drawBehind {
                        if (selected) {
                            drawRect(
                                color = underline,
                                topLeft = Offset(0f, size.height - 2.dp.toPx()),
                                size = Size(size.width, 2.dp.toPx()),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(tab.label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) underline else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DeleteDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.delete_recipe_title)) },
        text = { Text(stringResource(R.string.delete_recipe_body, title)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
internal fun QuietNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/** Motion is skipped when the system animation scale is off. */
@Composable
internal fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
}

private val RecipeTab.label: Int
    @StringRes get() = when (this) {
        RecipeTab.Ingredients -> R.string.tab_ingredients
        RecipeTab.Steps -> R.string.tab_steps
        RecipeTab.Times -> R.string.tab_times
        RecipeTab.Shopping -> R.string.tab_shopping
    }

