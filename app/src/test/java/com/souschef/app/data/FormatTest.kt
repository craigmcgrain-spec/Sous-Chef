package com.souschef.app.data

import com.souschef.app.sampleRecipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {

    @Test
    fun `duration formatting`() {
        assertEquals("3h 15m", formatDuration(195))
        assertEquals("3h", formatDuration(180))
        assertEquals("45m", formatDuration(45))
        assertNull(formatDuration(null))
    }

    @Test
    fun `temperature formatting pairs units`() {
        assertEquals("180°C / 350°F", formatTemperature(Temperature(180, 350, "180°C", "ctx", 0)))
        assertEquals("350°F", formatTemperature(Temperature(null, 350, "350 F", "ctx", 0)))
    }

    @Test
    fun `meta omits unknown parts`() {
        val recipe = sampleRecipe().copy(siteName = "BBC Good Food", servings = null, totalMinutes = 180)
        assertEquals("BBC Good Food · 3h", formatRecipeMeta(recipe))
    }
}
