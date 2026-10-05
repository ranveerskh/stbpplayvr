package com.example.stbplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.stbplay.ui.TvHomeSurface
import com.example.stbplay.ui.UiMedia
import com.example.stbplay.ui.theme.LocalStbPalette

/** UI only. The existing PlaybackRoute is rendered into this measured slot. */
@Composable
fun TvLivePreviewPanel(
    channel: UiMedia?,
    actionsRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onVideoBounds: (Rect) -> Unit,
    onFullscreen: () -> Unit,
    onFavorite: () -> Unit,
    onVlc: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalStbPalette.current
    Column(
        modifier.background(palette.panel, RoundedCornerShape(12.dp)).padding(12.dp)
            .onPreviewKeyEvent {
                if (it.key == Key.DirectionLeft) {
                    if (it.type == KeyEventType.KeyDown) onBack()
                    true
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .onGloballyPositioned { onVideoBounds(it.boundsInRoot()) }.background(Color.Black),
            contentAlignment = Alignment.Center) {
            if (channel == null) Text("Select a channel to preview", color = Color.White, fontSize = 13.sp)
        }
        Text(channel?.title ?: "Channel preview", color = palette.text, fontSize = 17.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (channel != null) {
            Text(listOfNotNull(channel.badge, channel.subtitle).filter { it.isNotBlank() }.joinToString(" • "),
                color = palette.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            channel.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        PreviewAction("Full screen", Modifier.focusRequester(actionsRequester), onFullscreen)
        if (channel != null) {
            PreviewAction(if (channel.isFavorite) "Remove from favourites" else "Add to favourites", onClick = onFavorite)
            PreviewAction("Not working? Play in VLC", onClick = onVlc)
            PreviewAction("Back", onClick = onBack)
        }
    }
}

@Composable
private fun PreviewAction(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalStbPalette.current
    TvHomeSurface(onClick, modifier.fillMaxWidth().height(36.dp), containerColor = palette.panelSoft) {
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            Text(title, color = palette.text, fontSize = 12.sp)
        }
    }
}
