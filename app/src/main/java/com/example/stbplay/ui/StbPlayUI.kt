@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.stbplay.ui

import androidx.activity.compose.BackHandler
import com.example.stbplay.data.VodCatalogBatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.lazy.itemsIndexed as indexedColumnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.itemsIndexed as indexedGridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.stbplay.ui.RemoteTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.media3.cast.MediaRouteButton
import androidx.media3.common.util.UnstableApi
import com.example.stbplay.data.PlayerPreference
import com.example.stbplay.data.ProviderPairingSession
import com.example.stbplay.data.ParentalMode
import com.example.stbplay.data.SubtitlePreference
import com.example.stbplay.data.ThemePreference
import com.example.stbplay.R
import com.example.stbplay.isAndroidTvDevice
import com.example.stbplay.ui.theme.LocalStbPalette
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.domain.model.PortalSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import java.text.Normalizer
import java.util.PriorityQueue
import java.util.Locale

private val Navy: Color @Composable get() = LocalStbPalette.current.background
private val Rail: Color @Composable get() = LocalStbPalette.current.rail
private val Panel: Color @Composable get() = LocalStbPalette.current.panel
private val PanelSoft: Color @Composable get() = LocalStbPalette.current.panelSoft
private val Gold: Color @Composable get() = LocalStbPalette.current.accent
private val GoldLight: Color @Composable get() = LocalStbPalette.current.accentLight
private val White: Color @Composable get() = LocalStbPalette.current.text
private val Muted: Color @Composable get() = LocalStbPalette.current.muted
private val OnAccent: Color @Composable get() = LocalStbPalette.current.onAccent
private val Danger: Color @Composable get() = LocalStbPalette.current.danger
private val Good: Color @Composable get() = LocalStbPalette.current.good
private val PosterWhite = Color.White

@Composable
private fun isCompactAndroidLayout(): Boolean {
    val context = LocalContext.current
    val isTelevision = remember(context) {
        context.isAndroidTvDevice()
    }
    return LocalConfiguration.current.screenWidthDp < 900 && !isTelevision
}

@Composable
private fun isTelevisionLayout(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.isAndroidTvDevice()
    }
}

private data class SettingsFocusTarget(val title: String, val onFocused: (String) -> Unit)
private val LocalSettingsFocusTarget = androidx.compose.runtime.staticCompositionLocalOf<SettingsFocusTarget?> { null }

private val LocalTvFocusEntry = androidx.compose.runtime.staticCompositionLocalOf { "" }

/** Request once per mounted screen, without stealing focus when a lazy row returns. */
@Composable
fun Modifier.questInitialFocus(): Modifier {
    val television = isTelevisionLayout()
    if (television && LocalTvFocusNavigation.current != null) return this
    val requester = remember { FocusRequester() }
    // Lazy items leave composition when scrolled away. Keep this flag in their
    // saved item state so returning to the viewport cannot steal remote focus.
    val fallbackEntry = remember { java.util.UUID.randomUUID().toString() }
    val entry = LocalTvFocusEntry.current.ifBlank { fallbackEntry }
    var requestedEntry by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(requester, entry) {
        if (!television || requestedEntry != entry) {
            if (television) {
                withFrameNanos { }
                withFrameNanos { }
            }
            requester.requestFocus()
            requestedEntry = entry
        }
    }
    return then(focusRequester(requester))
}

enum class StbPlayTab { HOME, LIVE, CONTENT, FAVOURITES, SETTINGS }

data class UiCategory(
    val id: String,
    val title: String,
    val isLocked: Boolean = false,
    val isAdult: Boolean = isLocked || title.isAdultLabel()
)

private fun String.isAdultLabel(): Boolean {
    val value = lowercase()
    return listOf("18+", "adult", "xxx", "porn", "erotic", "a-rated", "uncensored").any(value::contains)
}

data class UiMedia(
    val id: String,
    val title: String,
    val imageUrl: String? = null,
    val imageHeaders: Map<String, String> = emptyMap(),
    val subtitle: String? = null,
    val description: String? = null,
    val badge: String? = null,
    val progress: Float = 0f,
    val portrait: Boolean = false,
    val streamType: String = "movie",
    val year: Int? = null,
    val searchText: String? = null,
    val language: String? = null,
    val genre: String? = null,
    val rating: String? = null,
    val cast: String? = null,
    val isLocked: Boolean = false,
    val isFavorite: Boolean = false
)

data class UiMediaRow(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val items: List<UiMedia>
)

data class StbPlayHomeState(
    val loading: Boolean = false,
    val heroes: List<UiMedia> = emptyList(),
    val rows: List<UiMediaRow> = emptyList(),
    val expiryText: String? = null,
    val portalWarning: String? = null
)

data class StbPlayLibraryState(
    val loading: Boolean = false,
    val categories: List<UiCategory> = emptyList(),
    val selectedCategory: Int = 0,
    val items: List<UiMedia> = emptyList(),
    val totalItemsText: String = "",
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val emptyMessage: String = "Try another category or refresh the portal."
)

data class StbPlaySettingsState(
    val profiles: List<PortalSettings> = emptyList(),
    val activeProfileId: String = "",
    val subscriptionPlan: String = "Subscription",
    val subscriptionStatus: String = "",
    val expiryText: String = "",
    val licenseName: String = "Demo trial",
    val licenseExpiryText: String = "",
    val liveCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val playerPreference: PlayerPreference = PlayerPreference.AUTO,
    val androidBoxVideoCompatibility: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.BLUE,
    val parentalMode: ParentalMode = ParentalMode.ALL_CONTENT,
    val subtitlePreference: SubtitlePreference = SubtitlePreference.AUTO,
    val catalogueLanguage: String = "All",
    val analyticsEnabled: Boolean = false,
    val lastRefreshText: String = "Not refreshed yet",
    val updateText: String = "Check whether a newer STB Play version is available.",
    val updateAvailableVersion: String? = null,
    val licenseStatus: String = "No key activated",
    val licenseBusy: Boolean = false,
    val appAccessStatus: String = "",
    val deviceReference: String = "",
    val portalMac: String = "",
    val portalSubscriptionMessage: String = ""
)

@Composable
fun StbPlayApp(
    homeState: StbPlayHomeState,
    liveState: StbPlayLibraryState,
    contentState: StbPlayLibraryState,
    favouritesState: StbPlayLibraryState,
    settingsState: StbPlaySettingsState,
    onActivateLicense: (String) -> Unit,
    selectedTab: StbPlayTab,
    liveChannelListState: LazyListState,
    focusedLiveChannelId: String?,
    focusedContentId: String?,
    contentGridState: LazyGridState,
    onTabSelected: (StbPlayTab) -> Unit,
    onLoadMoreContent: () -> Unit,
    onCategorySelected: (StbPlayTab, Int) -> Unit,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onRemoveHistory: (UiMedia) -> Unit,
    onRefresh: () -> Unit,
    onClearCache: () -> Unit,
    onClearHistory: () -> Unit,
    onAddPortal: () -> Unit,
    onEditPortal: (PortalSettings) -> Unit,
    onUsePortal: (PortalSettings) -> Unit,
    onDeletePortal: (PortalSettings) -> Unit,
    onProviderPair: (PortalSettings) -> Unit,
    onPlayerPreferenceChanged: (PlayerPreference) -> Unit,
    onAndroidBoxVideoCompatibilityChanged: (Boolean) -> Unit,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    onSubtitlePreferenceChanged: (SubtitlePreference) -> Unit,
    onCatalogueLanguageChanged: (String) -> Unit,
    onAnalyticsChanged: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onParentalModeChanged: (ParentalMode) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onShare: () -> Unit,
    onSearchVisibilityChanged: (Boolean) -> Unit,
    searchCatalog: List<PortalStream>,
    searchRemote: suspend (String, Int) -> VodCatalogBatch,
    onSearchResults: (List<PortalStream>) -> Unit,
    searchMedia: (PortalStream) -> UiMedia,
    onHomeInteraction: () -> Unit = {},
    onMediaFocused: (StbPlayTab, UiMedia) -> Unit = { _, _ -> },
    playingLiveChannelId: String? = null,
    livePreviewActionsRequester: FocusRequester? = null,
    livePreview: (@Composable (Modifier, () -> Unit) -> Unit)? = null
) {
    var searchOpen by remember { mutableStateOf(false) }
    val tvLayout = isTelevisionLayout()
    val inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
    val railRequesters = remember { StbPlayTab.entries.associateWith { FocusRequester() } }
    val enterPageAction = remember { arrayOf<() -> Unit>({}) }
    fun returnToSelectedRail() { railRequesters.getValue(selectedTab).requestFocus() }
    LaunchedEffect(tvLayout) {
        if (tvLayout) inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
    }
    var railCollapsed by remember(tvLayout) { mutableStateOf(tvLayout) }
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val compactLayout = configuration.screenWidthDp < 900 &&
        !context.isAndroidTvDevice()

    BackHandler(enabled = !searchOpen && selectedTab != StbPlayTab.HOME) {
        onTabSelected(StbPlayTab.HOME)
    }

    if (searchOpen) {
        Box(Modifier.fillMaxSize().background(Navy).windowInsetsPadding(WindowInsets.safeDrawing)) {
            StbPlaySearchScreen(
                catalog = searchCatalog,
                searchRemote = searchRemote,
                onSearchResults = onSearchResults,
                toUi = searchMedia,
                scope = selectedTab,
                hasMore = selectedTab == StbPlayTab.CONTENT && contentState.hasMore,
                loadingMore = contentState.loadingMore,
                onLoadMore = onLoadMoreContent,
                onMediaClick = { media -> searchOpen = false; onSearchVisibilityChanged(false); onMediaClick(media) },
                onToggleFavorite = onToggleFavorite,
                onBack = { searchOpen = false; onSearchVisibilityChanged(false) }
            )
        }
        return
    }

    val tabStateHolder = rememberSaveableStateHolder()
    val renderTab: @Composable () -> Unit = {
            when (selectedTab) {
                StbPlayTab.HOME -> StbPlayHomeScreen(
                    state = homeState,
                    onMediaClick = onMediaClick,
                    onToggleFavorite = onToggleFavorite,
                    onRemoveHistory = onRemoveHistory,
                    onInteraction = onHomeInteraction,
                    onRefresh = onRefresh,
                    onEditPortal = {
                        settingsState.profiles.firstOrNull { it.id == settingsState.activeProfileId }
                            ?.let(onEditPortal)
                    }
                )
                StbPlayTab.LIVE -> LiveTvScreen(
                    state = liveState,
                    channelListState = liveChannelListState,
                    focusedChannelId = focusedLiveChannelId,
                    onCategorySelected = { onCategorySelected(StbPlayTab.LIVE, it) },
                    onMediaClick = onMediaClick,
                    onToggleFavorite = onToggleFavorite,
                    onMediaFocused = { onMediaFocused(StbPlayTab.LIVE, it) },
                    playingChannelId = playingLiveChannelId,
                    previewActionsRequester = livePreviewActionsRequester,
                    preview = livePreview
                )
                StbPlayTab.CONTENT -> ContentBrowserScreen(
                    state = contentState,
                    gridState = contentGridState,
                    focusedContentId = focusedContentId,
                    onLoadMore = onLoadMoreContent,
                    onCategorySelected = { onCategorySelected(StbPlayTab.CONTENT, it) },
                    onMediaClick = onMediaClick,
                    onToggleFavorite = onToggleFavorite,
                    onMediaFocused = { onMediaFocused(StbPlayTab.CONTENT, it) }
                )
                StbPlayTab.FAVOURITES -> FavouritesScreen(
                    state = favouritesState,
                    onMediaClick = onMediaClick,
                    onToggleFavorite = onToggleFavorite
                )
                StbPlayTab.SETTINGS -> StbPlaySettingsScreen(
                    state = settingsState,
                    onActivateLicense = onActivateLicense,
                    onRefresh = onRefresh,
                    onClearCache = onClearCache,
                    onClearHistory = onClearHistory,
                    onAddPortal = onAddPortal,
                    onEditPortal = onEditPortal,
                    onUsePortal = onUsePortal,
                    onDeletePortal = onDeletePortal,
                    onProviderPair = onProviderPair,
                    onPlayerPreferenceChanged = onPlayerPreferenceChanged,
                    onAndroidBoxVideoCompatibilityChanged = onAndroidBoxVideoCompatibilityChanged,
                    onThemePreferenceChanged = onThemePreferenceChanged,
                    onSubtitlePreferenceChanged = onSubtitlePreferenceChanged,
                    onCatalogueLanguageChanged = onCatalogueLanguageChanged,
                    onAnalyticsChanged = onAnalyticsChanged,
                    onChangePin = onChangePin,
                    onParentalModeChanged = onParentalModeChanged,
                    onCheckUpdates = onCheckUpdates,
                    onDownloadUpdate = onDownloadUpdate,
                    onShare = onShare
                )
            }
    }
    val focusEntry = remember(selectedTab) { java.util.UUID.randomUUID().toString() }
    val renderPage: @Composable () -> Unit = {
        if (tvLayout) CompositionLocalProvider(LocalTvFocusEntry provides focusEntry) {
            tabStateHolder.SaveableStateProvider(selectedTab.name) {
                var lastPageFocusId by rememberSaveable { mutableStateOf<String?>(null) }
                val navigation = remember { TvFocusNavigation(::returnToSelectedRail) { lastPageFocusId = it } }
                SideEffect { enterPageAction[0] = navigation::enterPage }
                val target = navigation.entryId?.let { navigation.targets[it] }
                LaunchedEffect(navigation.pending, navigation.requestGeneration, navigation.entryId, target) {
                    if (navigation.pending && target != null) {
                        inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
                        target.requestFocus()
                    } else if (navigation.pending && navigation.entryId == null) {
                        navigation.returnToRail()
                        navigation.pending = false
                    }
                }
                CompositionLocalProvider(LocalTvFocusNavigation provides navigation) {
                    Box(Modifier.fillMaxSize().focusProperties {
                        exit = { direction -> if (direction == androidx.compose.ui.focus.FocusDirection.Left)
                            railRequesters.getValue(selectedTab) else FocusRequester.Default }
                    }.focusGroup()) { renderTab() }
                }
            }
        }
        else renderTab()
    }
    val pageContent: @Composable () -> Unit = {
        if (tvLayout) CompositionLocalProvider(LocalBringIntoViewSpec provides TvVerticalBringIntoViewSpec) { renderPage() }
        else renderPage()
    }
    if (compactLayout) {
        Column(modifier = Modifier.fillMaxSize().background(Navy).windowInsetsPadding(WindowInsets.safeDrawing)) {
            StbPlayHeader(
                selectedTab = selectedTab,
                themePreference = settingsState.themePreference,
                onSearchClick = { searchOpen = true; onSearchVisibilityChanged(true) },
                compact = true
            )
            Box(Modifier.weight(1f).fillMaxWidth()) { pageContent() }
            PhoneBottomNavigation(selectedTab, onTabSelected)
        }
    } else {
        Row(modifier = Modifier.fillMaxSize().background(Navy).windowInsetsPadding(WindowInsets.safeDrawing)) {
            StbPlayNavigationRail(
                selectedTab = selectedTab,
                collapsed = railCollapsed,
                compactTv = tvLayout,
                onToggle = { if (selectedTab != StbPlayTab.CONTENT) railCollapsed = !railCollapsed },
                onTabSelected = onTabSelected,
                railRequesters = railRequesters,
                onEnterPage = { tab ->
                    if (tab == selectedTab) enterPageAction[0]() else onTabSelected(tab)
                }
            )
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                StbPlayHeader(
                    selectedTab = selectedTab,
                    themePreference = settingsState.themePreference,
                    onSearchClick = { searchOpen = true; onSearchVisibilityChanged(true) },
                    denseTv = tvLayout
                )
                Box(Modifier.weight(1f).fillMaxWidth()) { pageContent() }
            }
        }
    }
}

@Composable
private fun PhoneBottomNavigation(selectedTab: StbPlayTab, onTabSelected: (StbPlayTab) -> Unit) {
    val tabs = listOf(
        Triple(StbPlayTab.HOME, "Home", Icons.Filled.Home),
        Triple(StbPlayTab.LIVE, "Live", Icons.Filled.LiveTv),
        Triple(StbPlayTab.CONTENT, "Movies", Icons.Filled.Movie),
        Triple(StbPlayTab.FAVOURITES, "Saved", Icons.Filled.Favorite),
        Triple(StbPlayTab.SETTINGS, "Settings", Icons.Filled.Settings)
    )
    Row(
        Modifier.fillMaxWidth().background(Rail).padding(horizontal = 5.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (tab, label, icon) ->
            val active = selectedTab == tab
            QuestSurface(
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (active) Gold.copy(alpha = 0.18f) else Color.Transparent,
                    focusedContainerColor = Gold
                )
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(icon, contentDescription = label, tint = if (active) GoldLight else Muted, modifier = Modifier.size(20.dp))
                    Text(label, color = if (active) GoldLight else Muted, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun StbPlayNavigationRail(
    selectedTab: StbPlayTab,
    collapsed: Boolean,
    compactTv: Boolean,
    onToggle: () -> Unit,
    onTabSelected: (StbPlayTab) -> Unit,
    railRequesters: Map<StbPlayTab, FocusRequester>,
    onEnterPage: (StbPlayTab) -> Unit
) {
    val tabs = listOf(
        Triple(StbPlayTab.HOME, "Home", Icons.Filled.Home),
        Triple(StbPlayTab.LIVE, "Live TV", Icons.Filled.LiveTv),
        Triple(StbPlayTab.CONTENT, "Movies & Series", Icons.Filled.Movie),
        Triple(StbPlayTab.FAVOURITES, "Favourites", Icons.Filled.Favorite),
        Triple(StbPlayTab.SETTINGS, "Settings", Icons.Filled.Settings)
    )
    Column(
        modifier = Modifier
            .width(if (collapsed) (if (compactTv) 62.dp else 78.dp) else (if (compactTv) 160.dp else 198.dp))
            .fillMaxHeight()
            .background(Rail)
            .then(if (compactTv) Modifier else Modifier.animateContentSize())
            .zIndex(2f)
            .padding(horizontal = if (compactTv) 6.dp else 12.dp, vertical = if (compactTv) 10.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactTv) 3.dp else 8.dp)
    ) {
        QuestSurface(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().height(if (compactTv) 42.dp else 48.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Gold)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                contentAlignment = if (collapsed) Alignment.Center else Alignment.CenterStart
            ) {
                if (collapsed) {
                    Image(painterResource(R.drawable.icon_blue), "STB Play", contentScale = ContentScale.Fit, modifier = Modifier.size(32.dp))
                } else {
                    Image(painterResource(R.drawable.icon_blue), "STB Play", contentScale = ContentScale.Fit, modifier = Modifier.width(52.dp).height(42.dp))
                }
            }
        }
        Text(
            if (collapsed) "" else "Premium OTT Experience",
            color = Muted,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        tabs.forEach { (tab, title, icon) ->
            NavItem(
                label = title,
                icon = icon,
                collapsed = collapsed,
                compactTv = compactTv,
                selected = selectedTab == tab,
                focusModifier = if (compactTv) Modifier.focusRequester(railRequesters.getValue(tab))
                    .testTag("tv-rail:${tab.name}")
                    .focusProperties {
                        val position = StbPlayTab.entries.indexOf(tab)
                        up = StbPlayTab.entries.getOrNull(position - 1)?.let { railRequesters.getValue(it) } ?: FocusRequester.Cancel
                        down = StbPlayTab.entries.getOrNull(position + 1)?.let { railRequesters.getValue(it) } ?: FocusRequester.Cancel
                        left = FocusRequester.Cancel
                    }.onPreviewKeyEvent { event ->
                        if (event.key == Key.DirectionRight) {
                            if (event.type == KeyEventType.KeyDown) onEnterPage(tab)
                            true
                        } else false
                    } else Modifier,
                onClick = { if (compactTv) onEnterPage(tab) else onTabSelected(tab) }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (!collapsed) {
            Text("Android TV", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 10.dp, bottom = 4.dp))
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, collapsed: Boolean, compactTv: Boolean, selected: Boolean, focusModifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    var showFocusLabel by remember { mutableStateOf(false) }
    // On TV, the selected navigation item and the first page control were both
    // requesting initial focus. Let the page own initial focus on TV.
    val initialFocus = if (selected && !compactTv) Modifier.questInitialFocus() else Modifier
    LaunchedEffect(collapsed, showFocusLabel) {
        if (collapsed && showFocusLabel) {
            delay(2_000)
            showFocusLabel = false
        }
    }
    Box(Modifier.fillMaxWidth().height(if (compactTv) 42.dp else 48.dp)) {
        QuestSurface(
            onClick = onClick, tvContainerColor = if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent, tvFocusedContainerColor = Gold,
            modifier = focusModifier.then(initialFocus).fillMaxSize().onFocusChanged {
                focused = it.isFocused
                if (it.isFocused && collapsed) showFocusLabel = true
                if (!it.isFocused) showFocusLabel = false
            },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent,
                focusedContainerColor = Gold
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, GoldLight))
            )
        ) {
            Box(
                Modifier.fillMaxSize().padding(horizontal = if (collapsed) 0.dp else 12.dp),
                contentAlignment = if (collapsed) Alignment.Center else Alignment.CenterStart
            ) {
                if (collapsed) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = when {
                            focused -> OnAccent
                            selected -> GoldLight
                            else -> White
                        },
                        modifier = Modifier.size(23.dp)
                    )
                } else {
                    Text(
                        label,
                        color = if (focused) OnAccent else White,
                        fontSize = 13.sp,
                        fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (collapsed && showFocusLabel) {
            Text(
                label,
                color = White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 68.dp)
                    .zIndex(3f)
                    .widthIn(min = 100.dp)
                    .background(PanelSoft, RoundedCornerShape(8.dp))
                    .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun StbPlayHeader(
    selectedTab: StbPlayTab,
    themePreference: ThemePreference,
    onSearchClick: () -> Unit,
    compact: Boolean = false,
    denseTv: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(if (compact) 60.dp else if (denseTv) 58.dp else 76.dp)
            .padding(horizontal = if (compact) 14.dp else if (denseTv) 16.dp else 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                when (selectedTab) {
                    StbPlayTab.HOME -> "Welcome back"
                    StbPlayTab.LIVE -> "Live TV"
                    StbPlayTab.CONTENT -> "Movies & Series"
                    StbPlayTab.FAVOURITES -> "Favourites"
                    StbPlayTab.SETTINGS -> "Settings"
                },
                color = White,
                fontSize = if (compact || denseTv) 18.sp else 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HeaderAction(if (compact) "⌕" else "Search", onSearchClick, modifier = Modifier.width(if (compact) 44.dp else if (denseTv) 92.dp else 120.dp))
        Spacer(modifier = Modifier.width(if (compact) 6.dp else 10.dp))
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalContentColor provides White) {
                    MediaRouteButton(modifier = Modifier.size(44.dp))
                }
                Text("Cast", color = White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    QuestSurface(
        onClick = onClick, tvContainerColor = Panel, tvFocusedContainerColor = Gold,
        modifier = modifier.height(40.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Gold),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, GoldLight)))
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 15.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (focused) OnAccent else White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StbPlayHomeScreen(
    state: StbPlayHomeState,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onRemoveHistory: (UiMedia) -> Unit,
    onRefresh: () -> Unit,
    onEditPortal: () -> Unit,
    onInteraction: () -> Unit
) {
    if (state.loading) {
        LoadingContent("Loading your portal…")
        return
    }
    val compact = isCompactAndroidLayout()
    val denseTv = isTelevisionLayout()
    val visibleRows = remember(state.rows) { state.rows.filter { it.items.isNotEmpty() } }
    val homeListState = rememberLazyListState()
    val homeDensity = LocalDensity.current
    val homeScope = rememberCoroutineScope()
    val heroFocusRequester = remember { FocusRequester() }
    val restoreFocusRequester = remember { FocusRequester() }
    var focusedHomeRowId by rememberSaveable { mutableStateOf<String?>(null) }
    var focusedHomeItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    val focusNavigation = LocalTvFocusNavigation.current
    val restoredRow = visibleRows.firstOrNull { it.id == focusedHomeRowId }
    val restoredItem = restoredRow?.items?.firstOrNull { it.id == focusedHomeItemId }
    TvPageEntry(if (restoredItem != null) "home:${restoredRow.id}:${restoredItem.id}"
        else if (state.heroes.isNotEmpty()) "home:hero"
        else visibleRows.firstOrNull()?.let { "home:${it.id}:${it.items.first().id}" })
    val rowFocusRequesters = remember(visibleRows.map { it.id }) {
        if (denseTv) visibleRows.associate { it.id to FocusRequester() } else emptyMap()
    }
    val rowListStates = if (denseTv) visibleRows.associate { row ->
        row.id to key(row.id) { rememberLazyListState() }
    } else emptyMap()
    val firstRowLazyIndex = (if (state.portalWarning != null) 1 else 0) + 1 +
        (if (state.expiryText.isNullOrBlank()) 0 else 1)
    fun focusHomeRow(index: Int) {
        if (!denseTv) return
        val row = visibleRows.getOrNull(index) ?: return
        val requester = rowFocusRequesters[row.id] ?: return
        navigationJob?.cancel()
        val lazyIndex = firstRowLazyIndex + index
        val rowState = rowListStates[row.id] ?: return
        val alreadyVisible = homeListState.layoutInfo.visibleItemsInfo.any { it.index == lazyIndex }
        navigationJob = homeScope.launch {
            if (!alreadyVisible) {
                val viewport = homeListState.layoutInfo.viewportSize.height
                val rowCenter = (if (row.items.any { it.portrait }) 101 else 60) + 34
                val targetOffset = with(homeDensity) { rowCenter.dp.roundToPx() } - viewport / 2
                homeListState.animateScrollToItem(lazyIndex, targetOffset)
                withFrameNanos { }
            }
            if (rowState.firstVisibleItemIndex != 0 || rowState.firstVisibleItemScrollOffset != 0) rowState.scrollToItem(0)
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
    }
    fun focusHomeHero() {
        navigationJob?.cancel()
        val heroIndex = if (state.portalWarning != null) 1 else 0
        if (homeListState.layoutInfo.visibleItemsInfo.any { it.index == heroIndex } &&
            runCatching { heroFocusRequester.requestFocus() }.isSuccess) return
        navigationJob = homeScope.launch {
            homeListState.animateScrollToItem(heroIndex)
            withFrameNanos { }
            runCatching { heroFocusRequester.requestFocus() }
        }
    }
    LaunchedEffect(denseTv) {
        if (denseTv && focusNavigation == null) {
            withFrameNanos { }
            if (focusedHomeRowId == null) {
                if (state.heroes.isEmpty()) focusHomeRow(0) else runCatching { heroFocusRequester.requestFocus() }
            }
            else if (runCatching { restoreFocusRequester.requestFocus() }.isFailure) {
                val index = visibleRows.indexOfFirst { it.id == focusedHomeRowId }
                if (index >= 0) focusHomeRow(index)
                else if (visibleRows.isNotEmpty()) focusHomeRow(0)
                else runCatching { heroFocusRequester.requestFocus() }
            }
        }
    }
    LaunchedEffect(focusNavigation, restoredItem?.id) {
        if (focusNavigation != null && restoredItem != null && restoredRow != null) {
            val index = visibleRows.indexOf(restoredRow)
            if (homeListState.layoutInfo.visibleItemsInfo.none { it.index == firstRowLazyIndex + index })
                homeListState.scrollToItem(firstRowLazyIndex + index)
            val itemIndex = restoredRow.items.indexOf(restoredItem)
            rowListStates[restoredRow.id]?.let { rowState ->
                if (rowState.layoutInfo.visibleItemsInfo.none { it.index == itemIndex }) rowState.scrollToItem(itemIndex)
            }
        }
    }
    val homeContent: @Composable () -> Unit = {
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent {
                if (denseTv && it.type == KeyEventType.KeyDown) onInteraction()
                false
            },
            contentPadding = PaddingValues(start = if (compact || denseTv) 14.dp else 30.dp, end = if (compact || denseTv) 14.dp else 30.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact || denseTv) 11.dp else 22.dp)
        ) {
            state.portalWarning?.let { warning ->
                item { PortalConnectionWarning(warning, onRefresh, onEditPortal) }
            }
            item {
                if (state.heroes.isEmpty()) EmptyState("No content found", "Connect a portal with an active catalogue to start watching.")
                else RotatingHero(
                    state.heroes,
                    onMediaClick,
                    onToggleFavorite,
                    focusRequester = heroFocusRequester,
                    onFocusDown = { focusHomeRow(0) },
                    onFocused = { focusedHomeRowId = null; focusedHomeItemId = null }
                )
            }
            state.expiryText?.takeIf { it.isNotBlank() }?.let { expiry ->
                item { StatusPill(expiry, GoldLight) }
            }
            indexedColumnItems(visibleRows, key = { _, row -> row.id }) { index, row ->
                MediaRow(
                    row,
                    onMediaClick,
                    onToggleFavorite,
                    onRemoveHistory,
                    listState = rowListStates[row.id],
                    firstCardFocusRequester = rowFocusRequesters[row.id],
                    staticTvCards = denseTv,
                    restoreItemId = if (row.id == focusedHomeRowId) focusedHomeItemId else null,
                    restoreFocusRequester = restoreFocusRequester,
                    onMediaFocused = { focusedHomeRowId = row.id; focusedHomeItemId = it.id },
                    onFocusDown = if (denseTv && index < visibleRows.lastIndex) ({ focusHomeRow(index + 1) }) else null,
                    onFocusUp = if (denseTv) ({ if (index == 0) focusHomeHero() else focusHomeRow(index - 1) }) else null
                )
            }
        }
    }
    if (denseTv) CompositionLocalProvider(LocalBringIntoViewSpec provides TvVerticalBringIntoViewSpec) { homeContent() }
    else homeContent()
}

@Composable
private fun PortalConnectionWarning(message: String, onRetry: () -> Unit, onEditPortal: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF32251B)).border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text("Portal needs attention", color = GoldLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(message, color = White, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            WideAction("Retry", onRetry, Modifier.weight(1f))
            WideAction("Update details", onEditPortal, Modifier.weight(1f))
        }
    }
}

@Composable
private fun RotatingHero(
    heroes: List<UiMedia>,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    focusRequester: FocusRequester,
    onFocusDown: () -> Unit,
    onFocused: () -> Unit
) {
    val denseTv = isTelevisionLayout()
    var index by remember(heroes.map { it.id }) { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(heroes.map { it.id }, focused, denseTv) {
        if (!denseTv && !focused && heroes.size > 1) {
            while (true) {
                delay(8_000)
                index = (index + 1) % heroes.size
            }
        }
    }
    val item = heroes.getOrNull(index.coerceIn(0, (heroes.size - 1).coerceAtLeast(0))) ?: return
    val focusNavigation = LocalTvFocusNavigation.current
    val heroModifier = Modifier.tvFocusTarget("home:hero")
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionLeft && focusNavigation != null) {
                    if (event.type == KeyEventType.KeyDown) focusNavigation.returnToRail()
                    true
                } else false
            }
            .focusRequester(focusRequester)
            .fillMaxWidth()
        .height(if (isCompactAndroidLayout()) 205.dp else if (denseTv) 230.dp else 290.dp)
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
            .onPreviewKeyEvent { event ->
                if (denseTv && event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                    onFocusDown()
                    true
                } else false
            }
    val density = LocalDensity.current
    val heroContent: @Composable BoxScope.() -> Unit = {
        Row(Modifier.fillMaxSize().padding(if (isCompactAndroidLayout() || denseTv) 10.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp, horizontal = if (isCompactAndroidLayout()) 5.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompactAndroidLayout() || denseTv) 5.dp else 8.dp)
            ) {
                Text(item.badge ?: item.streamType.uppercase(), color = Color(0xFFF6D896), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.title, color = PosterWhite, fontSize = if (isCompactAndroidLayout() || denseTv) 20.sp else 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color(0xFFD3DBE6), fontSize = 12.sp, maxLines = if (isCompactAndroidLayout()) 2 else 3, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!denseTv) QuestButton(
                        onClick = { onMediaClick(item) },
                        modifier = Modifier.height(38.dp),
                        colors = ButtonDefaults.colors(containerColor = Gold, contentColor = OnAccent, focusedContainerColor = GoldLight, focusedContentColor = OnAccent)
                    ) { Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    else Text("Select for details", color = Muted, fontSize = 11.sp)
                    if (!denseTv) FavoriteButton(item.isFavorite, { onToggleFavorite(item) }, buttonSize = 38.dp)
                }
            }
            Box(
                Modifier.width(if (isCompactAndroidLayout()) 112.dp else if (denseTv) 155.dp else 190.dp)
                    .fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(Color(0xFF101319))
            ) {
                ArtworkImage(
                    imageUrl = item.imageUrl,
                    title = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    requestHeaders = item.imageHeaders,
                    fallbackText = null,
                    decodeWidthPx = if (denseTv) with(density) { 155.dp.roundToPx() } else null,
                    decodeHeightPx = if (denseTv) with(density) { 210.dp.roundToPx() } else null
                )
                if (!denseTv && heroes.size > 1) {
                    Text("${index + 1} / ${heroes.size}", color = White, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd).padding(7.dp))
                }
            }
        }
    }
    if (denseTv) TvHomeSurface({ onMediaClick(item) }, heroModifier, content = heroContent)
    else QuestSurface(
        onClick = { onMediaClick(item) }, modifier = heroModifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = PanelSoft, focusedContainerColor = PanelSoft),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Gold))),
        content = heroContent
    )
}

@Composable
private fun MediaRow(
    row: UiMediaRow,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onRemoveHistory: (UiMedia) -> Unit,
    listState: LazyListState? = null,
    firstCardFocusRequester: FocusRequester? = null,
    staticTvCards: Boolean = false,
    restoreItemId: String? = null,
    restoreFocusRequester: FocusRequester? = null,
    onMediaFocused: (UiMedia) -> Unit = {},
    onFocusDown: (() -> Unit)? = null,
    onFocusUp: (() -> Unit)? = null
) {
    val denseTv = isTelevisionLayout()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(row.title, color = White, fontSize = if (denseTv) 18.sp else 21.sp, fontWeight = FontWeight.SemiBold)
            row.subtitle?.let { Text(it, color = Muted, fontSize = 12.sp) }
        }
        val shelf: @Composable () -> Unit = { LazyRow(
            state = listState ?: rememberLazyListState(),
            modifier = Modifier.focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(if (denseTv) 10.dp else 15.dp),
            contentPadding = PaddingValues(end = 22.dp)
        ) {
            indexedColumnItems(
                row.items,
                key = { _, media -> media.id },
                contentType = { _, media -> if (media.portrait) "poster" else "landscape" }
            ) { index, media ->
                if (denseTv && staticTvCards) TvHomeMediaCard(
                    item = media,
                    onClick = { onMediaClick(media) },
                    focusRequester = if (index == 0) firstCardFocusRequester else null,
                    restoreFocusRequester = if (media.id == restoreItemId) restoreFocusRequester else null,
                    onFocused = { onMediaFocused(media) },
                    focusId = "home:${row.id}:${media.id}",
                    onReturnToCategory = if (index == 0) LocalTvFocusNavigation.current?.returnToRail else null,
                    onFocusDown = onFocusDown,
                    onFocusUp = onFocusUp
                ) else MediaCard(
                    item = media,
                    onClick = { onMediaClick(media) },
                    onFocused = { onMediaFocused(media) },
                    onToggleFavorite = { onToggleFavorite(media) },
                    onRemoveHistory = if (row.id == "continue") ({ onRemoveHistory(media) }) else null,
                    focusRequester = if (index == 0) firstCardFocusRequester else null,
                    onReturnToCategory = if (index == 0) LocalTvFocusNavigation.current?.returnToRail else null,
                    onFocusDown = onFocusDown,
                    onFocusUp = onFocusUp
                )
            }
        } }
        if (denseTv) CompositionLocalProvider(LocalBringIntoViewSpec provides TvHomeBringIntoViewSpec) { shelf() }
        else shelf()
    }
}

@Composable
private fun TvHomeMediaCard(
    item: UiMedia,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    restoreFocusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    focusId: String = "media:${item.id}",
    onFocusDown: (() -> Unit)? = null,
    onFocusUp: (() -> Unit)? = null,
    onReturnToCategory: (() -> Unit)? = null,
    initialFocus: Boolean = false,
    compactGrid: Boolean = false
) {
    val width = if (item.portrait) 142.dp else 205.dp
    val height = if (item.portrait) 202.dp else 120.dp
    val footerHeight = if (item.portrait) 46.dp else 38.dp
    val density = LocalDensity.current
    val cardModifier = if (compactGrid) Modifier.fillMaxWidth().aspectRatio(width.value / height.value)
        else Modifier.width(width).height(height)
    Box(cardModifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) false
        else when {
            event.key == Key.DirectionDown && onFocusDown != null -> { onFocusDown(); true }
            event.key == Key.DirectionUp && onFocusUp != null -> { onFocusUp(); true }
            event.key == Key.DirectionLeft && onReturnToCategory != null -> { onReturnToCategory(); true }
            else -> false
        }
    }) {
        TvHomeSurface(
            onClick,
            Modifier.fillMaxSize().tvFocusTarget(focusId).then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .then(restoreFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .then(if (initialFocus) Modifier.questInitialFocus() else Modifier)
                .onFocusChanged { if (it.isFocused) onFocused() }
        ) {
            Column(Modifier.fillMaxSize()) {
                ArtworkImage(
                    imageUrl = item.imageUrl, title = item.title,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    requestHeaders = item.imageHeaders, fallbackColor = Gold,
                    decodeWidthPx = with(density) { width.roundToPx() },
                    decodeHeightPx = with(density) { (height - footerHeight).roundToPx() }
                )
                Box(Modifier.fillMaxWidth().height(footerHeight).background(Panel), contentAlignment = Alignment.CenterStart) {
                    Text(item.title, color = White, fontSize = 12.sp, lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
            item.badge?.let {
                Text(it, color = GoldLight, fontSize = 9.sp, maxLines = 1,
                    modifier = Modifier.align(Alignment.TopStart).background(Panel).padding(horizontal = 4.dp, vertical = 2.dp))
            }
            if (item.isLocked) StatusPill("PIN", GoldLight, Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
            if (item.progress > 0f) ProgressBar(item.progress, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun LiveTvScreen(
    state: StbPlayLibraryState,
    channelListState: LazyListState,
    focusedChannelId: String?,
    onCategorySelected: (Int) -> Unit,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onMediaFocused: (UiMedia) -> Unit,
    playingChannelId: String? = null,
    previewActionsRequester: FocusRequester? = null,
    preview: (@Composable (Modifier, () -> Unit) -> Unit)? = null
) {
    val denseTv = isTelevisionLayout()
    val channelRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val initialChannelFocusId = focusedChannelId?.takeIf { id -> state.items.any { it.id == id } }
        ?: state.items.firstOrNull()?.id
    val categoryKey = state.categories.getOrNull(state.selectedCategory)?.id
    var localFocusedChannelId by rememberSaveable(categoryKey) { mutableStateOf(focusedChannelId) }
    TvPageEntry(localFocusedChannelId?.takeIf { id -> state.items.any { it.id == id } }?.let { "live:$it" }
        ?: state.categories.getOrNull(state.selectedCategory)?.let { "category:${it.id}" })
    val focusNavigation = LocalTvFocusNavigation.current
    val channelIndices = remember(state.items) { state.items.mapIndexed { index, item -> item.id to index }.toMap() }
    val navigationScope = rememberCoroutineScope()
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    fun moveChannelFocus(direction: Int) {
        val current = channelIndices[localFocusedChannelId] ?: 0
        val next = (current + direction).coerceIn(0, state.items.lastIndex)
        val item = state.items.getOrNull(next) ?: return
        localFocusedChannelId = item.id
        navigationJob?.cancel()
        navigationJob = navigationScope.launch {
            val layout = channelListState.layoutInfo
            val rowHeight = layout.visibleItemsInfo.firstOrNull()?.size ?: 32
            val pivot = ((layout.viewportEndOffset - layout.viewportStartOffset - rowHeight) / 2).coerceAtLeast(0)
            channelListState.scrollToItem(next, -pivot)
            withFrameNanos { }
            channelRequesters[item.id]?.requestFocus()
        }
    }
    var restoredChannelPosition by remember(categoryKey) { mutableStateOf(false) }
    LaunchedEffect(categoryKey, state.loading, state.items.isEmpty()) {
        if (!state.loading && state.items.isNotEmpty() && !restoredChannelPosition) {
            val restoreId = if (denseTv) localFocusedChannelId else focusedChannelId
            val focusedIndex = state.items.indexOfFirst { it.id == restoreId }
            if (focusedIndex >= 0 && (!denseTv || channelListState.layoutInfo.visibleItemsInfo.none { it.index == focusedIndex }))
                channelListState.scrollToItem(focusedIndex)
            if (denseTv && focusNavigation == null && focusedIndex >= 0) {
                withFrameNanos { }
                withFrameNanos { }
                channelRequesters[restoreId]?.requestFocus()
            }
            restoredChannelPosition = true
        }
    }
    if (isCompactAndroidLayout()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
            CompactCategorySelector(state.categories, state.selectedCategory, onCategorySelected)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Live TV", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(state.totalItemsText.ifBlank { "${state.items.size} channels" }, color = Muted, fontSize = 11.sp)
            }
            when {
                state.loading -> LoadingContent("Loading channels…")
                state.items.isEmpty() -> EmptyState("No channels in this category", state.emptyMessage)
                else -> LazyColumn(state = channelListState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                    columnItems(state.items, key = { it.id }, contentType = { "channel" }) { channel ->
                        LiveChannelRow(channel, { onMediaClick(channel) }, { onToggleFavorite(channel) }, initialFocus = channel.id == initialChannelFocusId)
                    }
                }
            }
        }
        return
    }
    val categoryListState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.selectedCategory.coerceIn(0, (state.categories.size - 1).coerceAtLeast(0))
    )
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val categoryScope = rememberCoroutineScope()
    val initialCategoryFocusRequested = remember { mutableStateOf(false) }
    LaunchedEffect(state.categories.size, state.loading) {
        if (focusNavigation == null && !state.loading && state.categories.isNotEmpty() && !initialCategoryFocusRequested.value) {
            val selected = state.selectedCategory.coerceIn(0, state.categories.lastIndex)
            if (!denseTv || categoryListState.layoutInfo.visibleItemsInfo.none { it.index == selected }) categoryListState.scrollToItem(selected)
            withFrameNanos { }
            withFrameNanos { }
            val restoreId = if (denseTv) localFocusedChannelId else focusedChannelId
            if (restoreId.isNullOrBlank() || state.items.none { it.id == restoreId }) {
                selectedCategoryFocusRequester.requestFocus()
            }
            initialCategoryFocusRequested.value = true
        }
    }
    if (!denseTv) LaunchedEffect(state.selectedCategory, state.categories.size) {
        if (state.categories.isNotEmpty()) categoryListState.animateScrollToItem(state.selectedCategory.coerceIn(0, state.categories.lastIndex))
    }
    fun focusSelectedCategory() {
        if (state.categories.isEmpty()) return
        val selected = state.selectedCategory.coerceIn(0, (state.categories.size - 1).coerceAtLeast(0))
        categoryScope.launch {
            if (categoryListState.layoutInfo.visibleItemsInfo.none { it.index == selected }) categoryListState.scrollToItem(selected)
            withFrameNanos { }
            selectedCategoryFocusRequester.requestFocus()
        }
    }
    Row(modifier = Modifier.fillMaxSize().padding(start = if (denseTv) 14.dp else 24.dp, end = if (denseTv) 16.dp else 30.dp, bottom = if (denseTv) 14.dp else 28.dp)) {
        CategorySidebar(state.categories, state.selectedCategory, categoryListState, selectedCategoryFocusRequester, onCategorySelected,
            onEnterItems = if (denseTv) ({
                val item = state.items.firstOrNull { it.id == localFocusedChannelId } ?: state.items.firstOrNull()
                if (item != null) categoryScope.launch {
                    val index = channelIndices[item.id] ?: 0
                    if (channelListState.layoutInfo.visibleItemsInfo.none { it.index == index }) channelListState.scrollToItem(index)
                    val target = snapshotFlow { focusNavigation?.targets?.get("live:${item.id}") }.first { it != null }
                    target?.requestFocus()
                }
            }) else null)
        Spacer(modifier = Modifier.width(if (denseTv) 12.dp else 24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Live TV", color = White, fontSize = if (denseTv) 23.sp else 30.sp, fontWeight = FontWeight.Bold)
                    Text("Choose a channel from your provider", color = Muted, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(state.totalItemsText.ifBlank { "${state.items.size} channels" }, color = Muted, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(if (denseTv) 10.dp else 17.dp))
            when {
                state.loading -> LoadingContent("Loading channels…")
                state.items.isEmpty() -> EmptyState("No channels in this category", state.emptyMessage)
                else -> {
                  val channelList: @Composable () -> Unit = { LazyColumn(
                    state = channelListState,
                    modifier = Modifier.fillMaxSize().focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(if (denseTv) 2.dp else 9.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    columnItems(state.items, key = { it.id }, contentType = { "channel" }) { channel ->
                        val requester = remember(channel.id) { FocusRequester() }
                        DisposableEffect(channel.id) {
                            channelRequesters[channel.id] = requester
                            onDispose { channelRequesters.remove(channel.id) }
                        }
                        LiveChannelRow(
                            channel,
                            { onMediaClick(channel) },
                            { onToggleFavorite(channel) },
                            initialFocus = !denseTv && channel.id == initialChannelFocusId,
                            focusRequester = if (denseTv) requester else null,
                            onFocused = { if (denseTv) localFocusedChannelId = channel.id else onMediaFocused(channel) },
                            onReturnToCategory = if (denseTv) ({ focusSelectedCategory() }) else null,
                            onEnterPreview = if (denseTv && preview != null) ({ previewActionsRequester?.requestFocus() }) else null,
                            playing = denseTv && channel.id == playingChannelId,
                            onMoveChannel = if (denseTv) ({ direction -> moveChannelFocus(direction) }) else null
                        )
                    }
                }
                  }
                  if (denseTv) CompositionLocalProvider(LocalBringIntoViewSpec provides TvHomeBringIntoViewSpec) { channelList() }
                  else channelList()
                }
            }
        }
        if (denseTv && preview != null) {
            Spacer(Modifier.width(14.dp))
            preview(Modifier.weight(0.95f).fillMaxHeight()) {
                categoryScope.launch {
                    val index = channelIndices[localFocusedChannelId] ?: -1
                    if (index >= 0) {
                        if (channelListState.layoutInfo.visibleItemsInfo.none { it.index == index })
                            channelListState.scrollToItem(index)
                        withFrameNanos { }
                        runCatching { channelRequesters[localFocusedChannelId]?.requestFocus() }
                    } else focusSelectedCategory()
                }
            }
        }
    }
}

@Composable
private fun CategorySidebar(
    categories: List<UiCategory>,
    selected: Int,
    listState: LazyListState,
    selectedFocusRequester: FocusRequester,
    onSelected: (Int) -> Unit,
    onEnterItems: (() -> Unit)? = null
) {
    val denseTv = isTelevisionLayout()
    val focusNavigation = LocalTvFocusNavigation.current
    Column(
        modifier = Modifier.width(if (denseTv) 170.dp else 210.dp).fillMaxHeight()
            .clip(RoundedCornerShape(15.dp))
            .background(Panel)
            .border(1.dp, Color(0xFF292929), RoundedCornerShape(15.dp))
            .padding(if (denseTv) 7.dp else 12.dp)
    ) {
        Text("Categories", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(if (denseTv) 2.dp else 6.dp)) {
            indexedColumnItems(categories, key = { _, category -> category.id }, contentType = { _, _ -> "category" }) { index, category ->
                CategorySidebarItem(category, index == selected, if (index == selected) selectedFocusRequester else null,
                    onEnterItems = onEnterItems) { onSelected(index) }
            }
        }
    }
}

@Composable
private fun CompactCategorySelector(categories: List<UiCategory>, selected: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.getOrNull(selected)
    Box(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        QuestSurface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Gold)
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selectedCategory?.title ?: "All categories", color = White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${categories.size}", color = Muted, fontSize = 11.sp)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose category", tint = GoldLight)
            }
        }
        if (expanded) Dialog(onDismissRequest = { expanded = false }) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.fillMaxWidth().height((maxHeight * 0.75f).coerceAtMost(520.dp))
                        .clip(RoundedCornerShape(14.dp)).background(Panel).padding(12.dp)
                ) {
                    Text("Categories", color = GoldLight, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp))
                    LazyColumn(state = rememberLazyListState(initialFirstVisibleItemIndex = selected),
                        verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        indexedColumnItems(categories, key = { _, category -> category.id }) { index, category ->
                            QuestSurface(
                                onClick = { expanded = false; onSelected(index) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .then(if (index == selected) Modifier.questInitialFocus() else Modifier),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (index == selected) Gold.copy(alpha = 0.2f) else Panel,
                                    focusedContainerColor = Gold)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(category.title, color = White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    if (category.isLocked) Text("PIN", color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySidebarItem(category: UiCategory, selected: Boolean, focusRequester: FocusRequester? = null, onEnterItems: (() -> Unit)? = null, onClick: () -> Unit) {
    val denseTv = isTelevisionLayout()
    var focused by remember { mutableStateOf(false) }
    if (denseTv) {
        val navigation = LocalTvFocusNavigation.current
        TvHomeSurface(onClick,
            Modifier.tvFocusTarget("category:${category.id}").onPreviewKeyEvent { event ->
                when {
                    event.key == Key.DirectionLeft && navigation != null -> {
                        if (event.type == KeyEventType.KeyDown) navigation.returnToRail(); true
                    }
                    event.key == Key.DirectionRight && onEnterItems != null -> {
                        if (event.type == KeyEventType.KeyDown) onEnterItems(); true
                    }
                    else -> false
                }
            }.then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .fillMaxWidth().height(48.dp).onFocusChanged { focused = it.isFocused },
            containerColor = if (selected) Gold.copy(alpha = 0.2f) else Color.Transparent,
            focusedContainerColor = Gold
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(category.title, color = if (focused) OnAccent else White, fontSize = 11.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (category.isLocked) Text("PIN", color = if (focused) OnAccent else GoldLight, fontSize = 9.sp)
            }
        }
        return
    }
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .fillMaxWidth().height(if (denseTv) 48.dp else 58.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Gold.copy(alpha = 0.2f) else Color.Transparent,
            focusedContainerColor = Gold
        ),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, GoldLight)))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(category.title, color = if (focused) OnAccent else White, fontSize = if (denseTv) 11.sp else 12.sp,
                lineHeight = if (denseTv) 13.sp else 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            if (category.isLocked) Text("PIN", color = if (focused) OnAccent else GoldLight,
                fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun LiveChannelRow(
    item: UiMedia,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    initialFocus: Boolean = false,
    onReturnToCategory: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onEnterPreview: (() -> Unit)? = null,
    playing: Boolean = false,
    onMoveChannel: ((Int) -> Unit)? = null
) {
    val denseTv = isTelevisionLayout()
    if (denseTv) {
        var focused by remember { mutableStateOf(false) }
        TvHomeSurface(onClick,
            Modifier.fillMaxWidth().height(32.dp).tvFocusTarget("live:${item.id}")
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
                .onPreviewKeyEvent { event ->
                    if (onMoveChannel != null && event.key in listOf(Key.DirectionDown, Key.DirectionUp)) {
                        if (event.type == KeyEventType.KeyDown) onMoveChannel(if (event.key == Key.DirectionDown) 1 else -1)
                        true
                    } else if (onReturnToCategory != null && event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                        onReturnToCategory(); true
                    } else if (onEnterPreview != null && event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                        onEnterPreview(); true
                    } else false
                },
            containerColor = if (playing) Gold.copy(alpha = 0.22f) else Panel,
            focusedContainerColor = Gold
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtworkImage(item.imageUrl, item.title, Modifier.width(42.dp).height(24.dp),
                    contentScale = ContentScale.Fit, requestHeaders = item.imageHeaders,
                    fallbackText = item.badge ?: "LIVE", fallbackTextSize = 8.sp, fallbackColor = GoldLight)
                Spacer(Modifier.width(6.dp))
                Text(item.title, color = if (focused) OnAccent else White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (playing) Text("▶", color = if (focused) OnAccent else GoldLight, fontSize = 11.sp)
                if (item.isLocked) StatusPill("PIN", GoldLight)
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth().height(if (denseTv) 32.dp else 78.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (denseTv) 4.dp else 8.dp)) {
        QuestSurface(
            onClick = onClick,
            modifier = Modifier.then(if (initialFocus) Modifier.questInitialFocus() else Modifier)
                .onPreviewKeyEvent { event ->
                    if (onReturnToCategory != null && event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                        onReturnToCategory()
                        true
                    } else false
                }.weight(1f).fillMaxHeight(),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(if (denseTv) 6.dp else 13.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Color(0xFF292929)),
            border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Gold))),
            focusScale = if (denseTv) 1.03f else null
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = if (denseTv) 6.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(if (denseTv) 42.dp else 102.dp).height(if (denseTv) 24.dp else 56.dp).clip(RoundedCornerShape(4.dp)).background(PanelSoft), contentAlignment = Alignment.Center) {
                ArtworkImage(
                    imageUrl = item.imageUrl,
                    title = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    requestHeaders = item.imageHeaders,
                    fallbackText = item.badge ?: "LIVE",
                    fallbackTextSize = if (denseTv) 8.sp else 11.sp,
                    fallbackColor = GoldLight
                )
            }
            Spacer(modifier = Modifier.width(if (denseTv) 6.dp else 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = White, fontSize = if (denseTv) 12.sp else 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!denseTv) Text(item.description?.takeIf { it.isNotBlank() } ?: "Live TV", color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (item.isLocked) StatusPill("PIN", GoldLight)
            }
        }
        FavoriteButton(item.isFavorite, onToggleFavorite, Modifier.size(if (denseTv) 28.dp else 46.dp), if (denseTv) 26.dp else 42.dp)
    }
}

@Composable
private fun AutoLoadMoreForGrid(
    gridState: LazyGridState,
    itemCount: Int,
    prefetchDistance: Int,
    hasMore: Boolean,
    loading: Boolean,
    resetKey: String,
    onLoadMore: () -> Unit
) {
    val latestItemCount by rememberUpdatedState(itemCount)
    val latestPrefetchDistance by rememberUpdatedState(prefetchDistance)
    val latestHasMore by rememberUpdatedState(hasMore)
    val latestLoading by rememberUpdatedState(loading)
    val loadMore by rememberUpdatedState(onLoadMore)
    var lastRequestedItemCount by remember(gridState, resetKey) { mutableIntStateOf(-1) }
    var lastRequestedVisibleIndex by remember(gridState, resetKey) { mutableIntStateOf(-1) }
    var requestInFlight by remember(gridState, resetKey) { mutableStateOf(false) }

    LaunchedEffect(gridState, resetKey) {
        snapshotFlow {
            val itemCountNow = latestItemCount
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
            val nearEnd = itemCountNow == 0 ||
                lastVisibleIndex >= (itemCountNow - latestPrefetchDistance).coerceAtLeast(0)
            Triple(lastVisibleIndex, itemCountNow, latestHasMore && !latestLoading && nearEnd)
        }.collect { (lastVisibleIndex, itemCountNow, shouldLoad) ->
            if (latestLoading) {
                requestInFlight = true
                return@collect
            }
            val pageCompleted = requestInFlight
            val newContentOrScroll = itemCountNow > lastRequestedItemCount ||
                lastVisibleIndex > lastRequestedVisibleIndex || pageCompleted
            if (shouldLoad && newContentOrScroll) {
                lastRequestedItemCount = itemCountNow
                lastRequestedVisibleIndex = lastVisibleIndex
                requestInFlight = true
                loadMore()
            }
        }
    }
}

@Composable
private fun ContentBrowserScreen(
    state: StbPlayLibraryState,
    gridState: LazyGridState,
    focusedContentId: String?,
    onLoadMore: () -> Unit,
    onCategorySelected: (Int) -> Unit,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onMediaFocused: (UiMedia) -> Unit
) {
    val selectedCategoryKey = state.categories.getOrNull(state.selectedCategory)?.id ?: state.selectedCategory.toString()
    AutoLoadMoreForGrid(
        gridState = gridState,
        itemCount = state.items.size,
        prefetchDistance = if (isTelevisionLayout()) 14 else 10,
        hasMore = state.hasMore,
        loading = state.loading || state.loadingMore,
        resetKey = selectedCategoryKey,
        onLoadMore = onLoadMore
    )
    if (isCompactAndroidLayout()) {
        ContentBrowserCompact(state, gridState, focusedContentId, onCategorySelected, onMediaClick, onToggleFavorite)
        return
    }
    val denseTv = isTelevisionLayout()
    val categoryListState = rememberLazyListState()
    val focusNavigation = LocalTvFocusNavigation.current
    TvPageEntry(focusedContentId?.takeIf { id -> state.items.any { it.id == id } }?.let { "media:$it" }
        ?: state.categories.getOrNull(state.selectedCategory)?.let { "category:${it.id}" })
    val contentFocusRequester = remember { FocusRequester() }
    val selectedCategoryFocusRequester = remember { FocusRequester() }
    val categoryScope = rememberCoroutineScope()
    val initialCategoryFocusRequested = remember { mutableStateOf(false) }
    LaunchedEffect(state.categories.size, state.loading) {
        if (focusNavigation == null && !state.loading && state.categories.isNotEmpty() && !initialCategoryFocusRequested.value) {
            val selected = state.selectedCategory.coerceIn(0, state.categories.lastIndex)
            if (!denseTv || categoryListState.layoutInfo.visibleItemsInfo.none { it.index == selected }) categoryListState.scrollToItem(selected)
            withFrameNanos { }
            withFrameNanos { }
            if (focusedContentId.isNullOrBlank() || state.items.none { it.id == focusedContentId }) {
                selectedCategoryFocusRequester.requestFocus()
            }
            initialCategoryFocusRequested.value = true
        }
    }
    if (!denseTv) {
        LaunchedEffect(state.selectedCategory, state.categories.size) {
            if (state.categories.isNotEmpty()) categoryListState.animateScrollToItem(state.selectedCategory.coerceIn(0, state.categories.lastIndex))
        }
        LaunchedEffect(selectedCategoryKey, focusedContentId) {
            val restoreIndex = focusedContentId?.let { id -> state.items.indexOfFirst { it.id == id } } ?: -1
            gridState.scrollToItem(restoreIndex.takeIf { it >= 0 } ?: 0)
        }
    }
    var positionRestored by remember(selectedCategoryKey) { mutableStateOf(false) }
    LaunchedEffect(selectedCategoryKey, state.loading, state.items.isEmpty()) {
        if (denseTv && !state.loading && state.items.isNotEmpty() && !positionRestored) {
            val restoreIndex = focusedContentId?.let { id -> state.items.indexOfFirst { it.id == id } } ?: -1
            if (restoreIndex >= 0 && gridState.layoutInfo.visibleItemsInfo.none { it.index == restoreIndex })
                gridState.scrollToItem(restoreIndex)
            if (denseTv && focusNavigation == null && restoreIndex >= 0) {
                withFrameNanos { }
                withFrameNanos { }
                contentFocusRequester.requestFocus()
            }
            positionRestored = true
        }
    }
    fun focusSelectedCategory() {
        if (state.categories.isEmpty()) return
        val selected = state.selectedCategory.coerceIn(0, (state.categories.size - 1).coerceAtLeast(0))
        categoryScope.launch {
            if (categoryListState.layoutInfo.visibleItemsInfo.none { it.index == selected }) categoryListState.scrollToItem(selected)
            withFrameNanos { }
            selectedCategoryFocusRequester.requestFocus()
        }
    }
    Row(modifier = Modifier.fillMaxSize().padding(start = if (denseTv) 14.dp else 24.dp, end = if (denseTv) 16.dp else 30.dp, bottom = if (denseTv) 14.dp else 28.dp)) {
        CategorySidebar(state.categories, state.selectedCategory, categoryListState, selectedCategoryFocusRequester, onCategorySelected,
            onEnterItems = if (denseTv) ({
                val item = state.items.firstOrNull { it.id == focusedContentId } ?: state.items.firstOrNull()
                if (item != null) categoryScope.launch {
                    val index = state.items.indexOf(item)
                    if (gridState.layoutInfo.visibleItemsInfo.none { it.index == index }) gridState.scrollToItem(index)
                    val target = snapshotFlow { focusNavigation?.targets?.get("media:${item.id}") }.first { it != null }
                    target?.requestFocus()
                }
            }) else null)
        Spacer(modifier = Modifier.width(if (denseTv) 12.dp else 24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(if (denseTv) 5.dp else 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(state.categories.getOrNull(state.selectedCategory)?.title?.takeUnless { it == "All" }
                            ?: "All titles", color = White, fontSize = if (denseTv) 22.sp else 30.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(state.totalItemsText.ifBlank { "${state.items.size} titles" }, color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(if (denseTv) 9.dp else 17.dp))
            when {
                state.loading -> LoadingContent("Loading provider catalogue…")
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("No titles in this category", color = White, fontSize = 20.sp)
                        Text(state.emptyMessage, color = Muted, fontSize = 13.sp)
                        if (state.loadingMore) Text("Loading more titles…", color = Muted, fontSize = 12.sp)
                    }
                }
                else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                    val columns = (maxWidth / (if (denseTv) 115.dp else 130.dp)).toInt()
                        .coerceIn(if (denseTv) 5 else 4, if (denseTv) 7 else 6)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        modifier = Modifier.fillMaxSize().focusGroup(),
                        contentPadding = PaddingValues(bottom = if (denseTv) 16.dp else 30.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (denseTv) 8.dp else 12.dp),
                        verticalArrangement = Arrangement.spacedBy(if (denseTv) 10.dp else 16.dp)
                    ) {
                        indexedGridItems(state.items, key = { _, item -> "${item.streamType}:${item.id}" },
                            contentType = { _, item -> if (item.portrait) "poster" else "landscape" }) { index, media ->
                            MediaCard(
                                media, { onMediaClick(media) }, { onToggleFavorite(media) }, compactGrid = true,
                                initialFocus = !denseTv && media.id == focusedContentId,
                                focusRequester = if (denseTv && media.id == focusedContentId) contentFocusRequester else null,
                                onFocused = { onMediaFocused(media) },
                                onReturnToCategory = if (denseTv && index % columns == 0) ({ focusSelectedCategory() }) else null
                            )
                        }
                        if (state.loadingMore) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Loading more titles…", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentBrowserCompact(
    state: StbPlayLibraryState,
    gridState: LazyGridState,
    focusedContentId: String?,
    onCategorySelected: (Int) -> Unit,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        CompactCategorySelector(state.categories, state.selectedCategory, onCategorySelected)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    state.categories.getOrNull(state.selectedCategory)?.title?.takeUnless { it == "All" } ?: "All titles",
                    color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(state.totalItemsText.ifBlank { "${state.items.size} titles" }, color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
        }
        when {
            state.loading -> LoadingContent("Loading provider catalogue…")
            state.items.isEmpty() -> EmptyState("No titles in this category", state.emptyMessage)
            else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                val columns = (maxWidth / 154.dp).toInt().coerceIn(2, 5)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns), state = gridState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(state.items, key = { "${it.streamType}:${it.id}" },
                        contentType = { if (it.portrait) "poster" else "landscape" }) { media ->
                        MediaCard(media, { onMediaClick(media) }, { onToggleFavorite(media) }, compactGrid = true, initialFocus = media.id == focusedContentId)
                    }
                    if (state.loadingMore) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            Text("Loading more titles…", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavouritesScreen(
    state: StbPlayLibraryState,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit
) {
    val channels = state.items.filter { it.streamType == "live" }
    val titles = state.items.filter { it.streamType != "live" }
    var lastItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val preferred = state.items.firstOrNull { it.id == lastItemId } ?: channels.firstOrNull() ?: titles.firstOrNull()
    TvPageEntry(preferred?.let { "media:${it.id}" })
    val navigation = LocalTvFocusNavigation.current
    val favouriteListState = rememberLazyListState()
    val channelRowState = rememberLazyListState()
    val titleRowState = rememberLazyListState()
    LaunchedEffect(navigation, preferred?.id) {
        if (navigation != null && navigation.pending && preferred != null) {
            val isChannel = preferred.streamType == "live"
            val rowIndex = if (isChannel) 1 else 2
            if (favouriteListState.layoutInfo.visibleItemsInfo.none { it.index == rowIndex })
                favouriteListState.scrollToItem(rowIndex)
            val rowState = if (isChannel) channelRowState else titleRowState
            val itemIndex = (if (isChannel) channels else titles).indexOf(preferred)
            if (rowState.layoutInfo.visibleItemsInfo.none { it.index == itemIndex }) rowState.scrollToItem(itemIndex)
        }
    }
    LazyColumn(
        state = favouriteListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = if (isCompactAndroidLayout()) 14.dp else 30.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            Text("Favourites", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Channels and titles saved on this device", color = Muted, fontSize = 13.sp)
        }
        item {
            if (channels.isEmpty()) EmptyInline("Favourite channels", "Use Save on any live channel to add it here.")
            else MediaRow(
                UiMediaRow("favorite-channels", "Favourite channels", items = channels),
                onMediaClick, onToggleFavorite, onRemoveHistory = {}, listState = channelRowState,
                onMediaFocused = { lastItemId = it.id }
            )
        }
        item {
            if (titles.isEmpty()) EmptyInline("Favourite titles", "Use Add to favourites on a movie or series.")
            else MediaRow(
                UiMediaRow("favorite-titles", "Favourite titles", items = titles),
                onMediaClick, onToggleFavorite, onRemoveHistory = {}, listState = titleRowState,
                onMediaFocused = { lastItemId = it.id }
            )
        }
    }
}

@Composable
private fun MediaCard(
    item: UiMedia,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveHistory: (() -> Unit)? = null,
    compactGrid: Boolean = false,
    initialFocus: Boolean = false,
    onReturnToCategory: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onFocusDown: (() -> Unit)? = null,
    onFocusUp: (() -> Unit)? = null,
    onFocused: () -> Unit = {}
) {
    val denseTv = isTelevisionLayout()
    if (denseTv) {
        TvHomeMediaCard(item, onClick, focusRequester = focusRequester, onFocused = onFocused,
            onFocusDown = onFocusDown, onFocusUp = onFocusUp, onReturnToCategory = onReturnToCategory,
            initialFocus = initialFocus, compactGrid = compactGrid)
        return
    }
    val compact = isCompactAndroidLayout()
    val width = if (item.portrait) (if (denseTv) 142.dp else if (compact) 132.dp else 166.dp) else (if (denseTv) 205.dp else if (compact) 190.dp else 235.dp)
    val height = if (item.portrait) (if (denseTv) 202.dp else if (compact) 190.dp else 235.dp) else (if (denseTv) 120.dp else if (compact) 112.dp else 138.dp)
    val cardModifier = if (compactGrid) {
        Modifier.fillMaxWidth().aspectRatio(width.value / height.value)
    } else {
        Modifier.width(width).height(height)
    }
    Box(
        cardModifier.onPreviewKeyEvent { event ->
            if (onFocusDown != null && event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                onFocusDown()
                true
            } else if (onFocusUp != null && event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                onFocusUp()
                true
            } else false
        }
    ) {
      QuestSurface(
        onClick = onClick,
        modifier = Modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .then(if (initialFocus && denseTv) Modifier.questInitialFocus() else Modifier)
            .fillMaxSize().onPreviewKeyEvent { event ->
            if (onReturnToCategory != null && event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                onReturnToCategory()
                true
            } else false
        },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = PanelSoft, focusedContainerColor = PanelSoft),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Gold)))
      ) {
        Box(Modifier.fillMaxSize()) {
            ArtworkImage(
                imageUrl = item.imageUrl,
                title = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                requestHeaders = item.imageHeaders,
                fallbackTextSize = 35.sp,
                fallbackColor = Gold
            )
            Box(
                (if (compactGrid) Modifier.fillMaxWidth().fillMaxHeight(0.4f) else Modifier.fillMaxWidth().height(82.dp))
                    .align(Alignment.BottomCenter).background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xF8070707)))
                )
            )
            item.badge?.let { Text(it, color = Color(0xFFF6D896), fontSize = if (compactGrid) 9.sp else 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(if (compactGrid) 6.dp else 9.dp)) }
            if (item.isLocked) StatusPill("PIN", GoldLight, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            onRemoveHistory?.let { remove ->
                QuestSurface(
                    onClick = remove,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp).height(30.dp),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0xCC070707),
                        focusedContainerColor = Danger
                    )
                ) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
                        Text("Remove", color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(item.title, color = PosterWhite, fontSize = if (compactGrid) 11.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(if (compactGrid) 7.dp else 11.dp))
            if (item.progress > 0f) ProgressBar(item.progress, Modifier.align(Alignment.BottomCenter))
        }
      }
      FavoriteButton(
          item.isFavorite,
          onToggleFavorite,
          Modifier.align(Alignment.TopEnd).padding(if (compactGrid) 4.dp else 6.dp),
          if (compactGrid || compact) 34.dp else 40.dp
      )
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, buttonSize: Dp = 34.dp) {
    QuestSurface(
        onClick = onClick,
        modifier = modifier.size(buttonSize),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(17.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xCC070707), focusedContainerColor = Gold)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isFavorite) "★" else "☆", color = GoldLight, fontSize = 19.sp)
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).background(Color(0x77565656))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(Gold))
    }
}

@Composable
private fun StbPlaySettingsScreen(
    state: StbPlaySettingsState,
    onActivateLicense: (String) -> Unit,
    onRefresh: () -> Unit,
    onClearCache: () -> Unit,
    onClearHistory: () -> Unit,
    onAddPortal: () -> Unit,
    onEditPortal: (PortalSettings) -> Unit,
    onUsePortal: (PortalSettings) -> Unit,
    onDeletePortal: (PortalSettings) -> Unit,
    onProviderPair: (PortalSettings) -> Unit,
    onPlayerPreferenceChanged: (PlayerPreference) -> Unit,
    onAndroidBoxVideoCompatibilityChanged: (Boolean) -> Unit,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    onSubtitlePreferenceChanged: (SubtitlePreference) -> Unit,
    onCatalogueLanguageChanged: (String) -> Unit,
    onAnalyticsChanged: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onParentalModeChanged: (ParentalMode) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onShare: () -> Unit
) {
    var page by remember { mutableStateOf(SettingsPage.HOME) }
    var lastMenuTitle by rememberSaveable { mutableStateOf("Subscription") }
    val television = isTelevisionLayout()
    val entry = LocalTvFocusEntry.current
    val restoreTitle = remember(page, entry) { lastMenuTitle }
    TvPageEntry(if (page == SettingsPage.HOME) "settings:$restoreTitle" else "settings:back")
    val settingsEntry = remember(page, entry) { java.util.UUID.randomUUID().toString() }
    val defaultSettingsListState = rememberLazyListState()
    val settingsListState = if (television) remember(page) { LazyListState() } else defaultSettingsListState
    LaunchedEffect(page, entry) {
        if (television && page == SettingsPage.HOME) {
            val group = when (restoreTitle) {
                "Content sources", "Content & storage" -> 1
                "Player options", "Appearance & language", "Parental controls" -> 2
                "Updates", "Privacy & permissions" -> 3
                "About STB Play", "Share app" -> 4
                else -> 0
            }
            settingsListState.scrollToItem(group)
        }
    }
    val uriHandler = LocalUriHandler.current
    BackHandler(enabled = page != SettingsPage.HOME) { page = SettingsPage.HOME }
    CompositionLocalProvider(
        LocalTvFocusEntry provides if (television) settingsEntry else entry,
        LocalSettingsFocusTarget provides if (television && page == SettingsPage.HOME)
            SettingsFocusTarget(restoreTitle) { lastMenuTitle = it } else null) {
    androidx.compose.runtime.key(if (isTelevisionLayout()) page else Unit) {
    LazyColumn(
        state = settingsListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = if (isCompactAndroidLayout()) 16.dp else 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (page != SettingsPage.HOME) item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                QuestButton(
                    onClick = { page = SettingsPage.HOME },
                    modifier = Modifier.widthIn(min = 108.dp).height(46.dp).tvFocusTarget("settings:back").questInitialFocus()
                ) { Text("←  Back", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                Text(page.title, color = White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        when (page) {
            SettingsPage.HOME -> {
                item {
                    SettingsMenuGroup {
                        SettingsMenuRow(Icons.Filled.Info, "Subscription", state.subscriptionPlan, initialFocus = true) { page = SettingsPage.SUBSCRIPTION }
                    }
                }
                item {
                    SettingsMenuGroup {
                        SettingsMenuRow(Icons.Filled.Storage, "Content sources", "Manage portals") { page = SettingsPage.SOURCES }
                        SettingsMenuDivider()
                        SettingsMenuRow(Icons.Filled.Tv, "Content & storage", "Catalogue and history") { page = SettingsPage.CONTENT }
                    }
                }
                item {
                    SettingsMenuGroup {
                        SettingsMenuRow(Icons.Filled.PlayArrow, "Player options", state.playerPreference.displayName()) { page = SettingsPage.PLAYBACK }
                        SettingsMenuDivider()
                        SettingsMenuRow(Icons.Filled.Language, "Appearance & language", state.themePreference.displayName()) { page = SettingsPage.APPEARANCE }
                        SettingsMenuDivider()
                        SettingsMenuRow(Icons.Filled.Lock, "Parental controls", "Change PIN") { page = SettingsPage.PARENTAL }
                    }
                }
                item {
                    SettingsMenuGroup {
                        SettingsMenuRow(Icons.Filled.SystemUpdate, "Updates", state.updateAvailableVersion?.let { "Version $it available" }) { page = SettingsPage.UPDATES }
                        SettingsMenuDivider()
                        SettingsMenuRow(Icons.Filled.Lock, "Privacy & permissions", "How your data is used") { page = SettingsPage.PRIVACY }
                    }
                }
                item {
                    SettingsMenuGroup {
                        SettingsMenuRow(Icons.Filled.Info, "About STB Play") { page = SettingsPage.ABOUT }
                        SettingsMenuDivider()
                        SettingsMenuRow(Icons.Filled.Share, "Share app", showChevron = false, onClick = onShare)
                    }
                }
            }
            SettingsPage.SUBSCRIPTION -> item { SubscriptionCard(state, onActivateLicense) }
            SettingsPage.SOURCES -> item {
                SettingsSection("Content sources") {
                    if (state.profiles.isEmpty()) Text("No portals saved yet.", color = Muted, fontSize = 13.sp)
                    state.profiles.forEach { profile ->
                        PortalProfileRow(profile, profile.id == state.activeProfileId,
                            { onUsePortal(profile) }, { onEditPortal(profile) }, { onDeletePortal(profile) })
                    }
                    state.profiles.firstOrNull { it.id == state.activeProfileId }?.let { activeProfile ->
                        WideAction("Link active device with a provider", { onProviderPair(activeProfile) }, Modifier.fillMaxWidth())
                        Text("Your provider can assign a portal profile and manage this device after you share the pairing code.", color = Muted, fontSize = 12.sp)
                    }
                    WideAction("Add portal", onAddPortal)
                }
            }
            SettingsPage.PLAYBACK -> item {
                SettingsSection("Playback") {
                    PreferenceRow("Default player", state.playerPreference.displayName()) { onPlayerPreferenceChanged(state.playerPreference.next()) }
                    PreferenceRow("Audio & subtitles", state.subtitlePreference.displayName()) { onSubtitlePreferenceChanged(state.subtitlePreference.next()) }
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Android box video compatibility", color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Try this if H.265 video has a green or blue picture. It switches the video surface on Android TV/boxes.", color = Muted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = state.androidBoxVideoCompatibility,
                            onCheckedChange = onAndroidBoxVideoCompatibilityChanged
                        )
                    }
                    Text("Auto uses the internal player first and offers VLC when playback fails, if installed.", color = Muted, fontSize = 12.sp)
                }
            }
            SettingsPage.APPEARANCE -> item {
                SettingsSection("Theme & launcher icon") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemePreference.entries.forEach { theme ->
                            val selected = state.themePreference == theme
                            QuestSurface(
                                onClick = { onThemePreferenceChanged(theme) },
                                modifier = Modifier.weight(1f).heightIn(min = 94.dp)
                                    .border(if (selected) 2.dp else 0.dp, Gold, RoundedCornerShape(10.dp)),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                                colors = ClickableSurfaceDefaults.colors(containerColor = if (selected) Gold.copy(alpha = 0.18f) else Navy, focusedContainerColor = Gold.copy(alpha = 0.28f)),
                                border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Gold)))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Image(painterResource(theme.iconResource()), contentDescription = "${theme.displayName()} icon", modifier = Modifier.size(49.dp))
                                    Text(theme.shortName(), color = White, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    PreferenceRow("Catalogue language", state.catalogueLanguage) { onCatalogueLanguageChanged(nextLanguage(state.catalogueLanguage)) }
                }
            }
            SettingsPage.PARENTAL -> item {
                SettingsSection("Parental controls") {
                    Text("Adult categories re-lock when you leave them.", color = Muted, fontSize = 13.sp)
                    ParentalMode.entries.forEach { mode ->
                        PreferenceRow(mode.displayName(), if (state.parentalMode == mode) "Selected" else mode.description()) {
                            onParentalModeChanged(mode)
                        }
                    }
                    Text("Adult-only mode shows adult content on Home and applies its own dark theme. Provider ratings and labels determine which individual titles are recognized as adult.", color = Muted, fontSize = 12.sp)
                    PrimaryAction("Change PIN", onClick = onChangePin)
                }
            }
            SettingsPage.CONTENT -> item {
                SettingsSection("Content & storage") {
                    Text("${state.liveCount} live · ${state.movieCount} movies · ${state.seriesCount} series", color = White, fontSize = 14.sp)
                    Text("Last refresh: ${state.lastRefreshText}", color = Muted, fontSize = 12.sp)
                    WideAction("Refresh content", onRefresh, Modifier.fillMaxWidth())
                    WideAction("Clear local cache", onClearCache, Modifier.fillMaxWidth())
                    WideAction("Clear watch history", onClearHistory, Modifier.fillMaxWidth())
                }
            }
            SettingsPage.UPDATES -> item {
                SettingsSection("App updates") {
                    Text(state.updateText, color = if (state.updateAvailableVersion == null) Muted else Good, fontSize = 13.sp)
                    WideAction("Check for updates", onCheckUpdates, Modifier.fillMaxWidth())
                    if (state.updateAvailableVersion != null) PrimaryAction("Download & install", onClick = onDownloadUpdate)
                }
            }
            SettingsPage.PRIVACY -> item {
                SettingsSection("Privacy policy") {
                    Text("STB Play is a player only; it does not supply portals, accounts, subscriptions, channels, movies, series or streams. Add only a portal and content you are authorized to access. You are responsible for complying with local law and your provider’s terms.", color = White, fontSize = 13.sp)
                    Text("Portal profiles, portal address, MAC address, parental PIN, favourites, playback history/progress, cached catalogue data and preferences are stored in local app storage. They are not sent to STB Play for basic usage counts. Protect the device; local app storage is not an encrypted password vault.", color = White, fontSize = 13.sp)
                    Text("The app connects directly to your portal and to media/artwork hosts supplied by it. Those services receive network requests and may see your IP address and portal credentials required for playback. HTTP portals do not encrypt that connection. Their privacy practices apply. Casting uses Google Cast and the receiver you select; update checks and downloads use GitHub.", color = White, fontSize = 13.sp)
                    Text("If you activate a STB Play key, the app sends the key, Android device identifier, platform, app version and portal hostname over HTTPS for validation. The service stores hashed key/device references plus the portal hostname and activity details; it does not need your portal password, MAC address, full portal URL or viewing history.", color = White, fontSize = 13.sp)
                    Text("If you choose Link with provider, the app sends its Android device identifier and active portal MAC over HTTPS to request a short-lived pairing code. STB Play stores a hashed device reference and the MAC with the assignment. Your assigned provider and STB Play administrators can see the Device ID and MAC in their dashboards and use them to assign an authorized portal profile. If paired, the app syncs the portal profile on startup or when you refresh. This is separate from optional device counts.", color = White, fontSize = 13.sp)
                    Text("Optional device counts are off unless you enable them here. When enabled, the app sends its Android device identifier, platform and app version to the STB Play service; it does not send portal details or viewing history for this count. The service uses a hashed device reference and activity dates, with records marked to expire after 12 months without activity. Turn this off any time to stop future count requests.", color = White, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Share optional device counts", color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (state.analyticsEnabled) "On · tap to stop future count requests" else "Off · no count requests are sent", color = Muted, fontSize = 12.sp)
                        }
                        Switch(checked = state.analyticsEnabled, onCheckedChange = onAnalyticsChanged)
                    }
                    Text("Internet/network access connects to selected services. Notifications are used for updates and locally scheduled portal-expiry reminders if you allow them. The app requests permission to open an APK installer only when you choose an update. It does not request camera, microphone, contacts or location access.", color = White, fontSize = 13.sp)
                    Text("Use Content & storage to erase cached catalogues and watch history. Remove saved portals in Content sources. Uninstalling clears remaining local app data.", color = Muted, fontSize = 12.sp)
                    WideAction("Full privacy policy online", { uriHandler.openUri("https://github.com/ranveerskh/stbpplayvr/blob/test/2.0.2/PRIVACY_POLICY.md") }, Modifier.fillMaxWidth())
                }
            }
            SettingsPage.ABOUT -> item {
                SettingsSection("About") {
                    val context = LocalContext.current
                    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                    val deviceDescription = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                    val mode = if (isTelevisionLayout()) "TV" else if (
                        listOf(android.os.Build.MANUFACTURER, android.os.Build.BRAND, android.os.Build.MODEL)
                            .any { it.contains("oculus", true) || it.contains("meta", true) || it.contains("quest", true) }
                    ) "Meta" else "Phone"
                    PairingValue("App version / build", "${com.example.stbplay.BuildConfig.VERSION_NAME} (${com.example.stbplay.BuildConfig.VERSION_CODE})")
                    PairingValue("STB PLAY Device ID", state.deviceReference.ifBlank { "Unavailable" })
                    WideAction("Copy Device ID", {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(state.deviceReference))
                        android.widget.Toast.makeText(context, "Device ID copied", android.widget.Toast.LENGTH_SHORT).show()
                    }, Modifier.fillMaxWidth())
                    PairingValue("Portal MAC", state.portalMac.ifBlank { "No portal MAC configured" })
                    WideAction("Copy MAC", {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(state.portalMac))
                        android.widget.Toast.makeText(context, "MAC copied", android.widget.Toast.LENGTH_SHORT).show()
                    }, Modifier.fillMaxWidth())
                    PairingValue("Device", deviceDescription)
                    PairingValue("Android version", "${android.os.Build.VERSION.RELEASE} · API ${android.os.Build.VERSION.SDK_INT}")
                    PairingValue("App mode", mode)
                    Text("STB Play does not provide subscriptions, channels, movies or streams. Use only content sources you are authorized to access.", color = White, fontSize = 13.sp)
                    WideAction("Share STB Play", onShare, Modifier.fillMaxWidth())
                }
            }
        }
    }
    }
    }
}

@Composable
fun ProviderPairingDialog(
    session: ProviderPairingSession?,
    status: String,
    onDismiss: () -> Unit
) {
    if (session == null) return
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 300.dp, max = 540.dp),
            shape = RoundedCornerShape(18.dp),
            colors = SurfaceDefaults.colors(containerColor = Panel)
        ) {
            Column(
                Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Link this device with your provider", color = GoldLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Send this code and the two device details to your provider. The code expires at ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(session.expiresAtMillis))}.", color = White, fontSize = 13.sp)
                PairingValue("Pairing code", session.pairingCode)
                PairingValue("Device ID", session.deviceReference)
                PairingValue("Portal MAC", session.portalMac)
                Text(status, color = GoldLight, fontSize = 13.sp)
                Text("Your provider sees the Device ID and MAC in their panel after pairing. Portal setup stays manual unless your provider assigns a profile. Canceling revokes the code when online; otherwise it expires automatically.", color = Muted, fontSize = 12.sp)
                PrimaryAction("Cancel pairing", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun PairingValue(label: String, value: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Navy).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private enum class SettingsPage(val title: String) {
    HOME("Settings"), SUBSCRIPTION("Subscription"), SOURCES("Content sources"),
    PLAYBACK("Player options"), APPEARANCE("Appearance & language"), PARENTAL("Parental controls"),
    CONTENT("Content & storage"), UPDATES("Updates"), PRIVACY("Privacy & permissions"), ABOUT("About")
}

@Composable
private fun SettingsMenuGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().widthIn(max = 820.dp).clip(RoundedCornerShape(16.dp)).background(Panel)
        .border(1.dp, White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)).padding(5.dp), content = content)
}

@Composable
private fun SettingsMenuDivider() {
    Spacer(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(Muted.copy(alpha = 0.18f)))
}

@Composable
private fun SettingsMenuRow(icon: ImageVector, title: String, subtitle: String? = null, showChevron: Boolean = true,
                            initialFocus: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val target = LocalSettingsFocusTarget.current
    val requestInitial = target?.let { it.title == title } ?: initialFocus
    val navigation = LocalTvFocusNavigation.current
    QuestSurface(onClick = onClick, tvContainerColor = Color.Transparent, tvFocusedContainerColor = Gold,
        modifier = Modifier.tvFocusTarget("settings:$title").onPreviewKeyEvent { event ->
            if (event.key == Key.DirectionLeft && navigation != null) {
                if (event.type == KeyEventType.KeyDown) navigation.returnToRail()
                true
            } else false
        }.then(if (requestInitial) Modifier.questInitialFocus() else Modifier)
            .fillMaxWidth().height(64.dp).onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) target?.onFocused?.invoke(title)
            },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Gold),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, GoldLight)))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = if (focused) OnAccent else Muted, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (focused) OnAccent else White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                subtitle?.let { Text(it, color = if (focused) OnAccent else Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (showChevron) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = if (focused) OnAccent else Muted)
        }
    }
}

@Composable
private fun SubscriptionCard(state: StbPlaySettingsState, onActivateLicense: (String) -> Unit) {
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var activationKey by remember { mutableStateOf("") }
    var activationMessage by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSection("STB Play app access") {
            Text(state.licenseName, color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (state.licenseExpiryText.isNotBlank()) Text(state.licenseExpiryText, color = Muted, fontSize = 12.sp)
            Text(state.appAccessStatus, color = if (state.appAccessStatus == "ACTIVE") Good else GoldLight,
                fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("License: ${state.licenseStatus}", color = if (state.licenseBusy) GoldLight else Muted, fontSize = 12.sp)
            WideAction("Upgrade / enter activation key", {
                activationMessage = ""
                showUpgradeDialog = true
            }, Modifier.fillMaxWidth())
        }

        SettingsSection("Portal subscription") {
            Text(state.subscriptionPlan, color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            val details = listOf(state.subscriptionStatus, state.expiryText).filter { it.isNotBlank() }.joinToString(" · ")
            Text(details.ifBlank { "The portal has not supplied subscription details. Refresh to try again." }, color = Muted, fontSize = 12.sp)
            if (state.portalSubscriptionMessage.isNotBlank()) Text(state.portalSubscriptionMessage, color = Muted, fontSize = 12.sp)
            Text("Expiry reminders: 10 and 9 days before, then daily during the final 5 days.", color = Muted, fontSize = 12.sp)
        }
    }

    if (showUpgradeDialog) Dialog(onDismissRequest = { showUpgradeDialog = false }) {
        Surface(
            modifier = Modifier.widthIn(min = 320.dp, max = 520.dp),
            shape = RoundedCornerShape(18.dp),
            colors = SurfaceDefaults.colors(containerColor = Panel)
        ) {
            Column(
                Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Upgrade STB Play", color = GoldLight, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Enter a key generated by your STB Play website. An assigned provider plan takes precedence over the app trial.",
                    color = White, fontSize = 13.sp
                )
                RemoteTextField(
                    value = activationKey,
                    onValueChange = { activationKey = it.take(80); activationMessage = "" },
                    modifier = Modifier.fillMaxWidth().height(50.dp).questInitialFocus().background(Navy, RoundedCornerShape(9.dp))
                        .border(1.dp, Gold.copy(alpha = 0.65f), RoundedCornerShape(9.dp)).padding(horizontal = 13.dp, vertical = 15.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = White, fontSize = 14.sp),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (activationKey.isBlank()) Text("Activation key", color = Muted, fontSize = 14.sp)
                            inner()
                        }
                    }
                )
                if (activationMessage.isNotBlank()) Text(activationMessage, color = GoldLight, fontSize = 12.sp)
                Text("License status: ${state.licenseStatus}", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryAction(if (state.licenseBusy) "Checking…" else "Activate key") {
                        if (state.licenseBusy) return@PrimaryAction
                        if (activationKey.isBlank()) activationMessage = "Enter your activation key first."
                        else {
                            activationMessage = ""
                            onActivateLicense(activationKey)
                        }
                    }
                    WideAction("Close", { showUpgradeDialog = false })
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel)
            .border(1.dp, White.copy(alpha = 0.06f), RoundedCornerShape(15.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Text(title, color = GoldLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun PortalProfileRow(profile: PortalSettings, active: Boolean, onUse: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(if (active) Gold.copy(alpha = 0.13f) else Navy).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (active) "Active portal" else "Available portal", color = if (active) GoldLight else Muted, fontSize = 11.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!active) WideAction("Use", onUse, Modifier.weight(1f))
            WideAction("Edit", onEdit, Modifier.weight(1f))
            if (!active) WideAction("Delete", onDelete, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PreferenceRow(title: String, value: String, onClick: () -> Unit) {
    QuestSurface(
        onClick = onClick, tvContainerColor = Navy, tvFocusedContainerColor = Gold.copy(alpha = 0.18f),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Navy, focusedContainerColor = Gold.copy(alpha = 0.18f)),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Gold)))
    ) {
        if (isCompactAndroidLayout()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(value, color = GoldLight, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        } else Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(value, color = GoldLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WideAction(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    QuestSurface(
        onClick = onClick, tvContainerColor = PanelSoft, tvFocusedContainerColor = Gold,
        modifier = modifier.height(38.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = PanelSoft, focusedContainerColor = Gold),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, GoldLight)))
    ) { Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text(title, color = if (focused) OnAccent else White, fontSize = 12.sp) } }
}

@Composable
private fun PrimaryAction(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    QuestButton(onClick = onClick, modifier = modifier, colors = ButtonDefaults.colors(containerColor = Gold, contentColor = OnAccent, focusedContainerColor = GoldLight, focusedContentColor = OnAccent)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StbPlaySearchScreen(
    catalog: List<PortalStream>,
    searchRemote: suspend (String, Int) -> VodCatalogBatch,
    onSearchResults: (List<PortalStream>) -> Unit,
    toUi: (PortalStream) -> UiMedia,
    scope: StbPlayTab,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var query by remember { mutableStateOf("") }
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { requester.requestFocus() }
    var indexed by remember { mutableStateOf<List<IndexedMedia>>(emptyList()) }
    var results by remember { mutableStateOf<List<PortalStream>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var remotePage by remember { mutableIntStateOf(1) }
    var remoteHasMore by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query) { remotePage = 1; remoteHasMore = false; results = emptyList(); searchError = null }
    LaunchedEffect(catalog) {
        if (scope == StbPlayTab.CONTENT) { indexed = emptyList(); return@LaunchedEffect }
        indexed = withContext(Dispatchers.Default) {
            val uniqueCatalog = catalog.distinctBy { "${it.streamType}:${it.id}" }
            uniqueCatalog.mapIndexed { index, media ->
                if ((index and 255) == 0) currentCoroutineContext().ensureActive()
                IndexedMedia(index, media, normalizeSearchText(media.name),
                    normalizeSearchText(media.originalTitle.orEmpty()),
                    normalizeSearchText(listOfNotNull(media.description, media.searchText, media.language,
                        media.genre, media.rating, media.cast, media.year?.toString()).joinToString(" ")))
            }
        }
    }
    LaunchedEffect(query, indexed, remotePage, scope) {
        val normalized = normalizeSearchText(query)
        if (normalized.length < 2) { results = emptyList(); searching = false; return@LaunchedEffect }
        searching = true
        delay(if (scope == StbPlayTab.CONTENT) 350 else 160)
        if (scope == StbPlayTab.CONTENT) {
            runCatching { searchRemote(query, remotePage) }
                .onSuccess { batch ->
                    val merged = if (remotePage == 1) batch.items else (results + batch.items)
                        .distinctBy { "${it.streamType}:${it.id}" }
                    val terms = normalized.split(' ').filter(String::isNotBlank)
                    results = withContext(Dispatchers.Default) {
                        merged.mapIndexed { index, media ->
                            val indexedMedia = IndexedMedia(index, media, normalizeSearchText(media.name),
                                normalizeSearchText(media.originalTitle.orEmpty()), "")
                            Triple(indexedMedia.matchRank(normalized, terms) ?: 9, index, media)
                        }.sortedWith(compareBy<Triple<Int, Int, PortalStream>> { it.first }.thenBy { it.second })
                            .map { it.third }
                    }
                    remoteHasMore = batch.hasMore
                    onSearchResults(results)
                    searchError = null
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    searchError = "Portal search unavailable. Try again."
                }
            searching = false
            return@LaunchedEffect
        }
        val tokens = normalized.split(' ').filter(String::isNotBlank)
        results = withContext(Dispatchers.Default) {
            topSearchMatches(indexed, normalized, tokens, limit = 200)
        }
        searching = false
    }
    val scopeTitle = when (scope) {
        StbPlayTab.LIVE -> "Live TV"
        StbPlayTab.CONTENT -> "Movies & Series"
        StbPlayTab.FAVOURITES -> "Favourites"
        else -> "All content"
    }
    val resultType = when (scope) {
        StbPlayTab.LIVE -> "channels"
        StbPlayTab.FAVOURITES -> "saved items"
        else -> "titles"
    }
    Column(modifier = Modifier.fillMaxSize().background(Navy).padding(if (isCompactAndroidLayout()) 14.dp else 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Search $scopeTitle", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Results stay within this section", color = Muted, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            HeaderAction("Back", onBack)
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp)
                .background(Panel, RoundedCornerShape(12.dp))
                .border(2.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                modifier = Modifier.weight(1f).fillMaxHeight().focusRequester(requester)
                    .padding(horizontal = 18.dp, vertical = 17.dp),
                singleLine = true,
                textStyle = TextStyle(color = White, fontSize = 18.sp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) Text("Type at least 2 characters…", color = Muted, fontSize = 17.sp)
                        inner()
                    }
                }
            )
            if (query.isNotBlank()) {
                var clearFocused by remember { mutableStateOf(false) }
                QuestSurface(
                    onClick = { query = "" },
                    modifier = Modifier.padding(end = 8.dp).size(40.dp).onFocusChanged { clearFocused = it.isFocused },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
                    colors = ClickableSurfaceDefaults.colors(containerColor = PanelSoft, focusedContainerColor = Gold),
                    border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.dp, GoldLight)))
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = if (clearFocused) OnAccent else White, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
        if (query.trim().length >= 2) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (scope == StbPlayTab.CONTENT) "${results.size} matching $resultType" else "${if (results.size == 200) "First " else ""}${results.size} matching $resultType · ${catalog.size} loaded", color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (remoteHasMore && scope == StbPlayTab.CONTENT) WideAction(if (searching) "Loading…" else "More results", { if (!searching) remotePage++ }, Modifier.width(210.dp))
            }
        }
        when {
            query.trim().length < 2 -> Text("Enter 2 or more characters to search. Title matches appear first.", color = Muted, fontSize = 14.sp)
            searching -> Text("Searching…", color = Muted, fontSize = 14.sp)
            results.isEmpty() -> EmptyState(
                "No matching $resultType",
                if (searchError != null) searchError!!
                else "Try a shorter part of the channel or title name."
            )
            else -> BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val columns = (maxWidth / 130.dp).toInt().coerceIn(4, 6)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    gridItems(results, key = { "${it.streamType}:${it.id}" },
                        contentType = { if (it.streamType == "live") "channel" else "title" }) { stream ->
                        val media = toUi(stream)
                        MediaCard(media, { onMediaClick(media) }, { onToggleFavorite(media) }, compactGrid = true)
                    }
                }
            }
        }
    }
}

private data class IndexedMedia(val index: Int, val media: PortalStream, val titleText: String,
    val alternateTitle: String, val metadata: String)

private data class RankedSearchResult(val rank: Int, val index: Int, val media: PortalStream)

/** Keeps only the best 200 local matches, avoiding a full-catalogue sort on every keystroke. */
private suspend fun topSearchMatches(
    indexed: List<IndexedMedia>,
    query: String,
    tokens: List<String>,
    limit: Int
): List<PortalStream> {
    val resultOrder = compareBy<RankedSearchResult> { it.rank }.thenBy { it.index }
    val worstFirst = Comparator<RankedSearchResult> { first, second -> resultOrder.compare(second, first) }
    val best = PriorityQueue(limit.coerceAtLeast(1), worstFirst)
    indexed.forEachIndexed { position, item ->
        if ((position and 255) == 0) currentCoroutineContext().ensureActive()
        val rank = item.matchRank(query, tokens) ?: return@forEachIndexed
        val candidate = RankedSearchResult(rank, item.index, item.media)
        if (best.size < limit) best.add(candidate)
        else if (resultOrder.compare(candidate, best.peek()) < 0) {
            best.poll()
            best.add(candidate)
        }
    }
    return best.toList().sortedWith(resultOrder).map { it.media }
}

private fun IndexedMedia.matchRank(query: String, tokens: List<String>): Int? {
    when {
        titleText == query -> return 0
        titleText.startsWith(query) -> return 1
        titleText.contains(query) -> return 2
        tokens.all(titleText::contains) -> return 3
        alternateTitle == query -> return 4
        alternateTitle.startsWith(query) -> return 5
        alternateTitle.contains(query) -> return 6
        tokens.all(alternateTitle::contains) -> return 7
    }

    return if (tokens.all(metadata::contains)) 8 else null
}

private val searchPunctuation = Regex("[^\\p{L}\\p{N}\\p{M}]+")
private val searchWhitespace = Regex("\\s+")
private fun normalizeSearchText(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT)
    .replace(searchPunctuation, " ")
    .trim()
    .replace(searchWhitespace, " ")

@Composable
fun FirstStartDisclaimer(onAccept: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    val policyScrollState = rememberScrollState()
    val policyFocusRequester = remember { FocusRequester() }
    var policyFocused by remember { mutableStateOf(false) }
    val television = isTelevisionLayout()
    val scope = rememberCoroutineScope()
    LaunchedEffect(policyFocusRequester) {
        withFrameNanos { }
        policyFocusRequester.requestFocus()
    }
    Box(
        Modifier.fillMaxSize().background(Navy).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth().fillMaxHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = Panel)
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Privacy and authorized use", color = GoldLight, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Review these terms before continuing. You can revisit the privacy controls in Settings.", color = Muted, fontSize = 13.sp)
                Column(
                    Modifier.weight(1f).fillMaxWidth()
                        .then(if (television) Modifier.border(if (policyFocused) 2.dp else 0.dp,
                            if (policyFocused) Gold else Color.Transparent, RoundedCornerShape(8.dp)).padding(8.dp) else Modifier)
                        .verticalScroll(policyScrollState)
                        .focusRequester(policyFocusRequester).onFocusChanged { policyFocused = it.isFocused }.focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> if (policyScrollState.canScrollForward) {
                                    scope.launch {
                                        policyScrollState.animateScrollTo((policyScrollState.value + 240).coerceAtMost(policyScrollState.maxValue))
                                    }
                                    true
                                } else false
                                Key.DirectionUp -> if (policyScrollState.canScrollBackward) {
                                    scope.launch {
                                        policyScrollState.animateScrollTo((policyScrollState.value - 240).coerceAtLeast(0))
                                    }
                                    true
                                } else false
                                else -> false
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PolicySection("1. STB Play and your content sources", "STB Play is a media player. It does not provide portals, accounts, subscriptions, playlists, channels, movies, series, stream links or credentials. Add only a portal/server you own or are authorized by its operator to use, and play only content you have the right to access. You are responsible for the source, its credentials, and following applicable laws, copyright rules and provider terms. Do not use the app to bypass access controls or redistribute content without permission.")
                    PolicySection("2. Ratings and parental controls", "Categories, age ratings and labels come from your provider and may be missing or inaccurate. PIN locks help restrict access but cannot guarantee that all unsuitable titles will be identified. Keep your device and PIN secure, and supervise children’s use.")
                    PolicySection("3. Data saved on this device", "The app saves portal profiles and settings, portal address, MAC address, parental PIN, favourites, watch history and progress, catalogue cache and preferences in local app storage. This is not an encrypted password vault. Remove saved portals or clear history/cache in Settings, or uninstall the app to remove local app data. Android backup is disabled.")
                    PolicySection("4. Connections to portals and other services", "The app contacts the portal you add to sign in, load its catalogue, and request playback links; it then contacts stream and artwork hosts supplied by that source. Those services can receive your network address and requests, and the portal receives the credentials needed to authenticate you. A portal using HTTP does not encrypt that traffic. The source operator’s own privacy and service terms apply. If you use Cast, Google Cast and your selected receiver participate in playback. Update checks and downloads use GitHub.")
                    PolicySection("5. STB Play key and license service", "If you activate a STB Play key, the app sends the entered key, Android device identifier, app platform/version and portal hostname over HTTPS for license verification. The service keeps hashed key and device references together with the portal hostname and registration/activity details. It does not need your portal password, MAC address, full portal URL or watched titles for license checks. The license service and its cloud host process these requests under their own security and retention settings.")
                    PolicySection("6. Optional provider pairing", "Only if you choose Link with provider, the app sends its Android device identifier, platform and active portal MAC over HTTPS to request a 10-minute pairing code. The service stores a hashed device reference and the MAC with the pairing and assignment. The assigned provider and STB Play administrators can view both values in their authorized dashboards and use them to assign an authorized portal profile and app license. After pairing, the app sends its device identifier, pairing token, platform and app version on startup or when you refresh to receive provider profile updates. Pairing data is not used for advertising or optional device counts. You can cancel the request; the app attempts to revoke the code, which expires after 10 minutes if revocation cannot reach the service.")
                    PolicySection("7. Optional device counts", "Device-count sharing is optional and off by default for new installs. If enabled in Settings, the app sends its Android device identifier, platform and app version to the STB Play service to estimate installs and recently active devices. The request excludes portal details and viewing history. Device references are hashed by the service; count records are marked to expire after 12 months without activity. You can turn this option off at any time in Settings. No advertising SDK is included.")
                    PolicySection("8. Notifications and permissions", "Internet and network access are used for your selected portal, streams, Cast, updates and license service. Notifications may announce an update or a local portal-expiry reminder if you grant permission. Android asks separately before installing an update you choose. STB Play does not request camera, microphone, contacts or location access.")
                    PolicySection("9. Changes and third parties", "Portal operators, media hosts, Google, Firebase and GitHub are separate services and handle their own connection data under their policies. STB Play cannot control their availability or practices. The full privacy policy is published in Settings and in the app’s policy document; a revised policy may be shown again when its version changes.")
                }
                QuestButton(
                    onClick = { agreed = !agreed },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    colors = ButtonDefaults.colors(containerColor = Navy, contentColor = White, focusedContainerColor = if (television) Gold else Navy, focusedContentColor = if (television) OnAccent else White)
                ) {
                    Text(if (agreed) "☑  I agree to the privacy policy and authorized-use terms" else "□  I agree to the privacy policy and authorized-use terms", fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                QuestButton(
                    onClick = onAccept,
                    enabled = agreed,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.colors(containerColor = Gold, contentColor = OnAccent, focusedContainerColor = GoldLight, focusedContentColor = OnAccent)
                ) { Text("Agree and continue", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(body, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
fun PinPrompt(title: String, expectedPin: String, onVerified: () -> Unit, onCancel: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    BackHandler(onBack = onCancel)
    Box(Modifier.fillMaxSize().background(Color(0xD9070707)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(min = 370.dp, max = 520.dp),
            shape = RoundedCornerShape(18.dp),
            colors = SurfaceDefaults.colors(containerColor = Panel)
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Protected content", color = GoldLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Enter your parental PIN to open $title", color = White, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
                Text(
                    text = "•".repeat(pin.length).ifEmpty { "Enter PIN" },
                    color = if (pin.isEmpty()) Muted else White,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(220.dp).height(52.dp)
                        .background(Navy, RoundedCornerShape(10.dp))
                        .border(2.dp, if (error) Danger else Gold, RoundedCornerShape(10.dp))
                        .padding(vertical = 13.dp)
                )
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "Clear")).forEachIndexed { rowIndex, keys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        keys.forEachIndexed { columnIndex, key ->
                            QuestButton(
                                onClick = {
                                    pin = when (key) {
                                        "⌫" -> pin.dropLast(1)
                                        "Clear" -> ""
                                        else -> (pin + key).take(8)
                                    }
                                    error = false
                                },
                                modifier = Modifier.width(86.dp).height(46.dp)
                                    .then(if (rowIndex == 0 && columnIndex == 0) Modifier.questInitialFocus() else Modifier),
                                colors = ButtonDefaults.colors(containerColor = Navy, contentColor = White, focusedContainerColor = Gold, focusedContentColor = OnAccent)
                            ) { Text(key, fontSize = 16.sp) }
                        }
                    }
                }
                if (error) Text("Incorrect PIN", color = Danger, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryAction("Unlock") { if (expectedPin.isNotBlank() && pin == expectedPin) onVerified() else error = true }
                    WideAction("Cancel", onCancel)
                }
            }
        }
    }
}

@Composable
fun ProviderPinSetupPrompt(onSave: (String) -> Unit, onCancel: () -> Unit) {
    var next by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val newPinFocus = remember { FocusRequester() }
    val confirmPinFocus = remember { FocusRequester() }
    val savePinFocus = remember { FocusRequester() }
    BackHandler(onBack = onCancel)
    Box(Modifier.fillMaxSize().background(Color(0xD9070707)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 520.dp),
            shape = RoundedCornerShape(18.dp),
            colors = SurfaceDefaults.colors(containerColor = Panel)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Set parental PIN", color = GoldLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Create a 4–8 digit PIN to protect parental settings and restricted content.",
                    color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center
                )
                PinField("New PIN", next, initialFocus = true, focusRequester = newPinFocus,
                    nextFocusRequester = confirmPinFocus, imeAction = ImeAction.Next) { next = it; error = "" }
                PinField("Confirm PIN", confirmation, focusRequester = confirmPinFocus,
                    previousFocusRequester = newPinFocus, nextFocusRequester = savePinFocus,
                    nextIsAction = true) { confirmation = it; error = "" }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryAction("Save PIN", Modifier.focusRequester(savePinFocus).focusProperties { up = confirmPinFocus }) {
                        error = when {
                            next.length !in 4..8 -> "PIN must have 4 to 8 digits."
                            confirmation != next -> "The PIN entries do not match."
                            else -> { onSave(next); "" }
                        }
                    }
                    WideAction("Cancel", onCancel)
                }
            }
        }
    }
}

@Composable
fun ChangePinPrompt(expectedPin: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val currentPinFocus = remember { FocusRequester() }
    val newPinFocus = remember { FocusRequester() }
    val savePinFocus = remember { FocusRequester() }
    BackHandler(onBack = onCancel)
    Box(Modifier.fillMaxSize().background(Color(0xD9070707)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(min = 400.dp, max = 560.dp),
            shape = RoundedCornerShape(18.dp),
            colors = SurfaceDefaults.colors(containerColor = Panel)
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("Change parental PIN", color = GoldLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                PinField("Current PIN", current, initialFocus = true, focusRequester = currentPinFocus,
                    nextFocusRequester = newPinFocus, imeAction = ImeAction.Next) { current = it; error = "" }
                PinField("New PIN", next, focusRequester = newPinFocus, previousFocusRequester = currentPinFocus,
                    nextFocusRequester = savePinFocus, nextIsAction = true) { next = it; error = "" }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryAction("Update", Modifier.focusRequester(savePinFocus).focusProperties { up = newPinFocus }) {
                        error = when {
                            current != expectedPin -> "Current PIN is incorrect."
                            next.length !in 4..8 -> "New PIN must have 4 to 8 digits."
                            else -> { onSave(next); "" }
                        }
                    }
                    WideAction("Cancel", onCancel)
                }
            }
        }
    }
}

@Composable
private fun PinField(
    label: String, value: String, initialFocus: Boolean = false,
    focusRequester: FocusRequester? = null,
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    nextIsAction: Boolean = false,
    onChange: (String) -> Unit
) {
    val television = isTelevisionLayout()
    val keyboard = LocalSoftwareKeyboardController.current
    fun focusNext() {
        if (nextIsAction) keyboard?.hide()
        nextFocusRequester?.requestFocus()
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        RemoteTextField(
            value = value,
            onValueChange = { onChange(it.filter(Char::isDigit).take(8)) },
            modifier = Modifier.then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .then(if (initialFocus) Modifier.questInitialFocus() else Modifier)
                .focusProperties {
                    nextFocusRequester?.let { next = it; down = it }
                    previousFocusRequester?.let { previous = it; up = it }
                }
                .fillMaxWidth().height(48.dp).background(Navy, RoundedCornerShape(10.dp)).border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
            keyboardActions = KeyboardActions(onNext = { focusNext() }, onDone = { focusNext(); keyboard?.hide() }),
            textStyle = TextStyle(color = White, fontSize = 16.sp)
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 4.dp))
}

@Composable
private fun LoadingContent(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("STB PLAY", color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Box(Modifier.fillMaxSize().padding(26.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(message, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EmptyInline(title: String, message: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(message, color = Muted, fontSize = 12.sp)
    }
}

private fun PlayerPreference.next(): PlayerPreference = when (this) {
    PlayerPreference.AUTO -> PlayerPreference.INTERNAL
    PlayerPreference.INTERNAL -> PlayerPreference.VLC
    PlayerPreference.VLC -> PlayerPreference.AUTO
}

private fun PlayerPreference.displayName(): String = when (this) {
    PlayerPreference.AUTO -> "Auto"
    PlayerPreference.INTERNAL -> "Internal player"
    PlayerPreference.VLC -> "VLC"
}

private fun ThemePreference.next(): ThemePreference = when (this) {
    ThemePreference.BLUE -> ThemePreference.LIGHT
    ThemePreference.LIGHT -> ThemePreference.BLACK
    ThemePreference.BLACK -> ThemePreference.BLUE
}

private fun ThemePreference.displayName(): String = when (this) {
    ThemePreference.BLUE -> "Blue & gold"
    ThemePreference.LIGHT -> "Ivory & gold"
    ThemePreference.BLACK -> "Black & silver"
}

private fun ThemePreference.shortName(): String = when (this) {
    ThemePreference.BLUE -> "Blue"
    ThemePreference.LIGHT -> "Ivory"
    ThemePreference.BLACK -> "Black"
}

private fun ThemePreference.iconResource(): Int = when (this) {
    ThemePreference.BLUE -> R.drawable.icon_blue
    ThemePreference.LIGHT -> R.drawable.icon_light
    ThemePreference.BLACK -> R.drawable.icon_black
}

private fun ParentalMode.displayName(): String = when (this) {
    ParentalMode.ALL_CONTENT -> "All content"
    ParentalMode.HIDE_ADULT -> "Hide adult content"
    ParentalMode.ADULT_ONLY -> "Adult only"
}

private fun ParentalMode.description(): String = when (this) {
    ParentalMode.ALL_CONTENT -> "Adult categories ask for your PIN"
    ParentalMode.HIDE_ADULT -> "Hide adult categories and flagged titles"
    ParentalMode.ADULT_ONLY -> "Show only adult categories and flagged titles"
}

private fun SubtitlePreference.next(): SubtitlePreference = when (this) {
    SubtitlePreference.AUTO -> SubtitlePreference.OFF
    SubtitlePreference.OFF -> SubtitlePreference.ENGLISH
    SubtitlePreference.ENGLISH -> SubtitlePreference.HINDI
    SubtitlePreference.HINDI -> SubtitlePreference.PUNJABI
    SubtitlePreference.PUNJABI -> SubtitlePreference.AUTO
}

private fun SubtitlePreference.displayName(): String = when (this) {
    SubtitlePreference.AUTO -> "Auto (provider tracks)"
    SubtitlePreference.OFF -> "Off"
    SubtitlePreference.ENGLISH -> "English"
    SubtitlePreference.HINDI -> "Hindi"
    SubtitlePreference.PUNJABI -> "Punjabi"
}

private fun nextLanguage(value: String): String = when (value) {
    "All" -> "English"
    "English" -> "Hindi"
    "Hindi" -> "Punjabi"
    "Punjabi" -> "All"
    else -> "All"
}
