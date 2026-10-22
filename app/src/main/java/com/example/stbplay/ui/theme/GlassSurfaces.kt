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

/** Cached background bitmap; optional blur is computed once by Coil, never per frame. */
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
        com.example.stbplay.data.GlassBackground.MIDNIGHT_LAKE -> if (landscape) com.example.stbplay.R.drawable.glass_midnight_lake else com.example.stbplay.R.drawable.glass_midnight_lake_portrait
        com.example.stbplay.data.GlassBackground.GRAPHITE_SILK -> if (landscape) com.example.stbplay.R.drawable.glass_graphite_silk else com.example.stbplay.R.drawable.glass_graphite_silk_portrait
        else -> if (landscape) com.example.stbplay.R.drawable.glass_backdrop else com.example.stbplay.R.drawable.glass_planet_portrait
    }
    val fallback = androidx.compose.ui.res.painterResource(resource)
    val customAvailable = customPath.isNotBlank() && remember(customPath) { java.io.File(customPath).isFile }
    val backdropContext = androidx.compose.ui.platform.LocalContext.current
    val blurModel = remember(customPath, customAvailable, resource, appearance.blurPercent) {
        coil.request.ImageRequest.Builder(backdropContext)
            .data(if (customAvailable) java.io.File(customPath) else resource)
            .size(512).allowHardware(false)
            .transformations(StaticBackgroundBlur(appearance.blurPercent)).build()
    }
    val image = if (appearance.blurPercent > 0)
        coil.compose.rememberAsyncImagePainter(blurModel, placeholder = fallback, error = fallback)
        else if (customAvailable)
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

/** Fixed shared entrance artwork, completely independent of profile appearance preferences. */
@Composable
fun Modifier.profileSelectionBackdrop(): Modifier {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val resource = if (configuration.screenWidthDp >= configuration.screenHeightDp)
        com.example.stbplay.R.drawable.profile_welcome_landscape else com.example.stbplay.R.drawable.profile_welcome_portrait
    return paint(androidx.compose.ui.res.painterResource(resource), sizeToIntrinsics = false,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.12f))
}
