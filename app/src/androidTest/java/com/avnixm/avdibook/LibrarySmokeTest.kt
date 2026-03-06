package com.avnixm.avdibook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class LibrarySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun libraryScreen_rendersImportActions() {
        composeRule.onNodeWithText("Import Folder").assertIsDisplayed()
        composeRule.onNodeWithText("Import Files").assertIsDisplayed()
    }
}
