package com.example.stbplay.ui

import androidx.compose.ui.test.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
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
    @Test fun adultAppearanceOffersSeparateThemesWithoutReplacingSavedNormalTheme() {
        prewarmBatch2PhoneCast(rule)
        val manager = com.example.stbplay.data.SettingsManager(rule.activity)
        val old = kotlinx.coroutines.runBlocking { manager.glassAppearance.first() }
        val normal = kotlinx.coroutines.runBlocking { manager.themePreference.first() }
        try {
            rule.setContent {
                val appearance by manager.glassAppearance.collectAsState(initial = old)
                STBPlayTheme(ThemePreference.GLASS, adultOnly = true, appearance = appearance) {
                    Batch2AppFixture(selected = StbPlayTab.SETTINGS,
                        settings = StbPlaySettingsState(parentalMode = com.example.stbplay.data.ParentalMode.ADULT_ONLY),
                        session = SearchSession())
                }
            }
            rule.onNodeWithText("Appearance & language").performScrollTo().performClick()
            rule.onNodeWithText("After Dark", substring = true).assertIsDisplayed()
            rule.onNodeWithText("Ivory").assertDoesNotExist()
            rule.onNodeWithText("Adult Glass", substring = true).performClick()
            rule.waitUntil(10_000) {
                rule.onAllNodesWithText("Midnight Velvet", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            saveThemePreview(rule, "adult-glass-settings-" + if (rule.activity.isAndroidTvDevice()) "tv" else "phone")
            kotlinx.coroutines.runBlocking {
                assertEquals(com.example.stbplay.data.AdultThemePreference.GLASS, manager.glassAppearance.first().adultTheme)
                assertEquals(normal, manager.themePreference.first())
            }
        } finally { kotlinx.coroutines.runBlocking { manager.setAdultTheme(old.adultTheme) } }
    }

    @Test fun validOwnerAndPersonalPinsUnlockWithoutSubmitting() {
        var ownerCalls = 0
        rule.setContent { STBPlayTheme { PinPrompt("Owner", "1234", { ownerCalls++ }, {}) } }
        rule.onNodeWithText("1").performClick()
        rule.runOnIdle { assertEquals(0, ownerCalls) }
        saveThemePreview(rule, "pin-glass-" + if (rule.activity.isAndroidTvDevice()) "tv" else "phone")
        listOf("2", "3", "4").forEach { rule.onNodeWithText(it).performClick() }
        rule.waitUntil(5_000) { ownerCalls == 1 }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(1, ownerCalls) }
    }
    @Test fun personalPinAutoUnlocksAndRejectsWrongDigits() {
        var calls = 0
        val hash = com.example.stbplay.data.encodeViewerPin("1234")
        rule.setContent { STBPlayTheme {
            ViewerPinPrompt(ViewerProfile("adult", "Adult", 30, pinHash = hash), { calls++ }, {})
        } }
        if (rule.activity.isAndroidTvDevice()) rule.onNodeWithTag("profile-field:PIN").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("9999")
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(0, calls) }
        if (rule.activity.isAndroidTvDevice() && rule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
            rule.onNodeWithTag("profile-field:PIN").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("1234")
        rule.waitUntil(10_000) { calls == 1 }
        rule.runOnIdle { assertEquals(1, calls) }
    }
    @Test fun glassProfilesRemainReadableAndKidsDoNotRequirePin() {
        val owner = ViewerProfile("owner", "Owner", 18, avatar = "family:indian_dad")
        val user = ViewerProfile("user", "User", 25, avatar = "family:indian_mom")
        val kid = ViewerProfile("kid", "Kids", 10, avatar = "family:indian_boy")
        var selected: ViewerProfile? = null
        rule.setContent { STBPlayTheme(ThemePreference.GLASS) {
            ViewerProfilesScreen(listOf(owner, user, kid), "1234", onSelected = { selected = it },
                onSave = { _, _ -> }, onSetOwnerPin = {})
        } }
        rule.onNodeWithText("Who's watching?").assertIsDisplayed()
        saveThemePreview(rule, "profiles-glass-" + if (rule.activity.isAndroidTvDevice()) "tv" else "phone")
        rule.onNodeWithText("Kids").performClick()
        rule.runOnIdle { assertEquals(kid, selected) }
        rule.onNodeWithText("Unlock").assertDoesNotExist()
    }

    @Test fun glassSettingsAndBackKeepExistingSection() {
        prewarmBatch2PhoneCast(rule)
        var visible by mutableStateOf(false)
        rule.setContent { STBPlayTheme(ThemePreference.GLASS) {
            Batch2AppFixture(selected = StbPlayTab.SETTINGS, session = SearchSession(), onSwitchViewer = { visible = true })
            if (visible) ViewerSwitchDialog({ visible = false }) {
                ViewerProfilesScreen(listOf(ViewerProfile("owner", "Owner", 18)), "1234",
                    onSelected = { visible = false }, onSave = { _, _ -> }, onSetOwnerPin = {}, onBack = { visible = false })
            }
        } }
        saveThemePreview(rule, "settings-glass-" + if (rule.activity.isAndroidTvDevice()) "tv" else "phone")
        rule.onNodeWithText("Who's watching?").performClick()
        rule.onNodeWithText("Add profile").assertIsDisplayed()
        rule.waitForIdle()
        androidx.test.uiautomator.UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        rule.waitUntil(5_000) { !visible }
        rule.onNodeWithText("Add profile").assertDoesNotExist()
        rule.onNodeWithText("Content sources").assertIsDisplayed()
        rule.runOnIdle { assertFalse(visible) }
    }

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
        rule.waitForIdle()
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
        rule.waitForIdle()
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
        rule.waitForIdle()
        rule.onNodeWithText("Edit Owner").assertExists()
        val television = InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice()
        if (television) rule.onNode(hasText("Owner") and hasClickAction()).performScrollTo().performClick()
        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Ranveer")
        if (television) rule.onNode(hasSetTextAction()).performImeAction()
        rule.onNodeWithContentDescription("Punjabi Dad").performScrollTo().performClick()
        rule.onNodeWithText("Save profile").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals("owner", saved?.id); assertEquals("Ranveer", saved?.name)
            assertEquals("family:punjabi_dad", saved?.avatar)
        }
    }

}
