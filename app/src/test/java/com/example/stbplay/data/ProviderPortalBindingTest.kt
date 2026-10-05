package com.example.stbplay.data

import com.example.stbplay.domain.model.PortalSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderPortalBindingTest {
    private val paired = PortalSettings(id = "paired", url = "https://paired.example/portal")
    private val manual = PortalSettings(id = "manual", url = "https://manual.example/portal")

    @Test fun providerUrlChangesStayBoundToThePairedProfile() {
        assertEquals("paired", resolveProviderPortalId("paired", "https://new.example/portal", listOf(manual, paired)))
    }

    @Test fun removingAPairedProfileCannotRedirectSyncToAManualProfile() {
        assertNull(resolveProviderPortalId("paired", manual.url, listOf(manual)))
    }

    @Test fun legacyTokenOnlyMigratesWhenOneProfileMatchesItsUrl() {
        assertEquals("paired", resolveProviderPortalId(null, paired.url + "/", listOf(manual, paired)))
        assertNull(resolveProviderPortalId(null, "https://new.example/portal", listOf(manual, paired)))
        assertNull(resolveProviderPortalId(null, paired.url, listOf(paired, paired.copy(id = "duplicate"))))
    }
}
