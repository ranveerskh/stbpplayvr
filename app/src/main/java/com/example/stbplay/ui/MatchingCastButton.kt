package com.example.stbplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.cast.MediaRouteButton
import androidx.compose.material3.LocalContentColor
import androidx.tv.material3.Text
import com.example.stbplay.ui.theme.*

/** Preserve the real Cast route picker; only its surrounding surface is styled. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun MatchingCastButton(modifier: Modifier = Modifier) {
    val p = LocalStbPalette.current
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(p.panel).glassEdge().glassSheen(),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        CompositionLocalProvider(LocalContentColor provides p.text) {
            MediaRouteButton(Modifier.size(44.dp))
            Text("Cast", color = p.text, fontSize = 11.sp)
        }
    }
}
