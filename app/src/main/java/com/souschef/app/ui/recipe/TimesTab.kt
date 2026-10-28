package com.souschef.app.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souschef.app.R
import com.souschef.app.data.Recipe
import com.souschef.app.data.Temperature
import com.souschef.app.data.formatDuration
import com.souschef.app.data.formatTemperature
import com.souschef.app.ui.components.SectionLabel

@Composable
fun TimesTab(recipe: Recipe, onJumpToStep: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        val prep = formatDuration(recipe.prepMinutes)
        val cook = formatDuration(recipe.cookMinutes)
        val total = formatDuration(recipe.totalMinutes)
        if (prep != null || cook != null || total != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimeCell(stringResource(R.string.time_prep), prep, Modifier.weight(1f))
                TimeCell(stringResource(R.string.time_cook), cook, Modifier.weight(1f))
                TimeCell(stringResource(R.string.time_total), total, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }
        if (recipe.temperatures.isEmpty()) {
            QuietNote(stringResource(R.string.temperatures_empty))
        } else {
            recipe.temperatures.forEachIndexed { index, temperature ->
                TemperatureCard(
                    temperature = temperature,
                    stepIndex = temperature.stepIndex?.takeIf { it in recipe.steps.indices },
                    onJumpToStep = onJumpToStep,
                )
                if (index != recipe.temperatures.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TimeCell(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
        )
        Spacer(Modifier.height(4.dp))
        SectionLabel(label)
    }
}

@Composable
private fun TemperatureCard(
    temperature: Temperature,
    stepIndex: Int?,
    onJumpToStep: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row {
            Text(
                text = temperature.valueC?.let { "$it°C" } ?: formatTemperature(temperature),
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            if (temperature.valueC != null && temperature.valueF != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "/ ${temperature.valueF}°F",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        if (temperature.context.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = temperature.context,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (stepIndex != null) {
            Spacer(Modifier.height(12.dp))
            StepChip(
                label = stringResource(R.string.temperature_step, stepIndex + 1),
                onClick = { onJumpToStep(stepIndex) },
            )
        }
    }
}

@Composable
private fun StepChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                .heightIn(min = 32.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
