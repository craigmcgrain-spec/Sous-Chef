package com.souschef.app.ui.recipe

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.souschef.app.R
import com.souschef.app.data.Recipe
import com.souschef.app.data.Step
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@Composable
fun StepsTab(
    recipe: Recipe,
    highlightStep: Int?,
    onToggle: (String) -> Unit,
    scrollState: LazyListState? = null,
    contentTop: Float = 0f,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (recipe.steps.isEmpty()) {
            QuietNote(stringResource(R.string.steps_empty))
        } else {
            val done = recipe.steps.count { it.checked }
            val animations = rememberAnimationsEnabled()
            Text(
                text = stringResource(R.string.steps_progress, done, recipe.steps.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val fraction by animateFloatAsState(
                targetValue = done.toFloat() / recipe.steps.size,
                animationSpec = if (animations) tween(220) else tween(0),
                label = "progress",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
            Spacer(Modifier.height(16.dp))
            val density = LocalDensity.current
            var highlightedTop by remember { mutableStateOf<Float?>(null) }
            LaunchedEffect(highlightStep) {
                if (highlightStep == null || scrollState == null || contentTop <= 0f) return@LaunchedEffect
                val top = snapshotFlow { highlightedTop }.filterNotNull().first()
                val margin = with(density) { 8.dp.toPx() }
                scrollState.scrollBy(top - contentTop - margin)
            }
            recipe.steps.forEachIndexed { index, step ->
                StepCard(
                    number = index + 1,
                    step = step,
                    highlighted = highlightStep == index,
                    onToggle = onToggle,
                    modifier = if (highlightStep == index) {
                        Modifier.onGloballyPositioned { highlightedTop = it.positionInRoot().y }
                    } else {
                        Modifier
                    },
                )
                if (index != recipe.steps.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    step: Step,
    highlighted: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val animations = rememberAnimationsEnabled()
    val shape = RoundedCornerShape(12.dp)
    val done = step.checked
    val state = stringResource(if (done) R.string.state_done else R.string.state_not_done)
    val checkScale = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done && animations) {
            checkScale.animateTo(1.15f, tween(110))
            checkScale.animateTo(1f, tween(110))
        } else {
            checkScale.snapTo(1f)
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (highlighted) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .toggleable(value = done, role = Role.Checkbox) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
                onToggle(step.id)
            }
            .semantics { stateDescription = state }
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = MaterialTheme.typography.titleLarge,
            color = if (done) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            StruckText(
                text = step.text,
                struck = done,
                color = if (done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        if (done) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.graphicsLayer {
                    scaleX = checkScale.value
                    scaleY = checkScale.value
                },
            )
        }
    }
}

/** Draws the strike so it can grow in with the tab motion; no decoration when unstruck. */
@Composable
private fun StruckText(text: String, struck: Boolean, color: Color) {
    val animations = rememberAnimationsEnabled()
    val progress by animateFloatAsState(
        targetValue = if (struck) 1f else 0f,
        animationSpec = if (animations) tween(220) else tween(0),
        label = "strike",
    )
    val strikeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val stroke = with(LocalDensity.current) { 1.5.dp.toPx() }
    var lines by remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        onTextLayout = { layout ->
            lines = (0 until layout.lineCount).map { line ->
                val y = (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f
                y to layout.getLineRight(line)
            }
        },
        modifier = Modifier.drawWithContent {
            drawContent()
            if (progress > 0f) {
                lines.forEach { (y, right) ->
                    drawLine(
                        color = strikeColor,
                        start = Offset(0f, y),
                        end = Offset(right * progress, y),
                        strokeWidth = stroke,
                    )
                }
            }
        },
    )
}
