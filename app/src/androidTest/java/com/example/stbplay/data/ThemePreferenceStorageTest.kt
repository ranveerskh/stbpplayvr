package com.example.stbplay.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceStorageTest {
    @Test fun blurStepsPersistAndOnlyBackgroundBitmapIsTransformed() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = SettingsManager(context)
        val old = manager.glassAppearance.first()
        val theme = manager.themePreference.first()
        val bitmap = android.graphics.Bitmap.createBitmap(128, 64, android.graphics.Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) for (x in 0 until 128) bitmap.setPixel(x, y, if (x < 64) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        try {
            for (value in listOf(0, 50, 100)) {
                manager.setGlassBlur(value)
                assertEquals(value, SettingsManager(context).glassAppearance.first().blurPercent)
                assertEquals(theme, manager.themePreference.first())
                val result = com.example.stbplay.ui.theme.StaticBackgroundBlur(value).transform(bitmap, coil.size.Size.ORIGINAL)
                assertEquals(android.graphics.Color.BLACK, bitmap.getPixel(63, 32))
                if (value == 0) org.junit.Assert.assertSame(bitmap, result)
                else {
                    org.junit.Assert.assertTrue(result.getPixel(63, 32) != bitmap.getPixel(63, 32))
                    org.junit.Assert.assertTrue(maxOf(result.width, result.height) <= 512)
                    result.recycle()
                }
            }
            org.junit.Assert.assertTrue(runCatching { manager.setGlassBlur(75) }.isFailure)
        } finally { bitmap.recycle(); manager.setGlassBlur(old.blurPercent) }
    }

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
