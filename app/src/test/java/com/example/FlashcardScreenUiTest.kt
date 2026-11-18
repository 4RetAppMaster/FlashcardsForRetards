package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.example.data.local.FlashcardDatabase
import com.example.data.repository.FlashcardRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FlashcardScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verify_drawer_and_quick_guide_visibility() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("btn_header_help").assertDoesNotExist()
        composeTestRule.onNodeWithTag("header_open_drawer").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("btn_help").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_help").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("How It Works").assertIsDisplayed()
        composeTestRule.onNodeWithText("File Format (CSV / XLSX)").assertIsDisplayed()
    }

    @Test
    fun verify_swipe_right_opens_drawer() {
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput {
            swipeRight(startX = 10f, endX = 500f)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_help").assertIsDisplayed()
    }

    @Test
    fun verify_card_flip_and_counter_shuffle() {
        val db = FlashcardDatabase.getInstance(composeTestRule.activity)
        val repo = FlashcardRepository(db.deckDao)
        runBlocking {
            repo.importDeck(
                deckId = "test_deck.csv",
                deckName = "test_deck",
                rawRows = listOf(
                    listOf("Alpha", "1"),
                    listOf("Beta", "2"),
                    listOf("Gamma", "3"),
                    listOf("Delta", "4"),
                    listOf("Epsilon", "5")
                )
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("card_counter").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        // In original order initially: no shuffle icon
        composeTestRule.onNodeWithTag("icon_shuffle", useUnmergedTree = true).assertDoesNotExist()

        composeTestRule.onNodeWithTag("btn_mode_A").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Alpha", useUnmergedTree = true).assertIsDisplayed()

        // Tap mid card to flip to side B
        composeTestRule.onNodeWithTag("flashcard_clickable").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1", useUnmergedTree = true).assertIsDisplayed()

        // Advance to card 2 / 5
        composeTestRule.onNodeWithTag("btn_next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 / 5", useUnmergedTree = true).assertIsDisplayed()

        // Tap top-right counter to shuffle: resets to 1 / 5 and shows shuffle icon before counter
        composeTestRule.onNodeWithTag("card_counter").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("icon_shuffle", useUnmergedTree = true).assertIsDisplayed()
                composeTestRule.onNodeWithText("1 / 5", useUnmergedTree = true).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // Tap top-right counter again to restore original order and hide shuffle icon
        composeTestRule.onNodeWithTag("card_counter").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeTestRule.onNodeWithTag("icon_shuffle", useUnmergedTree = true).assertDoesNotExist()
                composeTestRule.onNodeWithText("Alpha", useUnmergedTree = true).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }
}
