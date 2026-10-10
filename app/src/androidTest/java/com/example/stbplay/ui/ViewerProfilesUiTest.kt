package com.example.stbplay.ui

import androidx.compose.ui.test.*
import androidx.compose.runtime.*
import com.example.stbplay.data.ViewerPinMode
import com.example.stbplay.isAndroidTvDevice
import androidx.test.platform.app.InstrumentationRegistry
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
        rule.onNodeWithText("Kid").performClick()
        rule.runOnIdle { assertEquals(kid, selected) }
        rule.onNode(hasText("Owner") and hasClickAction()).performClick()
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
    @Test fun kidsSettingsHideOwnerControls() {
        prewarmBatch2PhoneCast(rule)
        rule.setContent { STBPlayTheme(ThemePreference.LIGHT) {
            Batch2AppFixture(selected = StbPlayTab.SETTINGS, settings = StbPlaySettingsState(kidsProfile = true, viewerName = "Kid"), session = SearchSession())
        } }
        rule.onNodeWithText("Who's watching?").assertIsDisplayed()
        rule.onNodeWithText("Parental controls").assertDoesNotExist()
        rule.onNodeWithText("Content sources").assertDoesNotExist()
    }
    @Test fun backFromSettingsPickerPreservesSettingsSubsection() {
        prewarmBatch2PhoneCast(rule)
        var visible by mutableStateOf(false)
        rule.setContent { STBPlayTheme(ThemePreference.LIGHT) {
            Batch2AppFixture(selected = StbPlayTab.SETTINGS, session = SearchSession(), onSwitchViewer = { visible = true })
            if (visible) ViewerSwitchDialog({ visible = false }) {
                ViewerProfilesScreen(listOf(ViewerProfile("owner", "Owner", 18)), "1234",
                    onSelected = { visible = false }, onSave = { _, _ -> }, onSetOwnerPin = {}, onBack = { visible = false })
            }
        } }
        rule.onNodeWithText("Who's watching?").performClick()
        rule.onNodeWithText("Add profile").assertIsDisplayed()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.runOnIdle { assertFalse(visible) }
        rule.onNodeWithText("Content sources").assertIsDisplayed()
        rule.onNodeWithText("Who's watching?").performClick()
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.runOnIdle { assertFalse(visible) }
        rule.onNodeWithText("Content sources").assertIsDisplayed()
    }
    @Test fun publicAddProfileAndKidsSkipEvenLegacyPin() {
        var selected: ViewerProfile? = null
        rule.setContent { STBPlayTheme(ThemePreference.LIGHT) {
            ViewerProfilesScreen(listOf(ViewerProfile("kid", "Kid", 10, pinHash = "legacy")), "1234",
                onSelected = { selected = it }, onSave = { _, _ -> }, onSetOwnerPin = {})
        } }
        val device = if (InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice()) "tv" else "phone"
        saveThemePreview(rule, "profiles-$device-ivory")
        rule.onNodeWithText("Add profile").performClick()
        rule.onNodeWithText("Name").assertIsDisplayed()
        rule.onNodeWithText("Protected content").assertDoesNotExist()
        rule.onNodeWithText("Every time I enter my profile").assertExists()
        rule.onNodeWithText("Only for restricted content").performScrollTo().performClick()
        rule.onNodeWithText("✓ Only for restricted content").assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Kid").performClick()
        rule.runOnIdle { assertEquals("kid", selected?.id) }
        rule.onNodeWithText("PIN").assertDoesNotExist()
    }
    @Test fun restrictedOnlyProfileEntersWithoutPrompt() {
        var selected: ViewerProfile? = null
        val profile = ViewerProfile("adult", "Adult", 30, pinHash = "stored", pinMode = ViewerPinMode.RESTRICTED_ONLY)
        rule.setContent { STBPlayTheme {
            ViewerProfilesScreen(listOf(profile), "1234", onSelected = { selected = it }, onSave = { _, _ -> }, onSetOwnerPin = {})
        } }
        rule.onNodeWithText("Adult").performClick()
        rule.runOnIdle { assertEquals(profile, selected) }
        rule.onNodeWithText("PIN").assertDoesNotExist()
    }

}
