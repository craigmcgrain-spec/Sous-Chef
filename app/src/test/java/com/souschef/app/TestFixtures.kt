package com.souschef.app

import com.souschef.app.data.Ingredient
import com.souschef.app.data.Recipe
import com.souschef.app.data.Step
import com.souschef.app.data.Temperature

fun sampleRecipe(id: String = "id-1", url: String = "https://example.com/r"): Recipe = Recipe(
    id = id,
    url = url,
    title = "Sunday Ragù",
    siteName = "BBC Good Food",
    author = "Anna Del Conte",
    imageUrl = null,
    servings = "4",
    prepMinutes = 15,
    cookMinutes = 180,
    totalMinutes = 195,
    ingredients = listOf(
        Ingredient("i-1", "2 tbsp olive oil", checked = true),
        Ingredient("i-2", "1kg beef shin", checked = false),
    ),
    steps = listOf(
        Step("s-1", "Heat the oven to 160°C.", checked = false),
        Step("s-2", "Brown the beef.", checked = true),
    ),
    temperatures = listOf(Temperature(160, 320, "160°C", "Heat the oven to 160°C.", 0)),
    savedAtEpochMs = 0L,
)
