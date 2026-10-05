package com.example.stbplay.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.stbplay.ui.ArtworkImage
import com.example.stbplay.ui.TvHomeSurface
import com.example.stbplay.ui.UiMedia
import com.example.stbplay.ui.questInitialFocus
import com.example.stbplay.ui.theme.LocalStbPalette

/** One focus target per TV tile; all secondary actions live on this page. */
@Composable
fun TvMediaDetailsScreen(
    item: UiMedia,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveHistory: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val palette = LocalStbPalette.current
    val canResume = item.streamType != "live" && item.progress > 0f && item.progress < 0.95f
    BoxWithConstraints(Modifier.fillMaxSize().background(palette.background).padding(24.dp)) {
        val posterHeight = (maxHeight * 0.75f).coerceAtMost(300.dp)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ArtworkImage(item.imageUrl, item.title,
                Modifier.width(if (item.portrait) posterHeight * 0.7f else 210.dp)
                    .height(if (item.portrait) posterHeight else 130.dp),
                contentScale = ContentScale.Fit, requestHeaders = item.imageHeaders)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.badge ?: item.streamType.uppercase(), color = palette.accentLight, fontSize = 11.sp)
                Text(item.title, color = palette.text, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                val metadata = listOfNotNull(item.year?.toString(), item.language, item.genre, item.rating)
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (metadata.isNotBlank()) Text(metadata, color = palette.muted, fontSize = 12.sp)
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                @Composable fun action(label: String, initialFocus: Boolean = false, onClick: () -> Unit) {
                    TvHomeSurface(onClick, Modifier.fillMaxWidth().height(40.dp)
                        .then(if (initialFocus) Modifier.questInitialFocus() else Modifier)) {
                        Text(label, color = palette.text, fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp))
                    }
                }
                if (canResume) action("Resume ${(item.progress * 100).toInt()}%", true, onResume)
                action(if (canResume) "Start over" else "Play", !canResume, onPlay)
                action(if (item.isFavorite) "Remove from favourites" else "Add to favourites", onClick = onToggleFavorite)
                onRemoveHistory?.let { action("Remove from history", onClick = it) }
                action("Back", onClick = onBack)
            }
        }
    }
}
