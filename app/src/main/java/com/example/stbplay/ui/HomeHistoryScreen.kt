package com.example.stbplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.stbplay.ui.theme.LocalStbPalette
import com.example.stbplay.ui.theme.stbBackdrop

@Composable
internal fun HomeLiveChannelCard(
    item: UiMedia, onClick: () -> Unit, focusRequester: FocusRequester? = null,
    restoreFocusRequester: FocusRequester? = null, onFocused: () -> Unit = {},
    focusId: String, onReturnToCategory: (() -> Unit)? = null,
    onFocusDown: (() -> Unit)? = null, onFocusUp: (() -> Unit)? = null
) {
    val palette = LocalStbPalette.current
    val density = LocalDensity.current
    TvHomeSurface(onClick, Modifier.width(186.dp).height(72.dp).tvFocusTarget(focusId)
        .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
        .then(restoreFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
        .onFocusChanged { if (it.isFocused) onFocused() }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) false else when {
                event.key == Key.DirectionLeft && onReturnToCategory != null -> { onReturnToCategory(); true }
                event.key == Key.DirectionDown && onFocusDown != null -> { onFocusDown(); true }
                event.key == Key.DirectionUp && onFocusUp != null -> { onFocusUp(); true }
                else -> false
            }
        }) {
        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArtworkImage(item.imageUrl, item.title, Modifier.size(48.dp), ContentScale.Fit,
                requestHeaders = item.imageHeaders, fallbackTextSize = 18.sp,
                decodeWidthPx = with(density) { 48.dp.roundToPx() }, decodeHeightPx = with(density) { 48.dp.roundToPx() })
            Column(Modifier.weight(1f)) {
                Text(item.title, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.badge ?: "LIVE", color = palette.muted, fontSize = 10.sp)
            }
        }
    }
}

/** Full saved history stays reachable; Home alone is limited to ten previews. */
@Composable
fun HomeHistoryScreen(title: String, items: List<UiMedia>, onMediaClick: (UiMedia) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val palette = LocalStbPalette.current
    Column(Modifier.fillMaxSize().stbBackdrop().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TvHomeSurface(onBack, Modifier.width(100.dp).height(44.dp).questInitialFocus()) {
            Text("Back", color = palette.text, modifier = Modifier.align(Alignment.Center))
        }
        Text(title, color = palette.text, fontSize = 20.sp)
        if (items.isEmpty()) Text("No watched content", color = palette.muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { "${it.streamType}:${it.id}" }) { item ->
                TvHomeSurface({ onMediaClick(item) }, Modifier.fillMaxWidth().height(72.dp)) {
                    Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArtworkImage(item.imageUrl, item.title, Modifier.size(48.dp), ContentScale.Fit, item.imageHeaders)
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.resumeLabel ?: if (item.streamType == "live") "Live TV" else "Resume ${(item.progress * 100).toInt()}%",
                                color = palette.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
