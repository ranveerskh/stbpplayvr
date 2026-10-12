package com.example.stbplay.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferencePolicyTest {
    @Test fun onlyFreshInstallGetsGlassByDefault() {
        assertEquals(ThemePreference.GLASS, resolveThemePreference(null, false))
        assertEquals(ThemePreference.BLUE, resolveThemePreference(null, true))
    }
    @Test fun existingAndLegacyChoicesArePreserved() {
        ThemePreference.entries.forEach { assertEquals(it, resolveThemePreference(it.name, true)) }
        listOf("DARK", "SYSTEM").forEach { assertEquals(ThemePreference.BLACK, resolveThemePreference(it, true)) }
        assertEquals(ThemePreference.BLUE, resolveThemePreference("unknown", true))
    }
}
