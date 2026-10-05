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
    val danger: Color = Color(0xFFFFA4A4),
    val good: Color = Color(0xFF87E7B0)
)

val BluePalette = StbPalette(
    background = Color(0xFF0A1019), rail = Color(0xFF101A27), panel = Color(0xFF1B2635),
    panelSoft = Color(0xFF263447), accent = Color(0xFFD6AC58), accentLight = Color(0xFFF6D896),
    text = Color(0xFFF6F8FC), muted = Color(0xFFB1BECE), onAccent = Color(0xFF0A1019)
)
val LightPalette = StbPalette(
    background = Color(0xFFF5F2EB), rail = Color(0xFFEDE8DF), panel = Color(0xFFFFFFFF),
    panelSoft = Color(0xFFF1ECE3), accent = Color(0xFFA1702F), accentLight = Color(0xFF81591E),
    text = Color(0xFF182638), muted = Color(0xFF5D6976), onAccent = Color(0xFF101B2B),
    danger = Color(0xFFB23D45), good = Color(0xFF277B58)
)
val BlackPalette = StbPalette(
    background = Color(0xFF070707), rail = Color(0xFF111111), panel = Color(0xFF181818),
    panelSoft = Color(0xFF252525), accent = Color(0xFFC9CDD2), accentLight = Color(0xFFF3F4F5),
    text = Color(0xFFF4F6FA), muted = Color(0xFF9AA8B8), onAccent = Color(0xFF070707)
)
val AfterDarkPalette = StbPalette(
    background = Color(0xFF09070B), rail = Color(0xFF140E15), panel = Color(0xFF211720),
    panelSoft = Color(0xFF30202B), accent = Color(0xFFB44C72), accentLight = Color(0xFFE08AA5),
    text = Color(0xFFF8F2F5), muted = Color(0xFFC0AEB8), onAccent = Color(0xFF120910)
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
