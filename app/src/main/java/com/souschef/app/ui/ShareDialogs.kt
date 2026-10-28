package com.souschef.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.souschef.app.R
import com.souschef.app.data.extractSharedUrl

/** Progress and error dialog for a shared link; idle and done render nothing. */
@Composable
fun ShareDialog(
    state: ShareUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onPasteLink: () -> Unit,
) {
    when (state) {
        is ShareUiState.Loading -> AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.share_reading, state.domain)) },
            text = {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )

        is ShareUiState.Failure -> AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.share_failure_title)) },
            text = {
                Column {
                    Text(state.message)
                    if (state.url.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.url,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            confirmButton = {
                Row {
                    if (state.canRetry) {
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    TextButton(onClick = onPasteLink) {
                        Text(stringResource(R.string.paste_different_link))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )

        ShareUiState.Idle, is ShareUiState.Done -> Unit
    }
}

@Composable
fun PasteLinkDialog(onDismiss: () -> Unit, onOpen: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val open = {
        // Share parsing already knows how to pull one http(s) link out of text.
        val link = extractSharedUrl(text)?.takeIf { it == text.trim() }
        if (link != null) {
            onOpen(link)
        } else {
            invalid = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.paste_link)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        invalid = false
                    },
                    label = { Text(stringResource(R.string.field_link)) },
                    singleLine = true,
                    isError = invalid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { open() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
                if (invalid) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.paste_link_invalid),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = open) {
                Text(stringResource(R.string.open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
