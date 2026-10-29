package com.souschef.app.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/** JSON contract of `souschef.extract.extract` in app/src/main/python. */
@Serializable
data class ExtractedRecipe(
    val title: String,
    val author: String? = null,
    val siteName: String? = null,
    val imageUrl: String? = null,
    val servings: String? = null,
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val totalMinutes: Int? = null,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val temperatures: List<Temperature> = emptyList(),
    val missing: List<String> = emptyList(),
)

@Serializable
data class Recipe(
    val id: String,
    val url: String,
    val title: String,
    val siteName: String?,
    val author: String?,
    val imageUrl: String?,
    val servings: String?,
    val prepMinutes: Int?,
    val cookMinutes: Int?,
    val totalMinutes: Int?,
    val ingredients: List<Ingredient>,
    val steps: List<Step>,
    val temperatures: List<Temperature>,
    val savedAtEpochMs: Long,
    /** [RecipeCategory] name, or null for recipes saved before categories existed. */
    val category: String? = null,
)

enum class RecipeCategory { Appetizers, Sides, Mains, Dessert, Snacks }

@Serializable data class Ingredient(val id: String, val text: String, val checked: Boolean)
@Serializable data class Step(val id: String, val text: String, val checked: Boolean)

@Serializable
data class Temperature(
    val valueC: Int?,
    val valueF: Int?,
    val raw: String,
    val context: String,
    val stepIndex: Int?,
)

@Serializable
data class ShoppingItem(
    val id: String,
    val text: String,
    val checked: Boolean,
    val recipeId: String,
    val recipeTitle: String,
    val addedAtEpochMs: Long,
)

@OptIn(ExperimentalSerializationApi::class)
val SousChefJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun decodeExtractedRecipe(json: String): ExtractedRecipe =
    SousChefJson.decodeFromString<ExtractedRecipe>(json)

fun ExtractedRecipe.toRecipe(id: String, url: String, savedAtEpochMs: Long): Recipe = Recipe(
    id = id,
    url = url,
    title = title,
    siteName = siteName,
    author = author,
    imageUrl = imageUrl,
    servings = servings,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    totalMinutes = totalMinutes,
    ingredients = ingredients.trimmedLines { Ingredient(UUID.randomUUID().toString(), it, checked = false) },
    steps = steps.trimmedLines { Step(UUID.randomUUID().toString(), it, checked = false) },
    temperatures = temperatures,
    savedAtEpochMs = savedAtEpochMs,
)

private fun <T> List<String>.trimmedLines(build: (String) -> T): List<T> =
    map { it.trim() }.filter { it.isNotEmpty() }.map(build)
