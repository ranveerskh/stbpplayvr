package com.example.stbplay.data

enum class GlassBackground { PLANET, MIDNIGHT_LAKE, GRAPHITE_SILK, CUSTOM }
enum class AdultThemePreference { AFTER_DARK, GLASS }

data class GlassAppearance(
    val background: GlassBackground = GlassBackground.PLANET,
    val customPath: String = "",
    val adultTheme: AdultThemePreference = AdultThemePreference.AFTER_DARK,
    val adultCustomPath: String = "",
    val blurPercent: Int = 0
)

internal fun parseGlassBackground(raw: String?): GlassBackground =
    runCatching { GlassBackground.valueOf(raw.orEmpty()) }.getOrDefault(GlassBackground.PLANET)

internal fun parseAdultTheme(raw: String?): AdultThemePreference =
    runCatching { AdultThemePreference.valueOf(raw.orEmpty()) }.getOrDefault(AdultThemePreference.AFTER_DARK)
