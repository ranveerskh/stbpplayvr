package com.example.stbplay.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.media3.cast.Cast
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastContext
import com.example.stbplay.data.VodCatalogBatch
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.isAndroidTvDevice
import com.example.stbplay.ui.theme.STBPlayTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class PhoneNavigationSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Suppress("DEPRECATION")
    @Test fun touchNavigationAndSearchKeyboardKeepTheSelectedMediaType() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(!context.isAndroidTvDevice())
        rule.runOnIdle {
            // API 30's old Dynamite module requires synchronous main-thread creation.
            // This is emulator setup only; the app's Cast initialization is unchanged.
            CastContext.getSharedInstance(context)
            Cast.getSingletonInstance(context).initialize()
        }
        val movie = UiMedia("same", "Fixture movie", portrait = true)
        val channel = UiMedia("same", "Fixture channel", streamType = "live")
        val movieStream = PortalStream(movie.id, movie.title, null, null, "movie")
        val channelStream = PortalStream(channel.id, channel.title, null, null, "live")
        var selected by mutableStateOf(StbPlayTab.HOME)
        var clicked: UiMedia? = null
        rule.setContent {
            STBPlayTheme {
                StbPlayApp(
                    homeState = StbPlayHomeState(heroes = listOf(movie)),
                    liveState = StbPlayLibraryState(categories = listOf(UiCategory("all", "All")), items = listOf(channel)),
                    contentState = StbPlayLibraryState(categories = listOf(UiCategory("all", "All")), items = listOf(movie)),
                    favouritesState = StbPlayLibraryState(items = listOf(channel, movie)), settingsState = StbPlaySettingsState(),
                    onActivateLicense = {}, selectedTab = selected, liveChannelListState = rememberLazyListState(),
                    focusedLiveChannelId = null, focusedContentId = null, contentGridState = rememberLazyGridState(),
                    onTabSelected = { selected = it }, onLoadMoreContent = {}, onCategorySelected = { _, _ -> },
                    onMediaClick = { clicked = it }, onToggleFavorite = {}, onRemoveHistory = {}, onRefresh = {},
                    onClearCache = {}, onClearHistory = {}, onAddPortal = {}, onEditPortal = {}, onUsePortal = {},
                    onDeletePortal = {}, onProviderPair = {}, onPlayerPreferenceChanged = {}, onAndroidBoxVideoCompatibilityChanged = {},
                    onThemePreferenceChanged = {}, onSubtitlePreferenceChanged = {}, onCatalogueLanguageChanged = {},
                    onAnalyticsChanged = {}, onChangePin = {}, onParentalModeChanged = {}, onCheckUpdates = {}, onDownloadUpdate = {},
                    onShare = {}, onSearchVisibilityChanged = {},
                    searchCatalog = rememberSearchCatalog(selected, listOf(channelStream), listOf(movieStream), emptyList(), "All"),
                    searchRemote = { _, _ -> VodCatalogBatch(emptyList(), 1, 0, false) }, onSearchResults = {},
                    searchMedia = { UiMedia(it.id, it.name, streamType = it.streamType) }
                )
            }
        }
        rule.onNodeWithContentDescription("Movies").performClick()
        rule.waitForIdle()
        assertEquals(StbPlayTab.CONTENT, selected)
        rule.onNodeWithText(movie.title).performTouchInput { click() }
        rule.waitUntil(5_000) { clicked?.title == movie.title }
        assertEquals("movie", clicked?.streamType)
        rule.onNodeWithContentDescription("Home").performClick()
        rule.onNodeWithText("⌕").performClick()
        // Keep the query different from the result title so the matcher cannot hit the text field.
        rule.onNode(hasSetTextAction()).performTextInput("Fixture chan")
        rule.waitUntil(10_000) { rule.onAllNodesWithText(channel.title).fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle {
            val input = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            input.hideSoftInputFromWindow(rule.activity.window.decorView.windowToken, 0)
            clicked = null
        }
        rule.waitForIdle()
        rule.onNodeWithText(channel.title).assertIsDisplayed().performTouchInput { click() }
        rule.waitUntil(5_000) { clicked?.title == channel.title }
        assertEquals("same", clicked?.id)
        assertEquals("live", clicked?.streamType)
        assertEquals(StbPlayTab.HOME, selected)
    }
}
