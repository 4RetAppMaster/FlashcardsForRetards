package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
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

        composeTestRule.onNodeWithTag("btn_header_help").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_header_help").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Quick Guide").assertIsDisplayed()
        composeTestRule.onNodeWithText("Got it").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("header_open_drawer").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("btn_help").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_help").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("How It Works").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Guide").assertIsDisplayed()
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
}
