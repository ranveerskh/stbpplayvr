@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.Border
import com.example.stbplay.ui.QuestButton
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import com.example.stbplay.ui.QuestSurface
import androidx.tv.material3.Text
import com.example.stbplay.ui.ArtworkImage
import com.example.stbplay.data.PortalRepository
import com.example.stbplay.data.model.PortalEpisode
import com.example.stbplay.data.model.PortalSeason
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.ui.questInitialFocus

private val SeriesNavy = Color(0xFF070707)
private val SeriesPanel = Color(0xFF181818)
private val SeriesGold = Color(0xFFDDB32F)
private val SeriesGoldLight = Color(0xFFFFD966)
private val SeriesWhite = Color(0xFFF4F6FA)
private val SeriesMuted = Color(0xFF9AA8B8)

@Composable
fun SeriesDetailsScreen(
    series: PortalStream,
    repository: PortalRepository,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEpisodeClick: (PortalEpisode) -> Unit,
    onBack: () -> Unit
) {
    var seasons by remember(series.id) { mutableStateOf<List<PortalSeason>>(emptyList()) }
    var selectedSeason by remember(series.id) { mutableStateOf<PortalSeason?>(null) }
    var episodes by remember(series.id) { mutableStateOf<List<PortalEpisode>>(emptyList()) }
    var loadingSeasons by remember(series.id) { mutableStateOf(true) }
    var loadingEpisodes by remember(series.id) { mutableStateOf(false) }
    var directEpisodeFallback by remember(series.id) { mutableStateOf(false) }
    var error by remember(series.id) { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val compact = LocalConfiguration.current.screenWidthDp < 900 &&
        !context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)

    val artworkUrl = remember(series.id, series.iconUrl, repository) {
        repository.resolveArtworkUrl(series.iconUrl)
    }
    val artworkHeaders = remember(series.id, artworkUrl, repository) {
        repository.artworkRequestHeaders(series.iconUrl)
    }

    LaunchedEffect(series.id) {
        loadingSeasons = true
        error = null
        runCatching { repository.getSeasons(series.id) }
            .onSuccess { list ->
                if (list.isNotEmpty()) {
                    seasons = list
                    selectedSeason = list.firstOrNull()
                } else {
                    val directEpisodes = runCatching { repository.getEpisodes(series.id, "0", series.cmd) }.getOrDefault(emptyList())
                    if (directEpisodes.isNotEmpty()) {
                        directEpisodeFallback = true
                        seasons = listOf(PortalSeason("direct", "Episodes", 1))
                        selectedSeason = seasons.first()
                        episodes = directEpisodes
                    } else {
                        seasons = emptyList()
                        error = "This portal did not return seasons or episodes for this series."
                    }
                }
            }
            .onFailure { error = it.message ?: "Could not load seasons." }
        loadingSeasons = false
    }
    LaunchedEffect(series.id, selectedSeason?.id) {
        val season = selectedSeason ?: return@LaunchedEffect
        if (directEpisodeFallback && season.id == "direct") return@LaunchedEffect
        loadingEpisodes = true
        error = null
        runCatching { repository.getEpisodes(series.id, season.id, series.cmd) }
            .onSuccess { episodes = it }
            .onFailure { error = it.message ?: "Could not load episodes." }
        loadingEpisodes = false
    }

    if (compact) {
        Column(Modifier.fillMaxSize().background(SeriesNavy).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(92.dp).height(126.dp).background(SeriesPanel), contentAlignment = Alignment.Center) {
                    ArtworkImage(
                        imageUrl = artworkUrl,
                        title = series.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        requestHeaders = artworkHeaders,
                        fallbackTextSize = 30.sp,
                        fallbackColor = SeriesGold
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("SERIES", color = SeriesGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(series.name, color = SeriesWhite, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    series.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = SeriesMuted, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestButton(onClick = onToggleFavorite, modifier = Modifier.weight(1f), colors = ButtonDefaults.colors(containerColor = SeriesPanel, contentColor = SeriesWhite)) {
                    Text(if (isFavorite) "Remove favourite" else "Add to favourites", maxLines = 1)
                }
                QuestButton(onClick = onBack, modifier = Modifier.width(88.dp), colors = ButtonDefaults.colors(containerColor = SeriesPanel, contentColor = SeriesWhite)) { Text("Back") }
            }
            Text(if (directEpisodeFallback) "Episodes" else "Seasons", color = SeriesGoldLight, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            when {
                loadingSeasons -> Text("Loading seasons…", color = SeriesMuted, fontSize = 13.sp)
                !error.isNullOrBlank() -> Text(error!!, color = Color(0xFFFFA4A4), fontSize = 13.sp)
                seasons.isEmpty() -> Text("No episodes are available for this series.", color = SeriesMuted, fontSize = 13.sp)
                else -> LazyRow(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(seasons, key = { it.id }) { season ->
                        val selected = selectedSeason?.id == season.id
                        QuestSurface(
                            onClick = { selectedSeason = season },
                            modifier = Modifier.height(42.dp),
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                            colors = ClickableSurfaceDefaults.colors(containerColor = if (selected) SeriesGold else SeriesPanel, focusedContainerColor = SeriesGold),
                            border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, SeriesGoldLight)))
                        ) {
                            Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                Text(if (season.id == "direct") "Episodes" else "Season ${season.number}", color = if (selected) SeriesNavy else SeriesWhite, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            Text("Episode list", color = SeriesWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            when {
                loadingEpisodes -> Text("Loading episodes…", color = SeriesMuted, fontSize = 13.sp)
                episodes.isEmpty() -> Text("No episodes were returned for this season.", color = SeriesMuted, fontSize = 13.sp)
                else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(episodes, key = { it.id }) { episode ->
                        EpisodeRow(episode, initialFocus = episode.id == episodes.firstOrNull()?.id) { onEpisodeClick(episode) }
                    }
                }
            }
        }
        return
    }

    Row(Modifier.fillMaxSize().background(SeriesNavy).padding(40.dp)) {
        Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.fillMaxWidth().height(380.dp).background(SeriesPanel), contentAlignment = Alignment.Center) {
                ArtworkImage(
                    imageUrl = artworkUrl,
                    title = series.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    requestHeaders = artworkHeaders,
                    fallbackTextSize = 70.sp,
                    fallbackColor = SeriesGold
                )
            }
            Text("SERIES", color = SeriesGoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(series.name, color = SeriesWhite, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            series.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = SeriesMuted, fontSize = 12.sp, maxLines = 5, overflow = TextOverflow.Ellipsis) }
            QuestButton(onClick = onToggleFavorite, modifier = Modifier.questInitialFocus(), colors = ButtonDefaults.colors(containerColor = Color(0xFF262626), contentColor = SeriesWhite)) {
                Text(if (isFavorite) "Remove favourite" else "Add to favourites")
            }
            QuestButton(onClick = onBack, colors = ButtonDefaults.colors(containerColor = Color(0xFF262626), contentColor = SeriesWhite)) { Text("Back") }
        }
        Spacer(Modifier.width(42.dp))
        Column(Modifier.weight(1f)) {
            Text("${series.name} · Seasons", color = SeriesWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            when {
                loadingSeasons -> Text("Loading seasons…", color = SeriesMuted, fontSize = 14.sp)
                !error.isNullOrBlank() -> Text(error!!, color = Color(0xFFFFA4A4), fontSize = 14.sp)
                seasons.isEmpty() -> Text("This provider did not return seasons for this series.", color = SeriesMuted, fontSize = 14.sp)
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        seasons.take(12).forEach { season ->
                            val selected = selectedSeason?.id == season.id
                            QuestSurface(
                                onClick = { selectedSeason = season },
                                modifier = Modifier.then(if (selected) Modifier.questInitialFocus() else Modifier).height(42.dp),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                                colors = ClickableSurfaceDefaults.colors(containerColor = if (selected) SeriesGold else SeriesPanel, focusedContainerColor = SeriesGold),
                                border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, SeriesGoldLight)))
                            ) {
                                Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                Text(if (season.id == "direct") "Episodes" else "Season ${season.number}", color = if (selected) SeriesNavy else SeriesWhite, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(27.dp))
                    Text("Episodes", color = SeriesGoldLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    when {
                        loadingEpisodes -> Text("Loading episodes…", color = SeriesMuted, fontSize = 14.sp)
                        episodes.isEmpty() -> Text("No episodes were returned for this season.", color = SeriesMuted, fontSize = 14.sp)
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(episodes, key = { it.id }) { episode ->
                                EpisodeRow(episode, initialFocus = episode.id == episodes.firstOrNull()?.id) { onEpisodeClick(episode) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: PortalEpisode, initialFocus: Boolean = false, onClick: () -> Unit) {
    val playable = !episode.cmd.isNullOrBlank()
    QuestSurface(
        onClick = { if (playable) onClick() },
        modifier = Modifier.then(if (initialFocus) Modifier.questInitialFocus() else Modifier).fillMaxWidth().height(68.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = SeriesPanel, focusedContainerColor = SeriesGold.copy(alpha = 0.2f)),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, SeriesGold)))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(episode.name, color = SeriesWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                episode.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = SeriesMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Text(if (playable) "Choose quality" else "Unavailable", color = if (playable) SeriesGoldLight else SeriesMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
