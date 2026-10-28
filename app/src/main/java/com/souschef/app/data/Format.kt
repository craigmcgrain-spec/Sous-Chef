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
    formatServings(recipe.servings),
).joinToString(" · ")

private val SERVES_PREFIX = Regex("^serves\\s*:?\\s*", RegexOption.IGNORE_CASE)
private val SERVINGS_SUFFIX = Regex("\\s*servings?$", RegexOption.IGNORE_CASE)

/** Raw page yields ("6 servings", "Makes 12 cookies") become one display line. */
fun formatServings(value: String?): String? {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return null
    val rest = SERVES_PREFIX.replace(text, "").trim()
    if (rest != text) return rest.takeIf { it.isNotEmpty() }?.let { "Serves $it" }
    if (text.startsWith("makes", ignoreCase = true) || text.startsWith("yields", ignoreCase = true)) {
        return text.replaceFirstChar { it.uppercase() }
    }
    return "Serves " + text.replace(SERVINGS_SUFFIX, "").trim()
}

/** Library rows show source and time only; servings belong on the recipe screen. */
fun formatLibraryMeta(recipe: Recipe): String = listOfNotNull(
    recipe.siteName,
    formatDuration(recipe.totalDurationMinutes()),
).joinToString(" · ")

/** Total time for display, falling back to prep + cook when the page omits it. */
fun Recipe.totalDurationMinutes(): Int? = totalMinutes ?: when {
    prepMinutes == null && cookMinutes == null -> null
    else -> (prepMinutes ?: 0) + (cookMinutes ?: 0)
}
