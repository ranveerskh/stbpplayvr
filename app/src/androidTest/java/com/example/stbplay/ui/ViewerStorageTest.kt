package com.example.stbplay.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.stbplay.data.*
import com.example.stbplay.domain.model.PortalSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewerStorageTest {
    @Test fun viewerDataIsIsolatedAndOwnerRecordsArePreserved() = runBlocking {
        val manager = SettingsManager(InstrumentationRegistry.getInstrumentation().targetContext)
        val previousPortal = manager.portalSettings.first()
        val previousViewer = manager.activeViewer.first()
        val portal = PortalSettings(id = "viewer-storage-test", name = "Fixture", url = "https://fixture.invalid", mac = "00:1A:79:00:00:01")
        try {
            manager.upsertPortal(portal)
            manager.activateViewer("owner")
            manager.setFavorite("owner-record", true)
            manager.addSearchHistory("Owner query")
            manager.setPhoneMovieColumns(4)
            val kid = ViewerProfile("viewer-test-kid", "Kid", 10)
            manager.saveViewer(kid, "")
            manager.activateViewer(kid.id)
            assertTrue(manager.favoriteIds.first().isEmpty())
            assertTrue(manager.searchHistory.first().isEmpty())
            assertEquals(2, manager.phoneMovieColumns.first())
            manager.setFavorite("kid-record", true)
            manager.setPhoneMovieColumns(3)
            manager.activateViewer("owner")
            assertEquals(setOf("owner-record"), manager.favoriteIds.first())
            assertEquals(listOf("Owner query"), manager.searchHistory.first())
            assertEquals(4, manager.phoneMovieColumns.first())
            manager.activateViewer(kid.id)
            assertEquals(setOf("kid-record"), manager.favoriteIds.first())
            assertEquals(3, manager.phoneMovieColumns.first())
        } finally {
            manager.activateViewer(previousViewer.id)
            manager.deletePortal(portal.id)
            if (previousPortal.id.isNotBlank()) manager.activatePortal(previousPortal.id)
        }
    }
}
