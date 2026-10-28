package com.souschef.app.data

fun formatDuration(minutes: Int?): String? {
    if (minutes == null) return null
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

fun formatTemperature(t: Temperature): String {
    val parts = listOfNotNull(
        t.valueC?.let { "$it°C" },
        t.valueF?.let { "$it°F" },
    )
    return if (parts.isEmpty()) t.raw else parts.joinToString(" / ")
}

fun formatRecipeMeta(recipe: Recipe): String {
    val minutes = recipe.totalMinutes ?: when {
        recipe.prepMinutes == null && recipe.cookMinutes == null -> null
        else -> (recipe.prepMinutes ?: 0) + (recipe.cookMinutes ?: 0)
    }
    return listOfNotNull(
        recipe.siteName,
        formatDuration(minutes),
        recipe.servings?.let { "Serves $it" },
    ).joinToString(" · ")
}
