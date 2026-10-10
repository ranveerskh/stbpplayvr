package com.example.stbplay.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.example.stbplay.data.ThemePreference

@OptIn(ExperimentalTvMaterial3Api::class)
data class StbPalette(
    val background: Color,
    val rail: Color,
    val panel: Color,
    val panelSoft: Color,
    val accent: Color,
    val accentLight: Color,
    val text: Color,
    val muted: Color,
    val onAccent: Color,
    val focusedAccent: Color = accentLight,
    val danger: Color = Color(0xFFFFA4A4),
    val good: Color = Color(0xFF87E7B0)
)

// Flat, cool surfaces keep artwork prominent; warm yellow remains the TV focus cue.
val BluePalette = StbPalette(
    background = Color(0xFF0B1020), rail = Color(0xFF10182A), panel = Color(0xFF172238),
    panelSoft = Color(0xFF202D45), accent = Color(0xFFFFD166), accentLight = Color(0xFFFFE4A3),
    text = Color(0xFFF4F7FC), muted = Color(0xFFAEBBD0), onAccent = Color(0xFF101827)
)
val LightPalette = StbPalette(
    background = Color(0xFFF4F7FC), rail = Color(0xFFEAF0F8), panel = Color(0xFFFFFFFF),
    panelSoft = Color(0xFFE5ECF6), accent = Color(0xFFFFD166), accentLight = Color(0xFF795000),
    text = Color(0xFF162237), muted = Color(0xFF52627A), onAccent = Color(0xFF101827),
    focusedAccent = Color(0xFFFFE4A3),
    danger = Color(0xFFB4233F), good = Color(0xFF176B45)
)
val BlackPalette = StbPalette(
    background = Color(0xFF080B10), rail = Color(0xFF10151D), panel = Color(0xFF191F29),
    panelSoft = Color(0xFF252E3B), accent = Color(0xFFFFD166), accentLight = Color(0xFFFFE4A3),
    text = Color(0xFFF5F7FA), muted = Color(0xFFADB9CA), onAccent = Color(0xFF101827)
)
val AfterDarkPalette = StbPalette(
    background = Color(0xFF100D16), rail = Color(0xFF191320), panel = Color(0xFF241C2E),
    panelSoft = Color(0xFF32263F), accent = Color(0xFFE879A8), accentLight = Color(0xFFF7B7D1),
    text = Color(0xFFFAF5FC), muted = Color(0xFFC5B5CF), onAccent = Color(0xFF1A1021)
)

val LocalStbPalette = staticCompositionLocalOf { BluePalette }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun STBPlayTheme(
    preference: ThemePreference = ThemePreference.BLUE,
    adultOnly: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = if (adultOnly) AfterDarkPalette else when (preference) {
        ThemePreference.BLUE -> BluePalette
        ThemePreference.LIGHT -> LightPalette
        ThemePreference.BLACK -> BlackPalette
    }
    val scheme = if (!adultOnly && preference == ThemePreference.LIGHT) lightColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent, background = palette.background,
        onBackground = palette.text, surface = palette.panel, onSurface = palette.text,
        secondary = palette.accentLight, onSecondary = palette.onAccent
    ) else darkColorScheme(
        primary = palette.accent, onPrimary = palette.onAccent, background = palette.background,
        onBackground = palette.text, surface = palette.panel, onSurface = palette.text,
        secondary = palette.accentLight, onSecondary = palette.onAccent
    )
    CompositionLocalProvider(LocalStbPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
