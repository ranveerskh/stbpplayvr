package com.example.stbplay.data

import androidx.test.platform.app.InstrumentationRegistry
import com.example.stbplay.domain.model.PortalSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SeriesHistoryStorageTest {
    @Test fun episodeIdentitySurvivesReloadAndHistoryRemovalKeepsFavourites() = runBlocking {
        val settings = SettingsManager(InstrumentationRegistry.getInstrumentation().targetContext)
        val original = settings.portalSettings.first()
        val fixture = PortalSettings(id = "series-history-fixture", name = "History", url = "http://history.invalid/c/", mac = "00:1A:79:00:00:64")
        try {
            settings.upsertPortal(fixture)
            settings.setFavorite("show", true)
            settings.saveProgress("e5", 15_000, 100_000, SeriesHistoryEntry("show", "e5", "Episode 5", "season-one"))
            val reloaded = SettingsManager(InstrumentationRegistry.getInstrumentation().targetContext)
            assertEquals("season-one", reloaded.seriesHistory.first()["e5"]!!.seasonId)
            assertEquals(.15f, reloaded.vodProgress.first()["e5"]!!, .001f)
            reloaded.markRecentlyPlayed("channel")
            reloaded.removeFromHistory("show")
            assertFalse("e5" in reloaded.vodProgress.first())
            assertTrue(reloaded.seriesHistory.first().isEmpty())
            assertTrue("show" in reloaded.favoriteIds.first())
            assertTrue("channel" in reloaded.vodProgress.first())
            reloaded.saveProgress("e6", 15_000, 100_000, SeriesHistoryEntry("show", "e6", "Episode 6", "season-one"))
            reloaded.saveProgress("e6", 96_000, 100_000, SeriesHistoryEntry("show", "e6", "Episode 6", "season-one"))
            assertTrue(reloaded.seriesHistory.first().isEmpty())
            reloaded.clearWatchHistory()
            assertTrue(reloaded.vodProgress.first().isEmpty())
            assertTrue("show" in reloaded.favoriteIds.first())
        } finally {
            settings.clearWatchHistory(); settings.setFavorite("show", false)
            settings.deletePortal(fixture.id)
            if (original.id.isNotBlank()) settings.activatePortal(original.id)
        }
    }
}
