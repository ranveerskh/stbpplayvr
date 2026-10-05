package com.example.stbplay.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import com.example.stbplay.ui.screens.TvLivePreviewPanel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.stbplay.data.VodCatalogBatch
import com.example.stbplay.isAndroidTvDevice
import com.example.stbplay.ui.theme.STBPlayTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TvNavigationFocusTest {
    @get:Rule val rule = createComposeRule()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val channels = (1..45).map { UiMedia("ch$it", "Channel $it", streamType = "live") }
    private val movies = (1..35).map { UiMedia("film$it", "Movie $it", portrait = true) }
    private var selected by mutableStateOf(StbPlayTab.HOME)
    private var loading by mutableStateOf(false)
    private var focusedMovie by mutableStateOf<String?>(null)
    private var favorites by mutableStateOf(channels.take(4) + movies.take(4))
    private var played = 0
    private var previewChannel by mutableStateOf<UiMedia?>(null)

    private fun start(tab: StbPlayTab = StbPlayTab.HOME) {
        assertTrue("Must run on a real Android TV system image", InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice())
        selected = tab
        rule.setContent {
            STBPlayTheme {
                val previewRequester = remember { FocusRequester() }
                Box(Modifier.fillMaxSize()) {
                    StbPlayApp(
                        homeState = StbPlayHomeState(heroes = movies.take(1), rows = listOf(UiMediaRow("recommended", "Recommended", items = movies))),
                        liveState = StbPlayLibraryState(loading = loading, categories = listOf(UiCategory("all", "All")), items = if (loading) emptyList() else channels),
                        contentState = StbPlayLibraryState(categories = listOf(UiCategory("all", "All")), items = movies),
                        favouritesState = StbPlayLibraryState(items = favorites), settingsState = StbPlaySettingsState(),
                        onActivateLicense = {}, selectedTab = selected,
                        liveChannelListState = rememberLazyListState(), focusedLiveChannelId = null,
                        focusedContentId = focusedMovie, contentGridState = rememberLazyGridState(),
                        onTabSelected = { selected = it }, onLoadMoreContent = {}, onCategorySelected = { _, _ -> },
                        onMediaClick = { played++; if (selected == StbPlayTab.LIVE) previewChannel = it }, onToggleFavorite = {}, onRemoveHistory = {},
                        onRefresh = {}, onClearCache = {}, onClearHistory = {}, onAddPortal = {},
                        onEditPortal = {}, onUsePortal = {}, onDeletePortal = {}, onProviderPair = {},
                        onPlayerPreferenceChanged = {}, onAndroidBoxVideoCompatibilityChanged = {},
                        onThemePreferenceChanged = {}, onSubtitlePreferenceChanged = {}, onCatalogueLanguageChanged = {},
                        onAnalyticsChanged = {}, onChangePin = {}, onParentalModeChanged = {},
                        onCheckUpdates = {}, onDownloadUpdate = {}, onShare = {}, onSearchVisibilityChanged = {},
                        searchCatalog = emptyList(), searchRemote = { _, _ -> VodCatalogBatch(emptyList(), 1, 0, false) },
                        onSearchResults = {}, searchMedia = { UiMedia(it.id, it.name) },
                        onMediaFocused = { tab, media -> if (tab == StbPlayTab.CONTENT) focusedMovie = media.id },
                        playingLiveChannelId = previewChannel?.id,
                        livePreviewActionsRequester = previewRequester,
                        livePreview = { modifier, returnToChannels ->
                            TvLivePreviewPanel(previewChannel, previewRequester, modifier,
                                onVideoBounds = {}, onFullscreen = {}, onFavorite = {}, onVlc = {}, onBack = returnToChannels)
                        }
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    private fun focused(tag: String) {
        rule.waitUntil(8_000) { rule.onAllNodes(hasTestTag(tag) and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag(tag).assertIsFocused().assertIsDisplayed()
    }
    private fun rail(tab: StbPlayTab) {
        rule.onNodeWithTag("tv-rail:${tab.name}").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        focused("tv-rail:${tab.name}")
    }
    private fun right() { device.pressDPadRight(); rule.waitForIdle() }
    private fun left() { device.pressDPadLeft(); rule.waitForIdle() }
    private fun down() { device.pressDPadDown(); rule.waitForIdle() }
    private fun ok() { device.pressDPadCenter(); rule.waitForIdle() }

    @Test fun liveRightWorksWithoutOkAndLeftReturnsToLiveRail() {
        start()
        rail(StbPlayTab.LIVE); right()
        focused("tv-focus:category:all")
        right(); focused("tv-focus:live:ch1")
        repeat(24) { down() }
        focused("tv-focus:live:ch25")
        assertEquals("Browsing must not activate playback", 0, played)
        left(); focused("tv-focus:category:all")
        left(); focused("tv-rail:LIVE")
        right(); focused("tv-focus:live:ch25")
    }

    @Test fun everyPageReceivesVisibleFocusAndReturnsToItsOwnRail() {
        start()
        focused("tv-focus:home:hero")
        left(); focused("tv-rail:HOME")
        rail(StbPlayTab.CONTENT); ok(); focused("tv-focus:category:all")
        right(); focused("tv-focus:media:film1")
        left(); focused("tv-focus:category:all")
        left(); focused("tv-rail:CONTENT")
        rail(StbPlayTab.FAVOURITES); ok(); focused("tv-focus:media:ch1")
        left(); focused("tv-rail:FAVOURITES")
        rail(StbPlayTab.SETTINGS); ok(); focused("tv-focus:settings:Subscription")
        left(); focused("tv-rail:SETTINGS")
    }

    @Test fun settingsSubpageAndBackRestoreActualMenuFocus() {
        start(StbPlayTab.SETTINGS)
        focused("tv-focus:settings:Subscription")
        down(); focused("tv-focus:settings:Content sources")
        ok(); focused("tv-focus:settings:back")
        device.pressBack(); rule.waitForIdle()
        focused("tv-focus:settings:Content sources")
        left(); focused("tv-rail:SETTINGS")
        right(); focused("tv-focus:settings:Content sources")
    }

    @Test fun homeShelfAndMovieFocusSurviveTabSwitches() {
        start()
        focused("tv-focus:home:hero")
        down(); focused("tv-focus:home:recommended:film1")
        right(); focused("tv-focus:home:recommended:film2")
        rail(StbPlayTab.LIVE); ok(); focused("tv-focus:category:all")
        rail(StbPlayTab.HOME); ok(); focused("tv-focus:home:recommended:film2")
        rail(StbPlayTab.CONTENT); ok(); focused("tv-focus:category:all")
        right(); focused("tv-focus:media:film1")
        right(); focused("tv-focus:media:film2")
        rail(StbPlayTab.SETTINGS); ok(); focused("tv-focus:settings:Subscription")
        rail(StbPlayTab.CONTENT); ok(); focused("tv-focus:media:film2")
    }

    @Test fun previewOptionsReturnToSameChannelWithoutChangingSelectedTab() {
        start(StbPlayTab.LIVE)
        focused("tv-focus:category:all")
        right(); focused("tv-focus:live:ch1")
        repeat(2) { down() }
        focused("tv-focus:live:ch3")
        ok(); right()
        rule.onNodeWithText("Full screen").assertIsFocused()
        down(); rule.onNodeWithText("Add to favourites").assertIsFocused()
        down(); rule.onNodeWithText("Not working? Play in VLC").assertIsFocused()
        left(); focused("tv-focus:live:ch3")
        left(); focused("tv-focus:category:all")
        left(); focused("tv-rail:LIVE")
        assertEquals(StbPlayTab.LIVE, selected)
    }

    @Test fun loadingAndEmptyFavoritesNeverLeaveInvisibleFocus() {
        loading = true
        start(StbPlayTab.LIVE)
        focused("tv-focus:category:all")
        rule.runOnIdle { loading = false }
        right(); focused("tv-focus:live:ch1")
        rule.runOnIdle { favorites = emptyList() }
        rail(StbPlayTab.FAVOURITES); ok(); focused("tv-rail:FAVOURITES")
        rail(StbPlayTab.HOME); right(); focused("tv-focus:home:hero")
    }
}
