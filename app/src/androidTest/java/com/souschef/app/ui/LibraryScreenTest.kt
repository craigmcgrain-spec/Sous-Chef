package com.souschef.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.souschef.app.sampleRecipe
import com.souschef.app.ui.theme.SousChefTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun empty_state_offers_paste() {
        var pasteCalls = 0
        composeRule.setContent {
            SousChefTheme {
                LibraryScreen(emptyList(), 0, {}, {}, { pasteCalls++ })
            }
        }
        composeRule.onNodeWithText("Nothing cooking yet").assertIsDisplayed()
        composeRule.onNodeWithText("Paste a link").performClick()
        assertEquals(1, pasteCalls)
    }

    @Test
    fun list_shows_title_and_meta() {
        composeRule.setContent {
            SousChefTheme {
                LibraryScreen(listOf(sampleRecipe()), 0, {}, {}, {})
            }
        }
        composeRule.onNodeWithText("Sunday Ragù").assertIsDisplayed()
        composeRule.onNodeWithText("BBC Good Food · 3h 15m").assertIsDisplayed()
    }

    @Test
    fun row_opens_recipe() {
        var opened: String? = null
        composeRule.setContent {
            SousChefTheme {
                LibraryScreen(listOf(sampleRecipe()), 0, { opened = it }, {}, {})
            }
        }
        composeRule.onNodeWithText("Sunday Ragù").performClick()
        assertEquals("id-1", opened)
    }

    @Test
    fun cart_badge_shows_unchecked_count() {
        var opened = false
        composeRule.setContent {
            SousChefTheme {
                LibraryScreen(listOf(sampleRecipe()), 3, {}, { opened = true }, {})
            }
        }
        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Shopping list").performClick()
        assertTrue(opened)
    }

    @Test
    fun wide_window_shows_two_columns() {
        val second = sampleRecipe("id-2").copy(
            title = "Roast Tomato Soup",
            siteName = "Ottolenghi",
            totalMinutes = 55,
        )
        composeRule.setContent {
            SousChefTheme {
                Box(Modifier.requiredWidth(600.dp).fillMaxHeight()) {
                    LibraryScreen(listOf(sampleRecipe(), second), 0, {}, {}, {})
                }
            }
        }
        val firstBounds = composeRule.onNodeWithText("Sunday Ragù").fetchSemanticsNode().boundsInRoot
        val secondBounds = composeRule.onNodeWithText("Roast Tomato Soup").fetchSemanticsNode().boundsInRoot
        assertEquals(firstBounds.top, secondBounds.top, 1f)
        assertTrue(secondBounds.left > firstBounds.left)
    }
}
