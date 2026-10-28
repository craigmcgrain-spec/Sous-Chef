package com.souschef.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    private val extraction_full = readResource("extraction_full.json")
    private val extraction_missing = readResource("extraction_missing.json")

    @Test
    fun `decode full and missing extraction`() {
        assertEquals("Sunday Ragù", decodeExtractedRecipe(extraction_full).title)
        assertNull(decodeExtractedRecipe(extraction_missing).cookMinutes)
        assertTrue(decodeExtractedRecipe(extraction_missing).temperatures.isEmpty())
    }

    @Test
    fun `toRecipe assigns unchecked lines and uuids`() {
        val recipe = decodeExtractedRecipe(extraction_full).toRecipe("id", "https://x", 0L)
        assertTrue(recipe.ingredients.all { !it.checked && it.id.isNotBlank() })
    }

    private fun readResource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader()
            .use { it.readText() }
}
