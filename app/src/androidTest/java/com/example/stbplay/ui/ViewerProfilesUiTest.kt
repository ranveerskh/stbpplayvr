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
        rule.onNodeWithText("Manage profiles").performClick()
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
        androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
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
        rule.onNodeWithText("✓ Every time I enter my profile").assertExists()
        rule.onNodeWithText("Only for restricted content").performScrollTo().performClick()
        rule.onNodeWithText("✓ Only for restricted content").assertExists()
        saveThemePreview(rule, "profile-form-$device-ivory")
        androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
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

    @Test fun newAdultCannotEnterBeforeOwnerApproval() {
        var selected: ViewerProfile? = null
        val profile = ViewerProfile("new-adult", "New adult", 30, pinHash = "stored",
            pinMode = ViewerPinMode.RESTRICTED_ONLY, adultApproved = false)
        var approved: ViewerProfile? = null
        rule.setContent { STBPlayTheme {
            ViewerProfilesScreen(listOf(profile), "1234", onSelected = { selected = it },
                onSave = { saved, _ -> approved = saved }, onSetOwnerPin = {})
        } }
        rule.onNodeWithText("New adult").performClick()
        rule.onNodeWithText("Unlock").performClick()
        rule.onNodeWithText("Incorrect PIN").assertIsDisplayed()
        rule.runOnIdle { assertNull(selected); assertNull(approved) }
        listOf("1", "2", "3", "4").forEach { rule.onNodeWithText(it).performClick() }
        rule.onNodeWithText("Unlock").performClick()
        rule.runOnIdle { assertEquals(true, approved?.adultApproved); assertEquals(approved, selected) }
    }

    @Test fun profileTilesShowOnlyNamesAndSelfDeletionNeedsConfirmation() {
        val profile = ViewerProfile("self-delete", "Aman", 19, avatar = "family:punjabi_boy")
        var deleted: String? = null
        rule.setContent { STBPlayTheme {
            ViewerProfilesScreen(listOf(ViewerProfile("owner", "Owner", 18), profile), "1234",
                activeViewerId = profile.id, onSelected = {}, onSave = { _, _ -> }, onSetOwnerPin = {},
                onDelete = { id, actor, owner ->
                    assertEquals(profile.id, actor); assertFalse(owner); deleted = id
                })
        } }
        rule.onNodeWithText("Profile PIN").assertDoesNotExist()
        rule.onNodeWithText("Restricted content PIN").assertDoesNotExist()
        rule.onNodeWithText("Kids · No PIN").assertDoesNotExist()
        rule.onNodeWithText("Edit my profile").performScrollTo().performClick()
        rule.onNodeWithText("Edit Owner").assertDoesNotExist()
        rule.onNodeWithText("Delete profile").performScrollTo().performClick()
        rule.runOnIdle { assertNull(deleted) }
        rule.onNodeWithText("Delete Aman?").assertIsDisplayed()
        rule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        rule.runOnIdle { assertNull(deleted) }
        rule.onNodeWithText("Delete profile").performScrollTo().performClick()
        rule.onNode(hasText("Delete profile") and hasAnyAncestor(isDialog())).performClick()
        rule.runOnIdle { assertEquals(profile.id, deleted) }
    }

    @Test fun ownerCanEditOwnNameAndAvatarWithoutChangingOwnerPin() {
        var saved: ViewerProfile? = null
        val owner = ViewerProfile("owner", "Owner", 18)
        rule.setContent { STBPlayTheme {
            ViewerProfilesScreen(listOf(owner), "1234", onSelected = {},
                onSave = { profile, pin -> assertEquals("", pin); saved = profile }, onSetOwnerPin = {})
        } }
        rule.onNodeWithText("Manage profiles").performScrollTo().performClick()
        listOf("1", "2", "3", "4").forEach { rule.onNodeWithText(it).performClick() }
        rule.onNodeWithText("Unlock").performClick()
        rule.onNodeWithText("Edit Owner").assertExists()
        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Ranveer")
        rule.onNodeWithContentDescription("Punjabi Dad").performScrollTo().performClick()
        rule.onNodeWithText("Save profile").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals("owner", saved?.id); assertEquals("Ranveer", saved?.name)
            assertEquals("family:punjabi_dad", saved?.avatar)
        }
    }

}
