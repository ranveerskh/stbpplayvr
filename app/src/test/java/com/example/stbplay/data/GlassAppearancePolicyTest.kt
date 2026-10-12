package com.example.stbplay.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassAppearancePolicyTest {
    @Test fun oldAndUnknownSettingsKeepExistingDefaults() {
        assertEquals(GlassBackground.PLANET, parseGlassBackground(null))
        assertEquals(GlassBackground.PLANET, parseGlassBackground("unknown"))
        assertEquals(AdultThemePreference.AFTER_DARK, parseAdultTheme(null))
        assertEquals(AdultThemePreference.AFTER_DARK, parseAdultTheme("unknown"))
    }
    @Test fun savedBackgroundAndAdultChoicesResolveIndependently() {
        GlassBackground.entries.forEach { assertEquals(it, parseGlassBackground(it.name)) }
        AdultThemePreference.entries.forEach { assertEquals(it, parseAdultTheme(it.name)) }
    }
}
