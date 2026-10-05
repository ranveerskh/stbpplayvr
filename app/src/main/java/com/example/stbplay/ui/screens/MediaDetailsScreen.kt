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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.stbplay.ui.QuestButton
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.stbplay.ui.ArtworkImage
import com.example.stbplay.data.model.PortalQualityOption
import com.example.stbplay.ui.UiMedia
import com.example.stbplay.ui.questInitialFocus
import com.example.stbplay.ui.theme.LocalStbPalette
import com.example.stbplay.isAndroidTvDevice

private val DetailNavy: Color @Composable get() = LocalStbPalette.current.background
private val DetailPanel: Color @Composable get() = LocalStbPalette.current.panelSoft
private val DetailGold: Color @Composable get() = LocalStbPalette.current.accent
private val DetailGoldLight: Color @Composable get() = LocalStbPalette.current.accentLight
private val DetailWhite: Color @Composable get() = LocalStbPalette.current.text
private val DetailMuted: Color @Composable get() = LocalStbPalette.current.muted
private val DetailOnAccent: Color @Composable get() = LocalStbPalette.current.onAccent

@Composable
fun MovieDetailsScreen(
    item: UiMedia,
    onPlay: () -> Unit,
    onResume: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onRemoveHistory: (() -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    if (context.isAndroidTvDevice()) {
        TvMediaDetailsScreen(item, onPlay, onResume, onToggleFavorite, onRemoveHistory, onBack)
        return
    }
    val compact = LocalConfiguration.current.screenWidthDp < 900 &&
        !context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
    if (compact) {
        Column(
            Modifier.fillMaxSize().background(DetailNavy).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ArtworkImage(
                imageUrl = item.imageUrl,
                title = item.title,
                modifier = Modifier.fillMaxWidth().height(210.dp),
                contentScale = ContentScale.Fit,
                requestHeaders = item.imageHeaders,
                fallbackTextSize = 52.sp,
                fallbackColor = DetailGold
            )
            Text(item.badge ?: "MOVIE", color = DetailGoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(item.title, color = DetailWhite, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(item.year?.toString(), item.language, item.genre, item.rating?.takeIf { it.isNotBlank() }).joinToString("  ·  ")
            if (meta.isNotBlank()) Text(meta, color = DetailMuted, fontSize = 13.sp)
            item.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = DetailWhite.copy(alpha = 0.9f), fontSize = 14.sp) }
            item.cast?.takeIf { it.isNotBlank() }?.let { Text("Cast: $it", color = DetailMuted, fontSize = 12.sp) }
            QuestButton(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().questInitialFocus(),
                colors = ButtonDefaults.colors(containerColor = DetailGold, contentColor = DetailOnAccent, focusedContainerColor = DetailGoldLight, focusedContentColor = DetailOnAccent)
            ) { Text("Play", fontWeight = FontWeight.Bold) }
            if (item.progress > 0f) QuestButton(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                Text("Resume ${(item.progress * 100).toInt()}%")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestButton(onClick = onToggleFavorite, modifier = Modifier.weight(1f), colors = ButtonDefaults.colors(containerColor = DetailPanel, contentColor = DetailWhite)) {
                    Text(if (item.isFavorite) "Remove favourite" else "Add to favourites", maxLines = 1)
                }
                QuestButton(onClick = onBack, modifier = Modifier.widthIn(min = 88.dp), colors = ButtonDefaults.colors(containerColor = DetailPanel, contentColor = DetailWhite)) { Text("Back") }
            }
        }
        return
    }
    Box(Modifier.fillMaxSize().background(DetailNavy)) {
        ArtworkImage(
            imageUrl = item.imageUrl,
            title = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            requestHeaders = item.imageHeaders,
            fallbackText = null
        )
        if (!item.imageUrl.isNullOrBlank()) {
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(DetailNavy, DetailNavy.copy(alpha = 0.92f), DetailNavy.copy(alpha = 0.55f)))))
        }
        Row(Modifier.fillMaxSize().padding(50.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(250.dp).height(360.dp)) {
                ArtworkImage(
                    imageUrl = item.imageUrl,
                    title = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    requestHeaders = item.imageHeaders,
                    fallbackTextSize = 70.sp,
                    fallbackColor = DetailGold
                )
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
                    QuestButton(onClick = onPlay, modifier = Modifier.questInitialFocus(), colors = ButtonDefaults.colors(containerColor = DetailGold, contentColor = DetailOnAccent, focusedContainerColor = DetailGoldLight, focusedContentColor = DetailOnAccent)) {
                        Text("Play", fontWeight = FontWeight.Bold)
                    }
                    if (item.progress > 0f) {
                        QuestButton(onClick = onResume, colors = ButtonDefaults.colors(containerColor = DetailPanel, contentColor = DetailWhite)) {
                            Text("Resume ${(item.progress * 100).toInt()}%")
                        }
                    }
                    QuestButton(onClick = onToggleFavorite, colors = ButtonDefaults.colors(containerColor = DetailPanel, contentColor = DetailWhite)) {
                        Text(if (item.isFavorite) "Remove favourite" else "Add to favourites")
                    }
                    QuestButton(onClick = onBack, colors = ButtonDefaults.colors(containerColor = DetailPanel, contentColor = DetailWhite)) { Text("Back") }
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
                Text("Play", color = DetailGoldLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(title, color = DetailWhite, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when {
                    isLoading -> Text("Reading the provider's playback options…", color = DetailMuted, fontSize = 13.sp)
                    !error.isNullOrBlank() -> Text(error, color = Color(0xFFFFA4A4), fontSize = 13.sp)
                    options.isEmpty() -> Text("This provider did not return a playable option.", color = DetailMuted, fontSize = 13.sp)
                    else -> options.forEachIndexed { index, option ->
                        QuestButton(
                            onClick = { onOptionClick(option) },
                            modifier = Modifier.then(if (index == 0) Modifier.questInitialFocus() else Modifier).fillMaxWidth(),
                            colors = ButtonDefaults.colors(containerColor = DetailGold, contentColor = DetailOnAccent, focusedContainerColor = DetailGoldLight, focusedContentColor = DetailOnAccent)
                        ) { Text(option.label, fontWeight = FontWeight.Bold) }
                    }
                }
                QuestButton(onClick = onBack, colors = ButtonDefaults.colors(containerColor = DetailPanel, contentColor = DetailWhite)) { Text("Back") }
            }
        }
    }
}
