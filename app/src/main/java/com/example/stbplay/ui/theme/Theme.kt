package com.example.stbplay.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Navy,
    secondary = GoldSoft,
    onSecondary = Navy,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = NavyLight,
    onSurfaceVariant = GoldSoft
)

@OptIn(ExperimentalTvMaterial3Api::class)
private val LightColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = DarkOnSurface,
    secondary = Gold,
    onSecondary = Navy,
    background = DarkOnSurface,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun STBPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
