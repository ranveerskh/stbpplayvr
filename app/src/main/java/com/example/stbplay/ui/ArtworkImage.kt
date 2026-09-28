package com.example.stbplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Loads portal artwork with the same headers as the authenticated Stalker session.
 * A quiet themed fallback remains visible for missing or broken provider images.
 */
@Composable
fun ArtworkImage(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    requestHeaders: Map<String, String> = emptyMap(),
    fallbackText: String? = title.trim().firstOrNull()?.uppercaseChar()?.toString(),
    fallbackTextSize: TextUnit = 35.sp,
    fallbackColor: Color = Color(0xFFDDB32F)
) {
    val context = LocalContext.current
    val request = remember(imageUrl, requestHeaders) {
        imageUrl?.trim()?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(false)
                .apply {
                    requestHeaders.forEach { (name, value) -> addHeader(name, value) }
                }
                .build()
        }
    }

    if (request == null) {
        ArtworkFallback(modifier, fallbackText, fallbackTextSize, fallbackColor)
        return
    }

    Box(modifier) {
        ArtworkFallback(Modifier.fillMaxSize(), fallbackText, fallbackTextSize, fallbackColor)
        AsyncImage(
            model = request,
            contentDescription = title.takeIf { it.isNotBlank() },
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
    }
}

@Composable
private fun ArtworkFallback(
    modifier: Modifier,
    text: String?,
    textSize: TextUnit,
    accent: Color
) {
    Box(
        modifier = modifier.background(Color(0xFF222222)),
        contentAlignment = Alignment.Center
    ) {
        text?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = accent.copy(alpha = 0.68f),
                fontSize = textSize,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
