package com.souschef.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.souschef.app.ui.theme.SousChefTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareDialogsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_dialog_shows_domain_and_cancel() {
        var dismissed = 0
        composeRule.setContent {
            SousChefTheme {
                ShareDialog(
                    state = ShareUiState.Loading("bbcgoodfood.com"),
                    onRetry = {},
                    onDismiss = { dismissed++ },
                    onPasteLink = {},
                )
            }
        }
        composeRule.onNodeWithText("Reading bbcgoodfood.com…").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun failure_shows_message_and_retry() {
        var retries = 0
        composeRule.setContent {
            SousChefTheme {
                ShareDialog(
                    state = ShareUiState.Failure(
                        url = "https://example.com/recipe",
                        message = "That page isn't available (error 404).",
                        canRetry = true,
                    ),
                    onRetry = { retries++ },
                    onDismiss = {},
                    onPasteLink = {},
                )
            }
        }
        composeRule.onNodeWithText("Couldn't read that recipe").assertIsDisplayed()
        composeRule.onNodeWithText("That page isn't available (error 404).").assertIsDisplayed()
        composeRule.onNodeWithText("https://example.com/recipe").assertIsDisplayed()
        composeRule.onNodeWithText("Paste a different link").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun no_link_failure_offers_paste() {
        var pastes = 0
        composeRule.setContent {
            SousChefTheme {
                ShareDialog(
                    state = ShareUiState.Failure(
                        url = "",
                        message = "That share didn't include a link.",
                        canRetry = false,
                    ),
                    onRetry = {},
                    onDismiss = {},
                    onPasteLink = { pastes++ },
                )
            }
        }
        composeRule.onNodeWithText("Paste a link").assertIsDisplayed().performClick()
        assertEquals(1, pastes)
    }

    @Test
    fun paste_dialog_strips_trailing_punctuation() {
        var opened: String? = null
        composeRule.setContent {
            SousChefTheme {
                PasteLinkDialog(onDismiss = {}, onOpen = { opened = it })
            }
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("https://x.com/a.")
        composeRule.onNodeWithText("Open").performClick()
        assertEquals("https://x.com/a", opened)
    }

    @Test
    fun paste_dialog_rejects_non_http() {
        var opened: String? = null
        composeRule.setContent {
            SousChefTheme {
                PasteLinkDialog(onDismiss = {}, onOpen = { opened = it })
            }
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("not a link")
        composeRule.onNodeWithText("Open").performClick()
        composeRule.onNodeWithText("Enter a link starting with http:// or https://.").assertIsDisplayed()
        assertNull(opened)
    }
}
