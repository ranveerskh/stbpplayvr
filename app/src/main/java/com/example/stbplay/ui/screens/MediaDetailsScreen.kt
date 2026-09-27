@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stbplay.ui.QuestButton
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.stbplay.data.model.PortalQualityOption
import com.example.stbplay.ui.UiMedia
import com.example.stbplay.ui.questInitialFocus

private val DetailNavy = Color(0xFF061426)
private val DetailPanel = Color(0xFF0D223B)
private val DetailGold = Color(0xFFDDB32F)
private val DetailGoldLight = Color(0xFFFFD966)
private val DetailWhite = Color(0xFFF4F6FA)
private val DetailMuted = Color(0xFF9AA8B8)

@Composable
fun MovieDetailsScreen(
    item: UiMedia,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(DetailNavy)) {
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(item.imageUrl, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(DetailNavy, DetailNavy.copy(alpha = 0.92f), DetailNavy.copy(alpha = 0.55f)))))
        }
        Row(Modifier.fillMaxSize().padding(50.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(250.dp).height(360.dp)) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(item.imageUrl, item.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(DetailPanel), contentAlignment = Alignment.Center) {
                        Text(item.title.take(1).uppercase(), color = DetailGold, fontSize = 70.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.width(44.dp))
            Column(Modifier.weight(1f).widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(item.badge ?: "MOVIE", color = DetailGoldLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(item.title, color = DetailWhite, fontSize = 35.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(item.year?.toString(), item.language, item.genre, item.rating?.takeIf { it.isNotBlank() }).joinToString("  ·  ")
                if (meta.isNotBlank()) Text(meta, color = DetailMuted, fontSize = 14.sp)
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = DetailWhite.copy(alpha = 0.9f), fontSize = 15.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
                item.cast?.takeIf { it.isNotBlank() }?.let { Text("Cast: $it", color = DetailMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuestButton(onClick = onPlay, modifier = Modifier.questInitialFocus(), colors = ButtonDefaults.colors(containerColor = DetailGold, contentColor = DetailNavy, focusedContainerColor = DetailGoldLight, focusedContentColor = DetailNavy)) {
                        Text("Play", fontWeight = FontWeight.Bold)
                    }
                    if (item.progress > 0f) {
                        QuestButton(onClick = onResume, colors = ButtonDefaults.colors(containerColor = Color(0xFF153452), contentColor = DetailWhite)) {
                            Text("Resume ${(item.progress * 100).toInt()}%")
                        }
                    }
                    QuestButton(onClick = onToggleFavorite, colors = ButtonDefaults.colors(containerColor = Color(0xFF153452), contentColor = DetailWhite)) {
                        Text(if (item.isFavorite) "Remove favourite" else "Add to favourites")
                    }
                    QuestButton(onClick = onBack, colors = ButtonDefaults.colors(containerColor = Color(0xFF153452), contentColor = DetailWhite)) { Text("Back") }
                }
            }
        }
    }
}

@Composable
fun QualitySelectionScreen(
    title: String,
    options: List<PortalQualityOption>,
    isLoading: Boolean,
    error: String?,
    onOptionClick: (PortalQualityOption) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(DetailNavy), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(min = 420.dp, max = 720.dp),
            shape = RoundedCornerShape(18.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(containerColor = DetailPanel)
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Choose quality", color = DetailGoldLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(title, color = DetailWhite, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when {
                    isLoading -> Text("Reading the provider's playback options…", color = DetailMuted, fontSize = 13.sp)
                    !error.isNullOrBlank() -> Text(error, color = Color(0xFFFFA4A4), fontSize = 13.sp)
                    options.isEmpty() -> Text("This provider did not return a playable option.", color = DetailMuted, fontSize = 13.sp)
                    else -> options.forEachIndexed { index, option ->
                        QuestButton(
                            onClick = { onOptionClick(option) },
                            modifier = Modifier.then(if (index == 0) Modifier.questInitialFocus() else Modifier).fillMaxWidth(),
                            colors = ButtonDefaults.colors(containerColor = DetailGold, contentColor = DetailNavy, focusedContainerColor = DetailGoldLight, focusedContentColor = DetailNavy)
                        ) { Text(option.label, fontWeight = FontWeight.Bold) }
                    }
                }
                QuestButton(onClick = onBack, colors = ButtonDefaults.colors(containerColor = Color(0xFF153452), contentColor = DetailWhite)) { Text("Back") }
            }
        }
    }
}
