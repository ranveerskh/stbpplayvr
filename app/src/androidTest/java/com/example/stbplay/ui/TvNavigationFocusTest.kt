package com.example.stbplay.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import com.example.stbplay.ui.screens.TvLivePreviewPanel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.stbplay.data.VodCatalogBatch
import com.example.stbplay.isAndroidTvDevice
import com.example.stbplay.ui.theme.STBPlayTheme
import com.example.stbplay.data.ThemePreference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TvNavigationFocusTest {
    @get:Rule val rule = createComposeRule()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val channels = (1..45).map { UiMedia("ch$it", "Channel $it", streamType = "live") }
    private val movies = (1..35).map { UiMedia("film$it", "Movie $it", portrait = true) }
    private var theme by mutableStateOf(ThemePreference.BLUE)
    private var selected by mutableStateOf(StbPlayTab.HOME)
    private var loading by mutableStateOf(false)
    private var liveCategories by mutableStateOf(listOf(UiCategory("all", "All")))
    private var liveFocusReset by mutableStateOf(0)
    private var focusFirstChannelAfterLoad by mutableStateOf(false)
    private var heroItems by mutableStateOf(movies.take(1))
    private var focusedMovie by mutableStateOf<String?>(null)
    private var favorites by mutableStateOf(channels.take(4) + movies.take(4))
    private var clearPageFocus: () -> Unit = {}
    private var played = 0
    private var previewChannel by mutableStateOf<UiMedia?>(null)

    private fun start(tab: StbPlayTab = StbPlayTab.HOME, portalSwitcher: Boolean = false) {
        assertTrue("Must run on a real Android TV system image", InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice())
        selected = tab
        rule.setContent {
            STBPlayTheme(preference = theme) {
                val manager = androidx.compose.ui.platform.LocalFocusManager.current
                SideEffect { clearPageFocus = { manager.clearFocus(force = true) } }
                val previewRequester = remember { FocusRequester() }
                val render: @Composable () -> Unit = {
                Box(Modifier.fillMaxSize()) {
                    StbPlayApp(
                        homeState = StbPlayHomeState(heroes = heroItems, rows = listOf(UiMediaRow("recommended", "Recommended", items = movies))),
                        liveState = StbPlayLibraryState(loading = loading, categories = liveCategories, focusResetGeneration = liveFocusReset, focusFirstChannelAfterLoad = focusFirstChannelAfterLoad, items = if (loading) emptyList() else channels),
                        contentState = StbPlayLibraryState(categories = listOf(UiCategory("all", "All")), items = movies),
                        favouritesState = StbPlayLibraryState(items = favorites), settingsState = StbPlaySettingsState(themePreference = theme),
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
                if (portalSwitcher) ExperimentalPortalHost { render() } else render()
            }
        }
        rule.waitForIdle()
    }

    @Test fun portalSelectorKeepsIntentionalFocusAndSwitchesWithRemote() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("free_drama_experiment", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("iptv_selected", true).commit()
        start(StbPlayTab.LIVE, portalSwitcher = true)
        focused("tv-focus:category:all")
        rule.onNodeWithTag("portal_default").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithTag("portal_default").assertIsFocused()
        ok()
        rule.onNodeWithTag("drama_zanjeerain").assertIsDisplayed()
    }

    @Test fun expandedRailShowsFullLabelsWithoutMovingHomeContent() {
        theme = ThemePreference.GLASS
        start()
        val before = rule.onNodeWithText("Recommended").fetchSemanticsNode().boundsInRoot.left
        rail(StbPlayTab.LIVE)
        rule.onNodeWithText("Movies & Series").assertIsDisplayed()
        rule.onNodeWithText("Favourites").assertIsDisplayed()
        val after = rule.onNodeWithText("Recommended").fetchSemanticsNode().boundsInRoot.left
        assertEquals(before, after, 0.1f)
        rail(StbPlayTab.HOME); right()
        focused("tv-focus:home:hero")
        rule.onNodeWithText("Movies & Series").assertDoesNotExist()
    }

    @Test fun adultCategoryLoadingMovesFocusToFirstChannelAndResetReturnsToTop() {
        theme = ThemePreference.GLASS
        liveCategories = listOf(UiCategory("adult", "Adult", isAdult = true))
        loading = true
        focusFirstChannelAfterLoad = true
        start(StbPlayTab.LIVE)
        rule.onNodeWithText("Loading channels…").assertIsDisplayed()
        rule.runOnIdle { loading = false }
        focused("tv-focus:live:ch1")
        rail(StbPlayTab.HOME); ok()
        rule.runOnIdle { liveCategories = listOf(UiCategory("all", "All")); liveFocusReset++; focusFirstChannelAfterLoad = false }
        rail(StbPlayTab.LIVE); ok()
        focused("tv-focus:category:all")
        rule.onNodeWithText("Adult").assertDoesNotExist()
    }

    private fun focused(tag: String) {
        try {
            rule.waitUntil(8_000) { rule.onAllNodes(hasTestTag(tag) and isFocused()).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            throw AssertionError("Expected focus on $tag\n" + rule.onRoot().printToString(), failure)
        }
        rule.onNodeWithTag(tag).assertIsFocused().assertIsDisplayed()
        if (tag != "tv-focus:settings:back") {
            fun hasYellowHighlight(): Boolean {
                val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
                return (0 until minOf(8, pixels.height)).any { y ->
                    val color = pixels[pixels.width / 2, y]
                    kotlin.math.abs(color.red - 1f) < 0.08f &&
                        kotlin.math.abs(color.green - 209f / 255f) < 0.08f &&
                        kotlin.math.abs(color.blue - 102f / 255f) < 0.08f
                }
            }
            rule.waitForIdle()
            rule.waitUntil(3_000) { hasYellowHighlight() }
            assertTrue("Focused control must also show its yellow highlight: $tag", hasYellowHighlight())
        }
    }
    private fun rail(tab: StbPlayTab) {
        rule.onNodeWithTag("tv-rail:${tab.name}").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        focused("tv-rail:${tab.name}")
    }
    private fun right() { device.pressDPadRight(); rule.waitForIdle() }
    private fun left() { device.pressDPadLeft(); rule.waitForIdle() }
    private fun down() { device.pressDPadDown(); rule.waitForIdle() }
    private fun ok() { device.pressDPadCenter(); rule.waitForIdle() }

    @Test fun modernThemesKeepFocusVisibleAcrossHomeLiveAndMovies() {
        start()
        ThemePreference.entries.forEachIndexed { index, preference ->
            rule.runOnIdle { theme = preference }
            rule.waitForIdle()
            rail(StbPlayTab.HOME); right(); focused("tv-focus:home:hero")
            saveThemePreview(rule, "tv-home-${preference.name.lowercase()}")
            rail(StbPlayTab.LIVE); right()
            // First entry starts at categories; subsequent entries restore the channel.
            if (index == 0) { focused("tv-focus:category:all"); right() }
            focused("tv-focus:live:ch1")
            saveThemePreview(rule, "tv-live-${preference.name.lowercase()}")
            rail(StbPlayTab.CONTENT); ok()
            // The remembered movie is the entry target after the first theme.
            if (index == 0) { focused("tv-focus:category:all"); right() }
            focused("tv-focus:media:movie:film1")
            saveThemePreview(rule, "tv-movies-${preference.name.lowercase()}")
        }
    }

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
        right(); focused("tv-focus:media:movie:film1")
        left(); focused("tv-focus:category:all")
        left(); focused("tv-rail:CONTENT")
        rail(StbPlayTab.FAVOURITES); ok(); focused("tv-focus:media:live:ch1")
        left(); focused("tv-rail:FAVOURITES")
        rail(StbPlayTab.SETTINGS); ok(); focused("tv-focus:settings:Subscription")
        left(); focused("tv-rail:SETTINGS")
    }

    @Test fun settingsSubpageAndBackRestoreActualMenuFocus() {
        start(StbPlayTab.SETTINGS)
        focused("tv-focus:settings:Subscription")
        down(); focused("tv-focus:settings:Who's watching?")
        down(); focused("tv-focus:settings:Content sources")
        ok(); focused("tv-focus:settings:back")
        device.pressBack(); rule.waitForIdle()
        focused("tv-focus:settings:Content sources")
        left(); focused("tv-rail:SETTINGS")
        right(); focused("tv-focus:settings:Content sources")
    }

    @Test fun livePageWaitsForLateCategoriesWithoutLosingItsEntryRequest() {
        loading = true
        liveCategories = emptyList()
        start()
        rail(StbPlayTab.LIVE); ok()
        rule.mainClock.advanceTimeBy(3_000)
        rule.runOnIdle { liveCategories = listOf(UiCategory("all", "All")); loading = false }
        focused("tv-focus:category:all")
        rule.onNodeWithTag("tv-navigation-expanded").assertDoesNotExist()
    }

    @Test fun settingsTransitionNeverExpandsNavigationRail() {
        start(StbPlayTab.SETTINGS)
        focused("tv-focus:settings:Subscription")
        down(); down(); focused("tv-focus:settings:Content sources")
        rule.mainClock.autoAdvance = false
        try {
            rule.onNodeWithTag("tv-focus:settings:Content sources").performClick()
            repeat(12) {
                rule.mainClock.advanceTimeByFrame()
                rule.onNodeWithTag("tv-navigation-expanded").assertDoesNotExist()
            }
            rule.onNodeWithTag("tv-focus:settings:back").assertIsFocused()
            rule.onNodeWithTag("tv-focus:settings:back").performClick()
            repeat(12) {
                rule.mainClock.advanceTimeByFrame()
                rule.onNodeWithTag("tv-navigation-expanded").assertDoesNotExist()
            }
        } finally { rule.mainClock.autoAdvance = true }
        focused("tv-focus:settings:Content sources")
    }

    @Test fun tvHeroRotatesWithAndWithoutFocusAndRetainsTitleAcrossPoolReorder() {
        heroItems = movies.take(3)
        start()
        focused("tv-focus:home:hero")
        rule.mainClock.advanceTimeBy(13_000)
        rule.onNode(hasText("Movie 2") and hasAnyAncestor(hasTestTag("tv-focus:home:hero")), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("tv-focus:home:hero").assertIsFocused()
        rail(StbPlayTab.HOME)
        rule.mainClock.advanceTimeBy(13_000)
        rule.onNode(hasText("Movie 3") and hasAnyAncestor(hasTestTag("tv-focus:home:hero")), useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("tv-focus:home:hero").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.runOnIdle { heroItems = listOf(movies[2], movies[0], movies[1]) }
        rule.waitForIdle()
        rule.onNode(hasText("Movie 3") and hasAnyAncestor(hasTestTag("tv-focus:home:hero")), useUnmergedTree = true).assertIsDisplayed()
        rule.mainClock.advanceTimeBy(13_000)
        rule.onNode(hasText("Movie 1") and hasAnyAncestor(hasTestTag("tv-focus:home:hero")), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun actualFocusLossRestoresLastLiveChannelAndSettingsControlWithoutStealingRail() {
        start(StbPlayTab.LIVE)
        focused("tv-focus:category:all"); right()
        repeat(4) { down() }
        focused("tv-focus:live:ch5")
        repeat(3) { rule.runOnIdle { clearPageFocus() }; focused("tv-focus:live:ch5") }
        left(); focused("tv-focus:category:all")
        left(); focused("tv-rail:LIVE")
        rule.mainClock.advanceTimeBy(1_000); focused("tv-rail:LIVE")
        rail(StbPlayTab.SETTINGS); ok(); focused("tv-focus:settings:Subscription")
        down(); down(); focused("tv-focus:settings:Content sources")
        rule.runOnIdle { clearPageFocus() }; focused("tv-focus:settings:Content sources")
        ok(); focused("tv-focus:settings:back")
        rule.runOnIdle { clearPageFocus() }; focused("tv-focus:settings:back")
        device.pressBack(); rule.waitForIdle(); focused("tv-focus:settings:Content sources")
    }

    @Test fun homeShelfAndMovieFocusSurviveTabSwitches() {
        start()
        focused("tv-focus:home:hero")
        down(); focused("tv-focus:home:recommended:movie:film1")
        right(); focused("tv-focus:home:recommended:movie:film2")
        rail(StbPlayTab.LIVE); ok(); focused("tv-focus:category:all")
        rail(StbPlayTab.HOME); ok(); focused("tv-focus:home:recommended:movie:film2")
        rail(StbPlayTab.CONTENT); ok(); focused("tv-focus:category:all")
        right(); focused("tv-focus:media:movie:film1")
        right(); focused("tv-focus:media:movie:film2")
        rail(StbPlayTab.SETTINGS); ok(); focused("tv-focus:settings:Subscription")
        rail(StbPlayTab.CONTENT); ok(); focused("tv-focus:media:movie:film2")
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

    @Test fun channelAndMovieWithSamePortalIdKeepSeparateFocusTargets() {
        favorites = listOf(UiMedia("same", "Saved channel", streamType = "live"), UiMedia("same", "Saved movie", portrait = true))
        start(StbPlayTab.FAVOURITES)
        focused("tv-focus:media:live:same")
        down(); focused("tv-focus:media:movie:same")
        rail(StbPlayTab.SETTINGS); ok(); focused("tv-focus:settings:Subscription")
        rail(StbPlayTab.FAVOURITES); ok(); focused("tv-focus:media:movie:same")
    }

    @Test fun loadingAndEmptyFavoritesNeverLeaveInvisibleFocus() {
        loading = true
        start(StbPlayTab.LIVE)
        focused("tv-focus:category:all")
        rule.runOnIdle { loading = false }
        rule.waitForIdle()
        rule.onNodeWithTag("tv-focus:live:ch1").assertIsDisplayed()
        right(); focused("tv-focus:live:ch1")
        rule.runOnIdle { favorites = emptyList() }
        rule.waitForIdle()
        rail(StbPlayTab.FAVOURITES); ok(); focused("tv-rail:FAVOURITES")
        rail(StbPlayTab.HOME); right(); focused("tv-focus:home:hero")
    }
}
