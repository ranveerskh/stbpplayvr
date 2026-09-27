@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.example.stbplay.data.PlayerPreference
import com.example.stbplay.data.SubtitlePreference
import com.example.stbplay.data.ThemePreference
import com.example.stbplay.domain.model.PortalSettings
import kotlinx.coroutines.delay

private val Navy = Color(0xFF070707)
private val Rail = Color(0xFF111111)
private val Panel = Color(0xFF181818)
private val PanelSoft = Color(0xFF222222)
private val Gold = Color(0xFFDDB32F)
private val GoldLight = Color(0xFFFFD966)
private val White = Color(0xFFF4F6FA)
private val Muted = Color(0xFF9AA8B8)
private val Danger = Color(0xFFFFA4A4)
private val Good = Color(0xFF87E7B0)

/** Gives a newly opened TV screen a focused control for Quest/gamepad input. */
@Composable
fun Modifier.questInitialFocus(): Modifier {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) {
        requester.requestFocus()
    }
    return then(focusRequester(requester))
}

enum class StbPlayTab { HOME, LIVE, CONTENT, FAVOURITES, SETTINGS }
enum class ContentKindFilter { ALL, MOVIES, SERIES }

data class UiCategory(
    val id: String,
    val title: String,
    val isLocked: Boolean = false
)

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
    val expiryText: String? = null
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
    val liveCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val playerPreference: PlayerPreference = PlayerPreference.AUTO,
    val themePreference: ThemePreference = ThemePreference.DARK,
    val subtitlePreference: SubtitlePreference = SubtitlePreference.AUTO,
    val catalogueLanguage: String = "All",
    val analyticsEnabled: Boolean = true,
    val lastRefreshText: String = "Not refreshed yet",
    val updateText: String = "Check whether a newer STB Play version is available.",
    val updateAvailableVersion: String? = null
)

@Composable
fun StbPlayApp(
    homeState: StbPlayHomeState,
    liveState: StbPlayLibraryState,
    contentState: StbPlayLibraryState,
    favouritesState: StbPlayLibraryState,
    settingsState: StbPlaySettingsState,
    selectedTab: StbPlayTab,
    contentFilter: ContentKindFilter,
    onTabSelected: (StbPlayTab) -> Unit,
    onContentFilterChanged: (ContentKindFilter) -> Unit,
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
    onPlayerPreferenceChanged: (PlayerPreference) -> Unit,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    onSubtitlePreferenceChanged: (SubtitlePreference) -> Unit,
    onCatalogueLanguageChanged: (String) -> Unit,
    onAnalyticsChanged: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onShare: () -> Unit,
    searchCatalog: List<UiMedia>
) {
    var searchOpen by remember { mutableStateOf(false) }
    var railCollapsed by remember { mutableStateOf(false) }

    if (searchOpen) {
        StbPlaySearchScreen(
            catalog = searchCatalog,
            onMediaClick = { media -> searchOpen = false; onMediaClick(media) },
            onToggleFavorite = onToggleFavorite,
            onBack = { searchOpen = false }
        )
        return
    }

    Row(modifier = Modifier.fillMaxSize().background(Navy)) {
        StbPlayNavigationRail(
            selectedTab = selectedTab,
            collapsed = railCollapsed,
            onToggle = { railCollapsed = !railCollapsed },
            onTabSelected = onTabSelected
        )
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            StbPlayHeader(
                selectedTab = selectedTab,
                onSearchClick = { searchOpen = true },
                onSettingsClick = { onTabSelected(StbPlayTab.SETTINGS) }
            )
            when (selectedTab) {
                StbPlayTab.HOME -> StbPlayHomeScreen(
                    state = homeState,
                    onMediaClick = onMediaClick,
                    onToggleFavorite = onToggleFavorite,
                    onRemoveHistory = onRemoveHistory
                )
                StbPlayTab.LIVE -> LiveTvScreen(
                    state = liveState,
                    onCategorySelected = { onCategorySelected(StbPlayTab.LIVE, it) },
                    onMediaClick = onMediaClick,
                    onToggleFavorite = onToggleFavorite
                )
                StbPlayTab.CONTENT -> ContentBrowserScreen(
                    state = contentState,
                    selectedFilter = contentFilter,
                    onFilterChanged = onContentFilterChanged,
                    onLoadMore = onLoadMoreContent,
                    onCategorySelected = { onCategorySelected(StbPlayTab.CONTENT, it) },
                    onMediaClick = onMediaClick,
                    onToggleFavorite = onToggleFavorite
                )
                StbPlayTab.FAVOURITES -> FavouritesScreen(
                    state = favouritesState,
                    onMediaClick = onMediaClick,
                    onToggleFavorite = onToggleFavorite
                )
                StbPlayTab.SETTINGS -> StbPlaySettingsScreen(
                    state = settingsState,
                    onRefresh = onRefresh,
                    onClearCache = onClearCache,
                    onClearHistory = onClearHistory,
                    onAddPortal = onAddPortal,
                    onEditPortal = onEditPortal,
                    onUsePortal = onUsePortal,
                    onDeletePortal = onDeletePortal,
                    onPlayerPreferenceChanged = onPlayerPreferenceChanged,
                    onThemePreferenceChanged = onThemePreferenceChanged,
                    onSubtitlePreferenceChanged = onSubtitlePreferenceChanged,
                    onCatalogueLanguageChanged = onCatalogueLanguageChanged,
                    onAnalyticsChanged = onAnalyticsChanged,
                    onChangePin = onChangePin,
                    onCheckUpdates = onCheckUpdates,
                    onDownloadUpdate = onDownloadUpdate,
                    onShare = onShare
                )
            }
        }
    }
}

@Composable
private fun StbPlayNavigationRail(
    selectedTab: StbPlayTab,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onTabSelected: (StbPlayTab) -> Unit
) {
    val tabs = listOf(
        StbPlayTab.HOME to "Home",
        StbPlayTab.LIVE to "Live TV",
        StbPlayTab.CONTENT to "Movies & Series",
        StbPlayTab.FAVOURITES to "Favourites",
        StbPlayTab.SETTINGS to "Settings"
    )
    Column(
        modifier = Modifier
            .width(if (collapsed) 78.dp else 198.dp)
            .fillMaxHeight()
            .background(Rail)
            .padding(horizontal = 12.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuestSurface(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Gold)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (collapsed) "STB" else "STB PLAY", color = if (collapsed) GoldLight else Gold, fontSize = if (collapsed) 12.sp else 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Text(
            if (collapsed) "" else "Premium OTT Experience",
            color = Muted,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        tabs.forEach { (tab, title) ->
            NavItem(
                label = if (collapsed) title.take(1) else title,
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (!collapsed) {
            Text("Android TV", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 10.dp, bottom = 4.dp))
        }
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val initialFocus = if (selected) Modifier.questInitialFocus() else Modifier
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.then(initialFocus).fillMaxWidth().height(48.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent,
            focusedContainerColor = Gold
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, GoldLight))
        )
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                label,
                color = if (focused) Navy else White,
                fontSize = 13.sp,
                fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StbPlayHeader(
    selectedTab: StbPlayTab,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 30.dp),
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
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("STB PLAY", color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
        HeaderAction("Search", onSearchClick)
        Spacer(modifier = Modifier.width(10.dp))
        HeaderAction("Settings", onSettingsClick)
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.height(40.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Gold),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, GoldLight)))
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 15.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (focused) Navy else White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StbPlayHomeScreen(
    state: StbPlayHomeState,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onRemoveHistory: (UiMedia) -> Unit
) {
    if (state.loading) {
        LoadingContent("Loading your portal…")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 30.dp, end = 30.dp, bottom = 42.dp),
        verticalArrangement = Arrangement.spacedBy(25.dp)
    ) {
        item {
            if (state.heroes.isEmpty()) EmptyState("No content found", "Connect a portal with an active catalogue to start watching.")
            else RotatingHero(state.heroes, onMediaClick, onToggleFavorite)
        }
        state.expiryText?.takeIf { it.isNotBlank() }?.let { expiry ->
            item { StatusPill(expiry, GoldLight) }
        }
        columnItems(state.rows, key = { it.id }) { row ->
            if (row.items.isNotEmpty()) MediaRow(row, onMediaClick, onToggleFavorite, onRemoveHistory)
        }
    }
}

@Composable
private fun RotatingHero(
    heroes: List<UiMedia>,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit
) {
    var index by remember(heroes.map { it.id }) { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(heroes.map { it.id }, focused) {
        if (!focused && heroes.size > 1) {
            while (true) {
                delay(8_000)
                index = (index + 1) % heroes.size
            }
        }
    }
    val item = heroes.getOrNull(index.coerceIn(0, (heroes.size - 1).coerceAtLeast(0))) ?: return
    val scale by animateFloatAsState(if (focused) 1.01f else 1f, label = "heroScale")
    QuestSurface(
        onClick = { onMediaClick(item) },
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
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
                fallbackText = null
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color(0xF8070707), Color(0xB8070707), Color.Transparent))
                )
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xE8070707)))
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(28.dp).widthIn(max = 590.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(item.badge ?: item.streamType.uppercase(), color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(item.title, color = White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color(0xFFD3DBE6), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    QuestButton(
                        onClick = { onMediaClick(item) },
                        colors = ButtonDefaults.colors(containerColor = Gold, contentColor = Navy, focusedContainerColor = GoldLight, focusedContentColor = Navy)
                    ) { Text("Play now", fontWeight = FontWeight.Bold) }
                    HeaderAction(if (item.isFavorite) "Saved" else "Add to favourites") { onToggleFavorite(item) }
                }
            }
            if (heroes.size > 1) {
                Text("${index + 1} / ${heroes.size}", color = White, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd).padding(18.dp))
            }
        }
    }
}

@Composable
private fun MediaRow(
    row: UiMediaRow,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onRemoveHistory: (UiMedia) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(row.title, color = White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            row.subtitle?.let { Text(it, color = Muted, fontSize = 12.sp) }
        }
        LazyRow(
            modifier = Modifier.focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            contentPadding = PaddingValues(end = 22.dp)
        ) {
            columnItems(row.items, key = { it.id }) { media ->
                MediaCard(
                    item = media,
                    onClick = { onMediaClick(media) },
                    onToggleFavorite = { onToggleFavorite(media) },
                    onRemoveHistory = if (row.id == "continue") ({ onRemoveHistory(media) }) else null
                )
            }
        }
    }
}

@Composable
private fun LiveTvScreen(
    state: StbPlayLibraryState,
    onCategorySelected: (Int) -> Unit,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 30.dp, bottom = 28.dp)) {
        CategorySidebar(state.categories, state.selectedCategory, onCategorySelected)
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Live TV", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Choose a channel from your provider", color = Muted, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(state.totalItemsText.ifBlank { "${state.items.size} channels" }, color = Muted, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(17.dp))
            when {
                state.loading -> LoadingContent("Loading channels…")
                state.items.isEmpty() -> EmptyState("No channels in this category", state.emptyMessage)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    columnItems(state.items, key = { it.id }) { channel ->
                        LiveChannelRow(
                            channel,
                            { onMediaClick(channel) },
                            { onToggleFavorite(channel) },
                            initialFocus = channel.id == state.items.firstOrNull()?.id
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySidebar(categories: List<UiCategory>, selected: Int, onSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.width(210.dp).fillMaxHeight()
            .clip(RoundedCornerShape(15.dp))
            .background(Panel)
            .border(1.dp, Color(0xFF292929), RoundedCornerShape(15.dp))
            .padding(12.dp)
    ) {
        Text("Categories", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            columnItems(categories) { category ->
                val index = categories.indexOf(category)
                CategorySidebarItem(category, index == selected) { onSelected(index) }
            }
        }
    }
}

@Composable
private fun CategorySidebarItem(category: UiCategory, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.then(if (selected) Modifier.questInitialFocus() else Modifier)
            .fillMaxWidth().heightIn(min = 42.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Gold.copy(alpha = 0.2f) else Color.Transparent,
            focusedContainerColor = Gold
        ),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, GoldLight)))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(category.title, color = if (focused) Navy else White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (category.isLocked) Text("PIN", color = if (focused) Navy else GoldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LiveChannelRow(
    item: UiMedia,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    initialFocus: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.then(if (initialFocus) Modifier.questInitialFocus() else Modifier)
            .fillMaxWidth().height(78.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(13.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, focusedContainerColor = Color(0xFF292929)),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Gold)))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(102.dp).height(56.dp).clip(RoundedCornerShape(8.dp)).background(PanelSoft), contentAlignment = Alignment.Center) {
                ArtworkImage(
                    imageUrl = item.imageUrl,
                    title = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    requestHeaders = item.imageHeaders,
                    fallbackText = item.badge ?: "LIVE",
                    fallbackTextSize = 11.sp,
                    fallbackColor = GoldLight
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.description?.takeIf { it.isNotBlank() } ?: "Live TV", color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (item.isLocked) StatusPill("PIN", GoldLight)
            Spacer(modifier = Modifier.width(10.dp))
            FavoriteButton(item.isFavorite, onToggleFavorite)
        }
    }
}

@Composable
private fun ContentBrowserScreen(
    state: StbPlayLibraryState,
    selectedFilter: ContentKindFilter,
    onFilterChanged: (ContentKindFilter) -> Unit,
    onLoadMore: () -> Unit,
    onCategorySelected: (Int) -> Unit,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 30.dp, bottom = 28.dp)) {
        CategorySidebar(state.categories, state.selectedCategory, onCategorySelected)
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Movies & Series", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("Provider catalogue", color = Muted, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(state.totalItemsText.ifBlank { "${state.items.size} titles" }, color = Muted, fontSize = 12.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    ContentKindFilter.entries.forEach { filter ->
                        FilterChip(filter, filter == selectedFilter) { onFilterChanged(filter) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(17.dp))
            when {
                state.loading -> LoadingContent("Loading provider catalogue…")
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("No titles loaded for this filter", color = White, fontSize = 20.sp)
                        Text(state.emptyMessage, color = Muted, fontSize = 13.sp)
                        if (state.hasMore) QuestButton(onClick = onLoadMore, enabled = !state.loadingMore) {
                            Text(if (state.loadingMore) "Loading…" else "Load more titles")
                        }
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 166.dp),
                    modifier = Modifier.fillMaxSize().focusGroup(),
                    contentPadding = PaddingValues(bottom = 30.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(17.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    gridItems(state.items, key = { "${it.streamType}:${it.id}" }) { media ->
                        MediaCard(media, { onMediaClick(media) }, { onToggleFavorite(media) })
                    }
                    if (state.hasMore) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            QuestButton(onClick = onLoadMore, enabled = !state.loadingMore) {
                                Text(if (state.loadingMore) "Loading…" else "Load more titles")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(filter: ContentKindFilter, selected: Boolean, onClick: () -> Unit) {
    val label = when (filter) {
        ContentKindFilter.ALL -> "All"
        ContentKindFilter.MOVIES -> "Movies"
        ContentKindFilter.SERIES -> "Series"
    }
    var focused by remember { mutableStateOf(false) }
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.height(38.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = if (selected) Gold else Panel, focusedContainerColor = Gold),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, GoldLight)))
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected || focused) Navy else White, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 30.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            Text("Favourites", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Channels and titles saved on this device", color = Muted, fontSize = 13.sp)
        }
        item {
            if (channels.isEmpty()) EmptyInline("Favourite channels", "Use Save on any live channel to add it here.")
            else MediaRow(UiMediaRow("favorite-channels", "Favourite channels", items = channels), onMediaClick, onToggleFavorite) { }
        }
        item {
            if (titles.isEmpty()) EmptyInline("Favourite titles", "Use Add to favourites on a movie or series.")
            else MediaRow(UiMediaRow("favorite-titles", "Favourite titles", items = titles), onMediaClick, onToggleFavorite) { }
        }
    }
}

@Composable
private fun MediaCard(
    item: UiMedia,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveHistory: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.055f else 1f, label = "cardScale")
    val width = if (item.portrait) 166.dp else 235.dp
    val height = if (item.portrait) 235.dp else 138.dp
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.width(width).height(height).graphicsLayer(scaleX = scale, scaleY = scale).onFocusChanged { focused = it.isFocused },
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
                Modifier.fillMaxWidth().height(82.dp).align(Alignment.BottomCenter).background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xF8070707)))
                )
            )
            item.badge?.let { Text(it, color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(9.dp)) }
            if (item.isLocked) StatusPill("PIN", GoldLight, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            FavoriteButton(item.isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd).padding(6.dp))
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
            Text(item.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(11.dp))
            if (item.progress > 0f) ProgressBar(item.progress, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    QuestSurface(
        onClick = onClick,
        modifier = modifier.size(34.dp),
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
    onRefresh: () -> Unit,
    onClearCache: () -> Unit,
    onClearHistory: () -> Unit,
    onAddPortal: () -> Unit,
    onEditPortal: (PortalSettings) -> Unit,
    onUsePortal: (PortalSettings) -> Unit,
    onDeletePortal: (PortalSettings) -> Unit,
    onPlayerPreferenceChanged: (PlayerPreference) -> Unit,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    onSubtitlePreferenceChanged: (SubtitlePreference) -> Unit,
    onCatalogueLanguageChanged: (String) -> Unit,
    onAnalyticsChanged: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onShare: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 30.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Your STB Play preferences are stored on this device.", color = Muted, fontSize = 13.sp)
        }
        item { SubscriptionCard(state) }
        item {
            SettingsSection("Content sources") {
                state.profiles.forEach { profile ->
                    PortalProfileRow(
                        profile = profile,
                        active = profile.id == state.activeProfileId,
                        onUse = { onUsePortal(profile) },
                        onEdit = { onEditPortal(profile) },
                        onDelete = { onDeletePortal(profile) }
                    )
                }
                WideAction("Add portal", onAddPortal)
            }
        }
        item {
            SettingsSection("Playback") {
                PreferenceRow("Default player", state.playerPreference.displayName()) {
                    onPlayerPreferenceChanged(state.playerPreference.next())
                }
                PreferenceRow("Audio & subtitles", state.subtitlePreference.displayName()) {
                    onSubtitlePreferenceChanged(state.subtitlePreference.next())
                }
                Text("Internal Media3 is used first. Auto sends a failed stream to VLC only when VLC is installed.", color = Muted, fontSize = 11.sp)
            }
        }
        item {
            SettingsSection("Appearance & language") {
                PreferenceRow("App theme", state.themePreference.displayName()) { onThemePreferenceChanged(state.themePreference.next()) }
                PreferenceRow("Catalogue language", state.catalogueLanguage) {
                    onCatalogueLanguageChanged(nextLanguage(state.catalogueLanguage))
                }
            }
        }
        item {
            SettingsSection("Content") {
                PreferenceRow("Catalogue", "${state.liveCount} live · ${state.movieCount} movies · ${state.seriesCount} series") {}
                Text("Last refresh: ${state.lastRefreshText}", color = Muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryAction("Refresh content", onRefresh)
                    WideAction("Clear local cache", onClearCache)
                }
            }
        }
        item {
            SettingsSection("History & parental controls") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WideAction("Clear watch history", onClearHistory)
                    PrimaryAction("Change PIN", onChangePin)
                }
                Text("Adult and A-rated content stays locked until the parental PIN is entered. It re-locks when you leave.", color = Muted, fontSize = 11.sp)
            }
        }
        item {
            SettingsSection("Anonymous analytics") {
                PreferenceRow("Anonymous diagnostics", if (state.analyticsEnabled) "Enabled" else "Disabled") {
                    onAnalyticsChanged(!state.analyticsEnabled)
                }
                Text("No portal URL, MAC address, stream URL, title, channel name, or personal files are sent.", color = Muted, fontSize = 11.sp)
            }
        }
        item {
            SettingsSection("Updates") {
                Text(state.updateText, color = if (state.updateAvailableVersion == null) Muted else Good, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WideAction("Check for updates", onCheckUpdates)
                    if (state.updateAvailableVersion != null) PrimaryAction("Download & install", onDownloadUpdate)
                }
            }
        }
        item {
            SettingsSection("About, FAQ & policies") {
                Text("STB Play does not provide any IPTV service, subscriptions, channels, movies, or streams. Use only sources you are authorized to access.", color = Muted, fontSize = 12.sp)
                WideAction("Share STB Play", onShare)
            }
        }
    }
}

@Composable
private fun SubscriptionCard(state: StbPlaySettingsState) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(PanelSoft).padding(19.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Subscription", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(state.subscriptionPlan, color = White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        val details = listOf(state.subscriptionStatus, state.expiryText).filter { it.isNotBlank() }.joinToString(" · ")
        Text(details.ifBlank { "Subscription details are reported by the portal when available." }, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Text(title, color = GoldLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun PortalProfileRow(profile: PortalSettings, active: Boolean, onUse: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(if (active) Gold.copy(alpha = 0.13f) else Navy).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.name, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(if (active) "Active portal" else "Available portal", color = if (active) GoldLight else Muted, fontSize = 11.sp)
        }
        if (!active) WideAction("Use", onUse)
        Spacer(modifier = Modifier.width(8.dp))
        WideAction("Edit", onEdit)
        if (!active) {
            Spacer(modifier = Modifier.width(8.dp))
            WideAction("Delete", onDelete)
        }
    }
}

@Composable
private fun PreferenceRow(title: String, value: String, onClick: () -> Unit) {
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Navy, focusedContainerColor = Gold.copy(alpha = 0.18f)),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Gold)))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(value, color = GoldLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WideAction(title: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.height(38.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF262626), focusedContainerColor = Gold),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, GoldLight)))
    ) { Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text(title, color = if (focused) Navy else White, fontSize = 12.sp) } }
}

@Composable
private fun PrimaryAction(title: String, onClick: () -> Unit) {
    QuestButton(onClick = onClick, colors = ButtonDefaults.colors(containerColor = Gold, contentColor = Navy, focusedContainerColor = GoldLight, focusedContentColor = Navy)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StbPlaySearchScreen(
    catalog: List<UiMedia>,
    onMediaClick: (UiMedia) -> Unit,
    onToggleFavorite: (UiMedia) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var query by remember { mutableStateOf("") }
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) { requester.requestFocus() }
    val results = remember(query, catalog) {
        if (query.trim().length < 3) emptyList()
        else catalog.filter { it.matchesStrictQuery(query) }.distinctBy { "${it.streamType}:${it.id}" }
    }
    Column(modifier = Modifier.fillMaxSize().background(Navy).padding(36.dp), verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Search catalogue", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Strict title and metadata search", color = Muted, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            HeaderAction("Back", onBack)
        }
        BasicTextField(
            value = query,
            onValueChange = { query = it.take(80) },
            modifier = Modifier.fillMaxWidth().height(58.dp).focusRequester(requester).background(Panel, RoundedCornerShape(12.dp)).border(2.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
            singleLine = true,
            textStyle = TextStyle(color = White, fontSize = 18.sp),
            decorationBox = { inner ->
                if (query.isBlank()) Text("Type at least 3 characters…", color = Muted, fontSize = 17.sp)
                inner()
            }
        )
        when {
            query.trim().length < 3 -> Text("Search title, original title, genre, language, cast, or year.", color = Muted, fontSize = 14.sp)
            results.isEmpty() -> EmptyState("No exact matches", "Try a complete word or another title.")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 166.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(17.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                gridItems(results, key = { "${it.streamType}:${it.id}" }) { media ->
                    MediaCard(media, { onMediaClick(media) }, { onToggleFavorite(media) })
                }
            }
        }
    }
}

private fun UiMedia.matchesStrictQuery(rawQuery: String): Boolean {
    val query = rawQuery.trim().lowercase().replace(Regex("\\s+"), " ")
    if (query.length < 3) return false
    val words = listOf(title, subtitle, description, searchText, language, genre, rating, cast, year?.toString())
        .filterNotNull()
        .joinToString(" ")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
    val normalizedQuery = query.replace(Regex("[^a-z0-9]+"), " ").trim()
    return Regex("(?:^| )${Regex.escape(normalizedQuery)}(?= |$)").containsMatchIn(words)
}

@Composable
fun FirstStartDisclaimer(onAccept: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = 690.dp).padding(32.dp),
            shape = RoundedCornerShape(20.dp),
            colors = SurfaceDefaults.colors(containerColor = Panel)
        ) {
            Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text("Before you continue", color = GoldLight, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("STB Play is a media player. It does not provide IPTV service, channels, movies, subscriptions, stream URLs, or access credentials.", color = White, fontSize = 15.sp, textAlign = TextAlign.Center)
                Text("Use only portals and content you are authorized to access. Your portal URL and MAC address remain on this device.", color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                QuestButton(
                    onClick = onAccept,
                    modifier = Modifier.questInitialFocus(),
                    colors = ButtonDefaults.colors(containerColor = Gold, contentColor = Navy, focusedContainerColor = GoldLight, focusedContentColor = Navy)
                ) { Text("I have read and understand", fontWeight = FontWeight.Bold) }
            }
        }
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
                                colors = ButtonDefaults.colors(containerColor = Navy, contentColor = White, focusedContainerColor = Gold, focusedContentColor = Navy)
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
fun ChangePinPrompt(expectedPin: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    BackHandler(onBack = onCancel)
    Box(Modifier.fillMaxSize().background(Color(0xD9070707)), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(min = 400.dp, max = 560.dp),
            shape = RoundedCornerShape(18.dp),
            colors = SurfaceDefaults.colors(containerColor = Panel)
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("Change parental PIN", color = GoldLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                PinField("Current PIN", current, initialFocus = true) { current = it; error = "" }
                PinField("New PIN", next) { next = it; error = "" }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryAction("Update") {
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
private fun PinField(label: String, value: String, initialFocus: Boolean = false, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.filter(Char::isDigit).take(8)) },
            modifier = Modifier.then(if (initialFocus) Modifier.questInitialFocus() else Modifier)
                .fillMaxWidth().height(48.dp).background(Navy, RoundedCornerShape(10.dp)).border(1.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
            singleLine = true,
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
    ThemePreference.DARK -> ThemePreference.LIGHT
    ThemePreference.LIGHT -> ThemePreference.SYSTEM
    ThemePreference.SYSTEM -> ThemePreference.DARK
}

private fun ThemePreference.displayName(): String = when (this) {
    ThemePreference.DARK -> "Dark navy"
    ThemePreference.LIGHT -> "Light"
    ThemePreference.SYSTEM -> "Device setting"
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
