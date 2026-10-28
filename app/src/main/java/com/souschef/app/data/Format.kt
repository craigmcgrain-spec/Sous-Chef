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

fun formatRecipeMeta(recipe: Recipe): String = listOfNotNull(
    recipe.siteName,
    formatDuration(recipe.totalDurationMinutes()),
    recipe.servings?.let { "Serves $it" },
).joinToString(" · ")

/** Library rows show source and time only; servings belong on the recipe screen. */
fun formatLibraryMeta(recipe: Recipe): String = listOfNotNull(
    recipe.siteName,
    formatDuration(recipe.totalDurationMinutes()),
).joinToString(" · ")

private fun Recipe.totalDurationMinutes(): Int? = totalMinutes ?: when {
    prepMinutes == null && cookMinutes == null -> null
    else -> (prepMinutes ?: 0) + (cookMinutes ?: 0)
}
