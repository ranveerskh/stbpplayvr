package com.example.stbplay.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stbplay.data.FreeDramaCatalog
import com.example.stbplay.ui.theme.LocalStbPalette

/** Experimental portal selection never writes the IPTV activePortal or account store. */
@Composable
fun ExperimentalPortalHost(isKids: Boolean = false, onPlayEpisode: ((Int) -> Unit)? = null, iptvContent: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("free_drama_experiment", Context.MODE_PRIVATE) }
    var iptv by rememberSaveable { mutableStateOf(prefs.getBoolean("iptv_selected", false)) }
    var showPrivacy by remember { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    val palette = LocalStbPalette.current
    if (showPrivacy) AlertDialog(
        onDismissRequest = { showPrivacy = false },
        title = { Text("YouTube playback privacy") },
        text = { Text(FreeDramaCatalog.playbackPrivacy) },
        confirmButton = { PortalTestButton("Close", "privacy_close") { showPrivacy = false } }
    )
    BackHandler(enabled = !iptv && details) { details = false }
    Column(Modifier.fillMaxSize().background(palette.background).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PortalTestButton(if (!iptv) "✓ Free Drama" else "Free Drama", "portal_default") {
                iptv = false
                prefs.edit().putBoolean("iptv_selected", false).apply()
            }
            PortalTestButton(if (iptv) "✓ IPTV portals" else "IPTV portals", "portal_iptv") {
                iptv = true
                prefs.edit().putBoolean("iptv_selected", true).apply()
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (iptv) iptvContent()
            else if (isKids) {
                Text("Free Drama test is available for adult profiles. Switch profile in IPTV settings.",
                    color = palette.text, modifier = Modifier.padding(24.dp))
            } else if (!details) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Free Drama · Portal Test", color = palette.text, fontSize = 24.sp)
                    Text("Watch official YouTube episodes in order. No IPTV account required.", color = palette.muted)
                    PortalTestButton("Zanjeerain · HUM TV · 39 episodes", "drama_zanjeerain") { details = true }
                    Text("Source: HUM TV on YouTube. Catalogue checked 14 Oct 2026.", color = palette.muted)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Text("Zanjeerain", color = palette.text, fontSize = 26.sp)
                        Text("HUM TV · Urdu · YouTube · Episodes 1–39", color = palette.muted)
                        Text("YouTube's player, ads and availability apply. Playback is not yet verified on your devices.", color = palette.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PortalTestButton("Back", "drama_back") { details = false }
                            PortalTestButton("Official playlist", "drama_playlist") { openDramaLink(context, FreeDramaCatalog.playlistUrl) }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PortalTestButton("YouTube terms", "youtube_terms") { openDramaLink(context, "https://www.youtube.com/t/terms") }
                            PortalTestButton("Playback privacy", "youtube_privacy") { showPrivacy = true }
                        }
                    }
                    items(FreeDramaCatalog.episodes, key = { it.videoId }) { episode ->
                        PortalTestButton("${episode.label} · Watch on YouTube", "drama_episode_${episode.number}", Modifier.fillMaxWidth()) {
                            if (onPlayEpisode != null) onPlayEpisode(episode.number)
                            else context.startActivity(Intent(context, YouTubeEpisodeActivity::class.java).putExtra("episode", episode.number))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PortalTestButton(label: String, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val palette = LocalStbPalette.current
    Button(onClick = onClick, modifier = modifier.testTag(tag).onFocusChanged { focused = it.isFocused }
        .border(if (focused) 2.dp else 0.dp, if (focused) Color(0xFFFFD166) else Color.Transparent, RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = palette.panel, contentColor = palette.text)) { Text(label) }
}

internal fun openDramaLink(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
