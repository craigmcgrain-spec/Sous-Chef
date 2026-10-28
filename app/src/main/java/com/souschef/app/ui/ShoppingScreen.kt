package com.souschef.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.souschef.app.R
import com.souschef.app.data.ShoppingItem
import com.souschef.app.ui.components.CheckRow
import com.souschef.app.ui.components.EmptyState
import com.souschef.app.ui.components.SectionLabel

@Composable
fun ShoppingScreen(
    items: List<ShoppingItem>,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        ShoppingTopBar(
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            onBack = onBack,
            onClearChecked = onClearChecked,
            onClearAll = { confirmClearAll = true },
        )
        if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyState(
                    title = stringResource(R.string.shopping_empty_title),
                    body = stringResource(R.string.shopping_empty_body),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
            ) {
                items.groupBy { it.recipeTitle }.forEach { (title, groupItems) ->
                    item(key = "heading-$title") {
                        SectionLabel(
                            text = title,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(groupItems, key = { it.id }) { item ->
                        CheckRow(
                            text = item.text,
                            checked = item.checked,
                            onCheckedChange = { onToggle(item.id) },
                        )
                    }
                }
            }
        }
    }

    if (confirmClearAll) {
        ClearAllDialog(
            onDismiss = { confirmClearAll = false },
            onConfirm = {
                confirmClearAll = false
                onClearAll()
            },
        )
    }
}

@Composable
private fun ShoppingTopBar(
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
) {
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
        Text(
            text = stringResource(R.string.shopping_list),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Box {
            IconButton(onClick = { onMenuOpenChange(true) }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_checked)) },
                    onClick = {
                        onMenuOpenChange(false)
                        onClearChecked()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_all)) },
                    onClick = {
                        onMenuOpenChange(false)
                        onClearAll()
                    },
                )
            }
        }
    }
}

@Composable
private fun ClearAllDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.clear_all_title)) },
        text = { Text(stringResource(R.string.clear_all_body)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.clear_all))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
