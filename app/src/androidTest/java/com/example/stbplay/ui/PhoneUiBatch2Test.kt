@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.toPixelMap
import androidx.test.platform.app.InstrumentationRegistry
import com.example.stbplay.data.CategoryDropdownPosition
import com.example.stbplay.data.ThemePreference
import com.example.stbplay.isAndroidTvDevice
import com.example.stbplay.ui.theme.STBPlayTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class PhoneUiBatch2Test {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @org.junit.Before fun setupPhoneCast() = prewarmBatch2PhoneCast(rule)
    private fun phoneOnly() = assumeTrue(!InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice())

    @Test fun settingsMoveCategoriesOnlyInLiveAndMoviesAndGridUsesTwoThreeFourColumns() {
        phoneOnly()
        var selected by mutableStateOf(StbPlayTab.HOME)
        var position by mutableStateOf(CategoryDropdownPosition.TOP)
        var columns by mutableStateOf(2)
        val session = SearchSession()
        rule.setContent {
            STBPlayTheme {
                Batch2AppFixture(selected, onTab = { selected = it }, session = session,
                    settings = StbPlaySettingsState(phoneCategoryPosition = position, phoneMovieColumns = columns),
                    onCategoryPosition = { position = it }, onColumns = { columns = it })
            }
        }
        rule.onNodeWithTag("phone-category-selector").assertDoesNotExist()
        rule.onNodeWithContentDescription("Movies").performClick()
        val topY = rule.onNodeWithTag("phone-category-selector").fetchSemanticsNode().boundsInRoot.top
        for (count in 2..4) {
            rule.onNodeWithTag("movie-columns:$count").performClick()
            rule.waitForIdle()
            assertEquals(count, columns)
            val first = rule.onNodeWithText("Film 1").fetchSemanticsNode().boundsInRoot
            val last = rule.onNodeWithText("Film $count").fetchSemanticsNode().boundsInRoot
            val next = rule.onNodeWithText("Film ${count + 1}").fetchSemanticsNode().boundsInRoot
            assertEquals(first.center.y, last.center.y, 1f)
            assertTrue(next.center.y > first.center.y)
            saveThemePreview(rule, "phone-grid-$count")
        }
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Appearance & language").performClick()
        rule.onNodeWithText("Category dropdown position").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(CategoryDropdownPosition.BOTTOM, position)
        rule.onNodeWithContentDescription("Movies").performClick()
        val bottomY = rule.onNodeWithTag("phone-category-selector").fetchSemanticsNode().boundsInRoot.top
        assertTrue(bottomY > topY + 200f)
        saveThemePreview(rule, "phone-movies-bottom")
        rule.onNodeWithTag("phone-category-selector").performClick()
        rule.onNodeWithText("Categories").assertIsDisplayed()
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Live").performClick()
        assertTrue(rule.onNodeWithTag("phone-category-selector").fetchSemanticsNode().boundsInRoot.top > topY + 200f)
        rule.onNodeWithContentDescription("Home").performClick()
        rule.onNodeWithTag("phone-category-selector").assertDoesNotExist()
    }

    @Test fun ivoryFocusedLiveRowIsCompactAndHasALightReadableBackground() {
        phoneOnly()
        rule.setContent {
            STBPlayTheme(preference = ThemePreference.LIGHT) {
                Batch2AppFixture(StbPlayTab.LIVE, session = SearchSession(),
                    settings = StbPlaySettingsState(phoneCategoryPosition = CategoryDropdownPosition.BOTTOM))
            }
        }
        rule.waitForIdle()
        val row = rule.onNodeWithTag("live-row:ch1")
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertEquals(48f * density, row.fetchSemanticsNode().boundsInRoot.height, 1f)
        val pixels = row.captureToImage().toPixelMap()
        // Sample the focused surface away from its logo, text, border and favourite control.
        val fill = pixels[(pixels.width * .65f).toInt(), (pixels.height * .88f).toInt()]
        assertTrue("Focused Ivory fill must not turn black", fill.red > .65f && fill.green > .65f && fill.blue > .50f)
        rule.onNodeWithText("Channel 1").assertIsDisplayed()
        saveThemePreview(rule, "phone-live-ivory-bottom")
    }

    @Test fun phoneSurfacesAndButtonsKeepTheirBoundsWhenFocused() {
        phoneOnly()
        val surface = FocusRequester()
        val button = FocusRequester()
        lateinit var input: InputModeManager
        rule.setContent {
            input = LocalInputModeManager.current
            STBPlayTheme {
                Column {
                    QuestSurface({}, Modifier.width(180.dp).height(60.dp).testTag("static-surface").focusRequester(surface)) {}
                    QuestButton({}, Modifier.width(180.dp).height(60.dp).testTag("static-button").focusRequester(button)) {}
                }
            }
        }
        val originalSurface = rule.onNodeWithTag("static-surface").fetchSemanticsNode().boundsInRoot
        val originalButton = rule.onNodeWithTag("static-button").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard); surface.requestFocus() }
        rule.waitForIdle()
        rule.onNodeWithTag("static-surface").assertIsFocused()
        assertEquals(originalSurface, rule.onNodeWithTag("static-surface").fetchSemanticsNode().boundsInRoot)
        rule.runOnIdle { button.requestFocus() }
        rule.waitForIdle()
        rule.onNodeWithTag("static-button").assertIsFocused()
        assertEquals(originalButton, rule.onNodeWithTag("static-button").fetchSemanticsNode().boundsInRoot)
    }
}
