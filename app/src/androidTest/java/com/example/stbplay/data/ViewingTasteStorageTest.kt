package com.example.stbplay.data

import androidx.test.platform.app.InstrumentationRegistry
import com.example.stbplay.domain.model.PortalSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ViewingTasteStorageTest {
    @Test fun preferenceSummarySurvivesCompletionIsPortalScopedAndClearable() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = SettingsManager(context)
        val original = settings.portalSettings.first()
        val fixture = PortalSettings(id = "taste-fixture", name = "Taste", url = "http://taste.invalid/c/", mac = "00:1A:79:00:00:63")
        val other = fixture.copy(id = "taste-other")
        try {
            settings.upsertPortal(fixture)
            val identity = settings.viewingTasteSnapshot.first().first
            val signal = ViewingTaste("movie", "movie", "punjabi", null, "Comedy", "Punjabi", 1, 1)
            settings.recordViewingTaste(signal, true, identity)
            settings.recordViewingTaste(signal.copy(updatedAt = 2, completed = true), false, identity)
            settings.saveProgress("movie", 96_000, 100_000)
            val reloaded = SettingsManager(context)
            assertTrue(reloaded.vodProgress.first().isEmpty())
            assertTrue(reloaded.viewingTasteSnapshot.first().second.single().completed)
            assertEquals(1, reloaded.viewingTasteSnapshot.first().second.single().sessions)
            settings.upsertPortal(other)
            assertTrue(reloaded.viewingTasteSnapshot.first().second.isEmpty())
            // A delayed callback from an old profile must not leak into the current one.
            settings.recordViewingTaste(signal, true, identity)
            assertTrue(reloaded.viewingTasteSnapshot.first().second.isEmpty())
            settings.activatePortal(fixture.id)
            assertEquals("Punjabi", reloaded.viewingTasteSnapshot.first().second.single().language)
            settings.clearWatchHistory()
            assertTrue(reloaded.viewingTasteSnapshot.first().second.isEmpty())
        } finally {
            settings.deletePortal(other.id); settings.deletePortal(fixture.id)
            if (original.id.isNotBlank()) settings.activatePortal(original.id)
        }
    }
}
