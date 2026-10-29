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
        assertEquals("350°F / 180°C", formatTemperature(Temperature(180, 350, "180°C", "ctx", 0)))
        assertEquals("350°F", formatTemperature(Temperature(null, 350, "350 F", "ctx", 0)))
    }

    @Test
    fun `servings normalize raw yields`() {
        assertEquals("Serves 6", formatServings("6 servings"))
        assertEquals("Serves 4", formatServings("4"))
        assertEquals("Makes 12 cookies", formatServings("Makes 12 cookies"))
        assertEquals("Makes 12 cookies", formatServings("makes 12 cookies"))
        assertEquals("Yields 8", formatServings("Yields 8"))
        assertEquals("Serves 4", formatServings("Serves 4"))
        assertEquals("Serves 4", formatServings("serves: 4"))
        assertNull(formatServings(null))
        assertNull(formatServings("   "))
    }

    @Test
    fun `meta uses the normalized servings`() {
        val recipe = sampleRecipe().copy(servings = "6 servings", siteName = null, totalMinutes = null)
        assertEquals("3h 15m · Serves 6", formatRecipeMeta(recipe))
    }

    @Test
    fun `total falls back to prep plus cook`() {
        assertEquals(195, sampleRecipe().totalDurationMinutes())
        assertEquals(45, sampleRecipe().copy(totalMinutes = null, prepMinutes = 15, cookMinutes = 30).totalDurationMinutes())
        assertNull(sampleRecipe().copy(totalMinutes = null, prepMinutes = null, cookMinutes = null).totalDurationMinutes())
    }

    @Test
    fun `meta omits unknown parts`() {
        val recipe = sampleRecipe().copy(siteName = "BBC Good Food", servings = null, totalMinutes = 180)
        assertEquals("BBC Good Food · 3h", formatRecipeMeta(recipe))
    }

    @Test
    fun `library meta omits servings`() {
        assertEquals("BBC Good Food · 3h 15m", formatLibraryMeta(sampleRecipe()))
        assertEquals("3h 15m", formatLibraryMeta(sampleRecipe().copy(siteName = null, totalMinutes = null)))
    }
}
