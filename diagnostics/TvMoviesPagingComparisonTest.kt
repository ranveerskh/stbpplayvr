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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.stbplay.BuildConfig
import androidx.compose.foundation.lazy.grid.LazyGridState

class TvMoviesPagingComparisonTest {
    @get:Rule val rule = createComposeRule()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val channels = (1..45).map { UiMedia("ch$it", "Channel $it", streamType = "live") }
    private val movies = (1..350).map { UiMedia("film$it", "Movie $it", portrait = true) }
    private var visibleMovies by mutableStateOf(movies.take(12))
    private var loadingMore by mutableStateOf(false)
    private var pageRequests = 0
    private var categoryIndex by mutableIntStateOf(0)
    private var grid = LazyGridState()
    private var selected by mutableStateOf(StbPlayTab.HOME)
    private var loading by mutableStateOf(false)
    private var focusedMovie by mutableStateOf<String?>(null)
    private var favorites by mutableStateOf(channels.take(4) + movies.take(4))
    private var played = 0
    private var previewChannel by mutableStateOf<UiMedia?>(null)

    private fun start(tab: StbPlayTab = StbPlayTab.CONTENT) {

        selected = tab
        rule.setContent {
            STBPlayTheme {
                val previewRequester = remember { FocusRequester() }
                val scope = rememberCoroutineScope()
                Box(Modifier.fillMaxSize().then(if (BuildConfig.VERSION_NAME == "2.0.19") Modifier.focusProperties { canFocus = true } else Modifier)) {
                    StbPlayApp(
                        homeState = StbPlayHomeState(heroes = movies.take(1), rows = listOf(UiMediaRow("recommended", "Recommended", items = movies))),
                        liveState = StbPlayLibraryState(loading = loading, categories = listOf(UiCategory("all", "All")), items = if (loading) emptyList() else channels),
                        contentState = StbPlayLibraryState(categories = listOf(UiCategory("all", "All"), UiCategory("drama", "Drama")), selectedCategory = categoryIndex, items = visibleMovies, hasMore = visibleMovies.size < movies.size, loadingMore = loadingMore),
                        favouritesState = StbPlayLibraryState(items = favorites), settingsState = StbPlaySettingsState(),
                        onActivateLicense = {}, selectedTab = selected,
                        liveChannelListState = rememberLazyListState(), focusedLiveChannelId = null,
                        focusedContentId = focusedMovie, contentGridState = grid,
                        onTabSelected = { selected = it }, onLoadMoreContent = {
                            pageRequests++
                            loadingMore = true
                            scope.launch {
                                delay(200)
                                visibleMovies = movies.take((visibleMovies.size + 24).coerceAtMost(movies.size))
                                loadingMore = false
                            }
                        }, onCategorySelected = { _, index -> categoryIndex = index },
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


    private fun sample(label: String) {
        rule.waitForIdle()
        val state = rule.runOnIdle { "$label requests=$pageRequests loaded=${visibleMovies.size} focused=$focusedMovie first=${grid.firstVisibleItemIndex} last=${grid.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index }}" }
        android.util.Log.i("MOVIES_COMPARISON", "version=${BuildConfig.VERSION_NAME} tv=${InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice()} $state")
        println("MOVIES_COMPARISON version=${BuildConfig.VERSION_NAME} $state")
    }
    private fun settlePages() {
        android.os.SystemClock.sleep(1500)
        rule.waitForIdle()
    }
    @Test fun growingCatalogueOnEntryAndRemoteNavigation() {
        start()
        settlePages()
        sample("entry")
        device.pressDPadRight(); rule.waitForIdle()
        sample("right")
        repeat(12) { device.pressDPadDown(); rule.waitForIdle() }
        settlePages()
        sample("after-down")
        repeat(8) { device.pressDPadUp(); rule.waitForIdle() }
        sample("after-up")
        assertTrue("Loader must not request beyond the fixture catalogue", pageRequests <= 20)
    }
    @Test fun catalogueGrowthWhileNoRemoteNavigationOccurs() {
        start()
        settlePages()
        sample("before-growth")
        repeat(10) {
            rule.runOnIdle { visibleMovies = movies.take((visibleMovies.size + 8).coerceAtMost(movies.size)) }
            rule.waitForIdle()
        }
        settlePages()
        sample("background-growth")
    }
}
