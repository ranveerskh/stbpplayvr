package com.example.stbplay.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import com.example.stbplay.isAndroidTvDevice
import com.example.stbplay.data.CategoryDropdownPosition
import com.example.stbplay.data.VodCatalogBatch
import com.example.stbplay.data.model.PortalStream

@Composable
internal fun Batch2AppFixture(
    selected: StbPlayTab,
    onTab: (StbPlayTab) -> Unit = {},
    settings: StbPlaySettingsState = StbPlaySettingsState(),
    session: SearchSession,
    channels: List<UiMedia> = listOf(UiMedia("ch1", "Channel 1", streamType = "live")),
    movies: List<UiMedia> = (1..24).map { UiMedia("film$it", "Film $it", portrait = true) },
    onPlay: (UiMedia) -> Unit = {},
    onSwitchViewer: () -> Unit = {},
    onCategoryPosition: (CategoryDropdownPosition) -> Unit = {},
    onColumns: (Int) -> Unit = {},
    history: List<String> = emptyList(),
    rememberSearch: (String) -> Unit = {},
    clearHistory: () -> Unit = {},
    remote: suspend (String, Int) -> VodCatalogBatch = { _, _ -> VodCatalogBatch(emptyList(), 1, 0, false) }
) {
    StbPlayApp(
        homeState = StbPlayHomeState(heroes = movies.take(1)),
        liveState = StbPlayLibraryState(categories = listOf(UiCategory("all", "All"), UiCategory("news", "News")), items = channels),
        contentState = StbPlayLibraryState(categories = listOf(UiCategory("all", "All"), UiCategory("films", "Films")), items = movies),
        favouritesState = StbPlayLibraryState(), settingsState = settings,
        onActivateLicense = {}, selectedTab = selected, liveChannelListState = rememberLazyListState(),
        focusedLiveChannelId = null, focusedContentId = null, contentGridState = rememberLazyGridState(),
        onTabSelected = onTab, onLoadMoreContent = {}, onCategorySelected = { _, _ -> },
        onMediaClick = onPlay, onToggleFavorite = {}, onRemoveHistory = {}, onRefresh = {},
        onClearCache = {}, onClearHistory = {}, onAddPortal = {}, onEditPortal = {}, onUsePortal = {},
        onDeletePortal = {}, onProviderPair = {}, onPlayerPreferenceChanged = {}, onAndroidBoxVideoCompatibilityChanged = {},
        onThemePreferenceChanged = {}, onSubtitlePreferenceChanged = {}, onCatalogueLanguageChanged = {},
        onAnalyticsChanged = {}, onChangePin = {}, onParentalModeChanged = {}, onCheckUpdates = {}, onDownloadUpdate = {},
        onShare = {}, onSearchVisibilityChanged = {}, onSwitchViewer = onSwitchViewer,
        searchCatalog = channels.map { PortalStream(it.id, it.title, null, null, "live") },
        searchRemote = remote, onSearchResults = {},
        searchMedia = { UiMedia(it.id, it.name, streamType = it.streamType, portrait = it.streamType != "live") },
        onPhoneCategoryPositionChanged = onCategoryPosition, onPhoneMovieColumnsChanged = onColumns,
        searchSession = session, searchHistory = history, onRememberSearch = rememberSearch, onClearSearchHistory = clearHistory
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Suppress("DEPRECATION")
internal fun prewarmBatch2PhoneCast(rule: androidx.compose.ui.test.junit4.ComposeContentTestRule) {
    val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    if (!context.isAndroidTvDevice()) rule.runOnIdle {
        com.google.android.gms.cast.framework.CastContext.getSharedInstance(context)
        androidx.media3.cast.Cast.getSingletonInstance(context).initialize()
    }
}
