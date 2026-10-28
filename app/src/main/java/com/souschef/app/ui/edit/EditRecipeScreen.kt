package com.souschef.app.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.souschef.app.R
import com.souschef.app.data.Ingredient
import com.souschef.app.data.Recipe
import com.souschef.app.data.Step
import com.souschef.app.ui.components.HairlineRule
import com.souschef.app.ui.components.SectionLabel
import java.util.UUID

private data class LineDraft(val id: String, val text: String, val checked: Boolean)

private val LineSaver = listSaver<List<LineDraft>, String>(
    save = { lines -> lines.flatMap { listOf(it.id, it.checked.toString(), it.text) } },
    restore = { saved -> saved.chunked(3).map { LineDraft(it[0], it[2], it[1].toBooleanStrict()) } },
)

@Composable
fun EditRecipeScreen(
    recipe: Recipe,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: (Recipe) -> Unit,
) {
    var title by rememberSaveable(recipe.id) { mutableStateOf(recipe.title) }
    var servings by rememberSaveable(recipe.id) { mutableStateOf(recipe.servings.orEmpty()) }
    var prep by rememberSaveable(recipe.id) { mutableStateOf(recipe.prepMinutes?.toString().orEmpty()) }
    var cook by rememberSaveable(recipe.id) { mutableStateOf(recipe.cookMinutes?.toString().orEmpty()) }
    var total by rememberSaveable(recipe.id) { mutableStateOf(recipe.totalMinutes?.toString().orEmpty()) }
    var ingredients by rememberSaveable(recipe.id, stateSaver = LineSaver) {
        mutableStateOf(recipe.ingredients.map { LineDraft(it.id, it.text, it.checked) })
    }
    var steps by rememberSaveable(recipe.id, stateSaver = LineSaver) {
        mutableStateOf(recipe.steps.map { LineDraft(it.id, it.text, it.checked) })
    }
    var showTitleError by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    fun draft(): Recipe = recipe.copy(
        title = title.trim(),
        servings = servings.trim().ifEmpty { null },
        prepMinutes = prep.toIntOrNull(),
        cookMinutes = cook.toIntOrNull(),
        totalMinutes = total.toIntOrNull() ?: sumMinutes(prep.toIntOrNull(), cook.toIntOrNull()),
        ingredients = ingredients.mapNotNull { it.toIngredient() },
        steps = steps.mapNotNull { it.toStep() },
    )

    fun cancel() {
        if (draft() != recipe) confirmDiscard = true else onCancel()
    }

    fun save() {
        if (title.isBlank()) {
            showTitleError = true
            return
        }
        onSave(draft())
    }

    val ingredientReorder = rememberReorderState { from, to ->
        ingredients = moveItem(ingredients, from, to)
    }
    val stepReorder = rememberReorderState { from, to ->
        steps = moveItem(steps, from, to)
    }

    BackHandler { cancel() }

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
            EditActions(saving = saving, onBack = ::cancel, onSave = ::save)
            HairlineRule()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) showTitleError = false
                    },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    isError = showTitleError,
                    supportingText = if (showTitleError) {
                        { Text(stringResource(R.string.title_required)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text(stringResource(R.string.field_servings)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(prep, { prep = it }, stringResource(R.string.time_prep), Modifier.weight(1f))
                    NumberField(cook, { cook = it }, stringResource(R.string.time_cook), Modifier.weight(1f))
                    NumberField(total, { total = it }, stringResource(R.string.time_total), Modifier.weight(1f))
                }
                SectionLabel(stringResource(R.string.tab_ingredients))
                ingredients.forEachIndexed { index, line ->
                    LineRow(
                        line = line,
                        index = index,
                        keys = ingredients.map { it.id },
                        reorder = ingredientReorder,
                        reorderLabel = stringResource(R.string.reorder_ingredient, line.text),
                        removeLabel = stringResource(R.string.remove_ingredient, line.text),
                        placeholder = stringResource(R.string.field_ingredient),
                        onTextChange = { text -> ingredients = ingredients.replaceAt(index, line.copy(text = text)) },
                        onRemove = { ingredients = ingredients.filterIndexed { i, _ -> i != index } },
                    )
                }
                TextButton(onClick = { ingredients = ingredients + LineDraft(newId(), "", false) }) {
                    Text(stringResource(R.string.add_ingredient))
                }
                SectionLabel(stringResource(R.string.tab_steps))
                steps.forEachIndexed { index, line ->
                    LineRow(
                        line = line,
                        index = index,
                        keys = steps.map { it.id },
                        reorder = stepReorder,
                        reorderLabel = stringResource(R.string.reorder_step, line.text),
                        removeLabel = stringResource(R.string.remove_step, line.text),
                        placeholder = stringResource(R.string.field_step),
                        onTextChange = { text -> steps = steps.replaceAt(index, line.copy(text = text)) },
                        onRemove = { steps = steps.filterIndexed { i, _ -> i != index } },
                    )
                }
                TextButton(onClick = { steps = steps + LineDraft(newId(), "", false) }) {
                    Text(stringResource(R.string.add_step))
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        onCancel()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.keep_editing))
                }
            },
        )
    }
}

@Composable
private fun EditActions(saving: Boolean, onBack: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
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
        Text(
            text = stringResource(R.string.edit_recipe),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        if (saving) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp),
                strokeWidth = 2.dp,
            )
        }
        TextButton(onClick = onSave, enabled = !saving) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(5)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun LineRow(
    line: LineDraft,
    index: Int,
    keys: List<String>,
    reorder: ReorderState,
    reorderLabel: String,
    removeLabel: String,
    placeholder: String,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val currentKeys by rememberUpdatedState(keys)
    val moveUp = stringResource(R.string.move_up)
    val moveDown = stringResource(R.string.move_down)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (reorder.dragging == line.id) 1f else 0f)
            .graphicsLayer { translationY = reorder.offsetFor(line.id) }
            .onGloballyPositioned { coordinates ->
                // positionInRoot + size keeps rows that scroll off-screen measurable;
                // boundsInRoot clips them to zero.
                reorder.record(
                    line.id,
                    Rect(coordinates.positionInRoot(), coordinates.size.toSize()),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .pointerInput(line.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { reorder.start(line.id) },
                        onDragEnd = { reorder.end(line.id) },
                        onDragCancel = { reorder.end(line.id) },
                        onDrag = { change, amount ->
                            change.consume()
                            reorder.drag(line.id, currentKeys, amount.y)
                        },
                    )
                }
                .semantics {
                    contentDescription = reorderLabel
                    customActions = buildList {
                        if (index > 0) {
                            add(CustomAccessibilityAction(moveUp) { reorder.move(index, index - 1); true })
                        }
                        if (index < keys.lastIndex) {
                            add(CustomAccessibilityAction(moveDown) { reorder.move(index, index + 1); true })
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = line.text,
            onValueChange = onTextChange,
            placeholder = { Text(placeholder) },
            maxLines = 4,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = removeLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LineDraft.toIngredient(): Ingredient? {
    val text = text.trim()
    return if (text.isEmpty()) null else Ingredient(id = id, text = text, checked = checked)
}

private fun LineDraft.toStep(): Step? {
    val text = text.trim()
    return if (text.isEmpty()) null else Step(id = id, text = text, checked = checked)
}

private fun List<LineDraft>.replaceAt(index: Int, line: LineDraft): List<LineDraft> =
    mapIndexed { i, existing -> if (i == index) line else existing }

private fun sumMinutes(prep: Int?, cook: Int?): Int? =
    if (prep != null && cook != null) prep + cook else null

private fun newId(): String = UUID.randomUUID().toString()
