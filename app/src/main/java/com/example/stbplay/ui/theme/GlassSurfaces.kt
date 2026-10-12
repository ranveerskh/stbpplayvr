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
    val selection = LocalGlassBackdrop.current
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >=
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val appearance = selection.appearance
    val customPath = if (selection.adult) appearance.adultCustomPath else
        appearance.customPath.takeIf { appearance.background == com.example.stbplay.data.GlassBackground.CUSTOM }.orEmpty()
    val resource = if (selection.adult) {
        if (landscape) com.example.stbplay.R.drawable.glass_adult_landscape else com.example.stbplay.R.drawable.glass_adult_portrait
    } else when (appearance.background) {
        com.example.stbplay.data.GlassBackground.MIDNIGHT_LAKE -> com.example.stbplay.R.drawable.glass_midnight_lake
        com.example.stbplay.data.GlassBackground.GRAPHITE_SILK -> com.example.stbplay.R.drawable.glass_graphite_silk
        else -> com.example.stbplay.R.drawable.glass_backdrop
    }
    val fallback = androidx.compose.ui.res.painterResource(resource)
    val image = if (customPath.isNotBlank() && remember(customPath) { java.io.File(customPath).isFile })
        coil.compose.rememberAsyncImagePainter(model = java.io.File(customPath), placeholder = fallback, error = fallback)
        else fallback
    return paint(image, sizeToIntrinsics = false, contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.12f))
}

/** Draw static reflection beneath content so nested panels cannot wash out text or yellow focus. */
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
        val r = radius.toPx()
        drawRoundRect(
            brush = reflection,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
        )
        drawContent()
    }
}
