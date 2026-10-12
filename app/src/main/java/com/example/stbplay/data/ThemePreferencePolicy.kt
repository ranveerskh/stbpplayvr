package com.example.stbplay.data

/** Preserve stored themes and the implicit Blue default on existing installations. */
internal fun resolveThemePreference(raw: String?, hasExistingSettings: Boolean): ThemePreference = when (raw) {
    null -> if (hasExistingSettings) ThemePreference.BLUE else ThemePreference.GLASS
    "DARK", "SYSTEM", ThemePreference.BLACK.name -> ThemePreference.BLACK
    ThemePreference.LIGHT.name -> ThemePreference.LIGHT
    ThemePreference.GLASS.name -> ThemePreference.GLASS
    else -> ThemePreference.BLUE
}
