package com.example.stbplay.data

import androidx.test.platform.app.InstrumentationRegistry
import com.example.stbplay.domain.model.PortalSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class SettingsSearchHistoryTest {
    @Test fun layoutPreferencesPersistAndSearchClearCannotEraseWatchHistoryOrFavorites() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SettingsManager(context)
        val original = settings.portalSettings.first()
        val position = settings.phoneCategoryPosition.first()
        val columns = settings.phoneMovieColumns.first()
        val fixture = PortalSettings(id = "batch2-history-test", name = "Test fixture", url = "http://127.0.0.1/portal", mac = "00:1A:79:AA:BB:CC")
        try {
            settings.upsertPortal(fixture)
            settings.setPhoneCategoryPosition(CategoryDropdownPosition.BOTTOM)
            settings.setPhoneMovieColumns(4)
            settings.setFavorite("keep-film", true)
            settings.markRecentlyPlayed("keep-channel")
            settings.addSearchHistory("Film")
            settings.addSearchHistory("News")
            val reloaded = SettingsManager(context)
            assertEquals(CategoryDropdownPosition.BOTTOM, reloaded.phoneCategoryPosition.first())
            assertEquals(4, reloaded.phoneMovieColumns.first())
            assertEquals(listOf("News", "Film"), reloaded.searchHistory.first())
            reloaded.clearSearchHistory()
            assertTrue(reloaded.searchHistory.first().isEmpty())
            assertTrue("keep-film" in reloaded.favoriteIds.first())
            assertTrue(reloaded.watchHistory.first().any { it.contentId == "keep-channel" })
            assertEquals(fixture.mac, reloaded.portalSettings.first().mac)
        } finally {
            settings.setPhoneCategoryPosition(position)
            settings.setPhoneMovieColumns(columns)
            settings.setFavorite("keep-film", false)
            settings.removeFromHistory("keep-channel")
            settings.clearSearchHistory()
            settings.deletePortal(fixture.id)
            if (original.id.isNotBlank()) settings.activatePortal(original.id)
        }
    }
}
