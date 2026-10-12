package com.example.stbplay.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceStorageTest {
    @Test fun backgroundsAndAdultThemeSurviveReloadWithoutOverwritingNormalTheme() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = SettingsManager(context)
        val theme = manager.themePreference.first()
        val old = manager.glassAppearance.first()
        try {
            GlassBackground.entries.filterNot { it == GlassBackground.CUSTOM }.forEach { background ->
                manager.setGlassBackground(background)
                manager.setAdultTheme(AdultThemePreference.GLASS)
                assertEquals(background, SettingsManager(context).glassAppearance.first().background)
                assertEquals(AdultThemePreference.GLASS, manager.glassAppearance.first().adultTheme)
                assertEquals(theme, manager.themePreference.first())
            }
            val source = java.io.File(context.cacheDir, "background-import-test.png")
            val bitmap = android.graphics.Bitmap.createBitmap(3000, 1800, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.rgb(20, 30, 45))
            source.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val copy = importCustomBackground(context, android.net.Uri.fromFile(source))
            val decoded = android.graphics.BitmapFactory.decodeFile(copy.absolutePath)
            org.junit.Assert.assertTrue(maxOf(decoded.width, decoded.height) <= 1920)
            decoded.recycle()
            org.junit.Assert.assertTrue(source.exists())
            manager.setCustomGlassBackground(copy.absolutePath, false)
            assertEquals(GlassBackground.CUSTOM, manager.glassAppearance.first().background)
            assertEquals(copy.absolutePath, SettingsManager(context).glassAppearance.first().customPath)
            manager.setCustomGlassBackground(copy.absolutePath, true)
            assertEquals(copy.absolutePath, manager.glassAppearance.first().adultCustomPath)
            manager.useBuiltInAdultBackground()
            assertEquals("", manager.glassAppearance.first().adultCustomPath)
            assertEquals(copy.absolutePath, manager.glassAppearance.first().customPath)
        } finally {
            manager.setGlassBackground(old.background)
            manager.setAdultTheme(old.adultTheme)
        }
    }
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
