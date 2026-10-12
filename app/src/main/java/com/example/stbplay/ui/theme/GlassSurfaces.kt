@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border

/** A single static stroke, drawn only for Glass; existing theme surfaces stay unchanged. */
@Composable
fun glassSurfaceBorder(): Border = if (LocalStbPalette.current.glass)
    Border(BorderStroke(1.dp, LocalStbPalette.current.edge)) else Border.None

@Composable
fun Modifier.glassEdge(radius: Dp = 12.dp): Modifier = if (LocalStbPalette.current.glass)
    border(1.dp, LocalStbPalette.current.edge, RoundedCornerShape(radius)) else this

/** One cached static bitmap. No blur, RenderEffect, offscreen layer or animation. */
@Composable
fun Modifier.stbBackdrop(): Modifier {
    val palette = LocalStbPalette.current
    if (!palette.glass) return background(palette.background)
    val image = androidx.compose.ui.res.painterResource(com.example.stbplay.R.drawable.glass_backdrop)
    return paint(image, sizeToIntrinsics = false, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.12f))
}

/** Fixed surface reflection; it does not sample or blur pixels behind the panel. */
@Composable
fun Modifier.glassSheen(radius: Dp = 12.dp): Modifier {
    if (!LocalStbPalette.current.glass) return this
    val reflection = remember {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.085f),
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.015f),
                androidx.compose.ui.graphics.Color.Transparent
            )
        )
    }
    return drawWithContent {
        drawContent()
        val r = radius.toPx()
        drawRoundRect(
            brush = reflection,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
        )
    }
}
