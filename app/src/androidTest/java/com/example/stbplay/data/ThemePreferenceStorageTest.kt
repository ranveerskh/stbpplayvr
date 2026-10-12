package com.example.stbplay.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceStorageTest {
    @Test fun savedThemesSurviveReloadAndGlassDoesNotChangeLayoutSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SettingsManager(context)
        val original = settings.themePreference.first()
        val position = settings.phoneCategoryPosition.first()
        val columns = settings.phoneMovieColumns.first()
        try {
            ThemePreference.entries.forEach { preference ->
                settings.setThemePreference(preference)
                assertEquals(preference, SettingsManager(context).themePreference.first())
                assertEquals(position, settings.phoneCategoryPosition.first())
                assertEquals(columns, settings.phoneMovieColumns.first())
            }
        } finally { settings.setThemePreference(original) }
    }
}
