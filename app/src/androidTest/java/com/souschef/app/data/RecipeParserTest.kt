package com.souschef.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeParserTest {
    private val parser = RecipeParser()

    @Test fun extract_returns_title_from_jsonld() = runBlocking {
        val html = """<script type="application/ld+json">{"@context":"https://schema.org","@type":"Recipe","name":"Test Cake","recipeIngredient":["1 egg"],"recipeInstructions":[{"@type":"HowToStep","text":"Bake at 180°C."}]}</script>"""
        val recipe = parser.extract(html, "https://example.com/cake")
        assertEquals("Test Cake", recipe.title)
        assertEquals(1, recipe.temperatures.size)
    }

    @Test fun temperatures_returns_objects() = runBlocking {
        assertEquals(listOf(180), parser.temperatures(listOf("Bake at 180°C.")).map { it.valueC })
    }

    @Test fun no_recipe_maps_to_code() = runBlocking {
        val error = assertThrows(ExtractionException::class.java) { runBlocking { parser.extract("<html></html>", "https://example.com") } }
        assertEquals("NO_RECIPE", error.code)
    }
}
