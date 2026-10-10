package com.example.stbplay.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.stbplay.data.ViewerProfile
import com.example.stbplay.ui.theme.STBPlayTheme
import com.example.stbplay.data.ThemePreference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ViewerProfilesUiTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun ownerSelectionAndManagementRequireOwnerPin() {
        val owner = ViewerProfile("owner", "Owner", 18)
        val kid = ViewerProfile("kid", "Kid", 10)
        var selected: ViewerProfile? = null
        rule.setContent { STBPlayTheme(ThemePreference.LIGHT) {
            ViewerProfilesScreen(listOf(owner, kid), "1234", onSelected = { selected = it }, onSave = { _, _ -> }, onSetOwnerPin = {})
        } }
        rule.onNodeWithText("Who's watching?").assertIsDisplayed()
        rule.onNodeWithText("  Kid · Kids").performClick()
        rule.runOnIdle { assertEquals(kid, selected) }
        rule.onNodeWithText("  Owner").performClick()
        rule.onNodeWithText("Unlock").performClick()
        rule.onNodeWithText("Incorrect PIN").assertIsDisplayed()
        rule.runOnIdle { assertEquals(kid, selected) }
        listOf("1", "2", "3", "4").forEach { rule.onNodeWithText(it).performClick() }
        rule.onNodeWithText("Unlock").performClick()
        rule.runOnIdle { assertEquals(owner, selected) }
        rule.onNodeWithText("Manage profiles · Owner PIN").performClick()
        rule.onNodeWithText("Protected content").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Who's watching?").assertIsDisplayed()
    }
}
