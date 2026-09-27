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
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.stbplay.data.PortalRepository
import com.example.stbplay.data.model.PortalEpisode
import com.example.stbplay.data.model.PortalSeason
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.ui.questInitialFocus

private val SeriesNavy = Color(0xFF061426)
private val SeriesPanel = Color(0xFF0D223B)
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
    var error by remember(series.id) { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    LaunchedEffect(series.id) {
        loadingSeasons = true
        error = null
        runCatching { repository.getSeasons(series.id) }
            .onSuccess { list -> seasons = list; selectedSeason = list.firstOrNull() }
            .onFailure { error = it.message ?: "Could not load seasons." }
        loadingSeasons = false
    }
    LaunchedEffect(series.id, selectedSeason?.id) {
        val season = selectedSeason ?: return@LaunchedEffect
        loadingEpisodes = true
        error = null
        runCatching { repository.getEpisodes(series.id, season.id) }
            .onSuccess { episodes = it }
            .onFailure { error = it.message ?: "Could not load episodes." }
        loadingEpisodes = false
    }

    Row(Modifier.fillMaxSize().background(SeriesNavy).padding(40.dp)) {
        Column(Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.fillMaxWidth().height(380.dp).background(SeriesPanel), contentAlignment = Alignment.Center) {
                if (!series.iconUrl.isNullOrBlank()) AsyncImage(series.iconUrl, series.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text(series.name.take(1).uppercase(), color = SeriesGold, fontSize = 70.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("SERIES", color = SeriesGoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(series.name, color = SeriesWhite, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            series.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = SeriesMuted, fontSize = 12.sp, maxLines = 5, overflow = TextOverflow.Ellipsis) }
            Button(onClick = onToggleFavorite, modifier = Modifier.questInitialFocus(), colors = ButtonDefaults.colors(containerColor = Color(0xFF153452), contentColor = SeriesWhite)) {
                Text(if (isFavorite) "Remove favourite" else "Add to favourites")
            }
            Button(onClick = onBack, colors = ButtonDefaults.colors(containerColor = Color(0xFF153452), contentColor = SeriesWhite)) { Text("Back") }
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
                            Surface(
                                onClick = { selectedSeason = season },
                                modifier = Modifier.then(if (selected) Modifier.questInitialFocus() else Modifier).height(42.dp),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                                colors = ClickableSurfaceDefaults.colors(containerColor = if (selected) SeriesGold else SeriesPanel, focusedContainerColor = SeriesGold),
                                border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp, SeriesGoldLight)))
                            ) {
                                Box(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                    Text("Season ${season.number}", color = if (selected) SeriesNavy else SeriesWhite, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
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
    Surface(
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
