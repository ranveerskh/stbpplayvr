package com.example.stbplay.data

import com.example.stbplay.domain.model.PortalSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderPairingPortalTest {
    private val mac = "02:11:22:33:44:55"
    private val manual = PortalSettings(id = "manual", url = "https://manual.example", mac = mac)

    @Test fun firstInstallPairingCreatesTheDraftTargetWithoutASavedUrl() {
        val target = resolvePairingPortal("draft", mac, emptyList(), PortalSettings())
        assertEquals("draft", target.id)
        assertEquals(mac, target.mac)
        assertEquals("", target.url)
        val assigned = target.copy(url = "https://provider.example").normalized().withStableId()
        assertEquals("draft", assigned.id)
        assertEquals("https://provider.example", assigned.url)
    }

    @Test fun newPairingDoesNotOverwriteAnUnrelatedActivePortal() {
        val target = resolvePairingPortal("draft", mac, listOf(manual), manual)
        assertEquals("draft", target.id)
        assertEquals("", target.url)
        assertEquals("https://manual.example", manual.url)
    }

    @Test fun existingPairingUsesItsProfileEvenWhenAnotherPortalIsActive() {
        val paired = manual.copy(id = "paired", name = "Existing")
        assertEquals(paired, resolvePairingPortal("paired", mac, listOf(manual, paired), manual))
    }

    @Test fun legacyPendingSessionsUseTheActiveProfile() {
        assertEquals(manual, resolvePairingPortal("", mac, listOf(manual), manual))
    }
}
