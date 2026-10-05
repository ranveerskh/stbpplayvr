package com.example.stbplay.data

import com.example.stbplay.domain.model.PortalSettings

/**
 * Explicit pairing authorizes creating its target profile. New drafts have no
 * URL and therefore are absent from the list of saved, usable portal profiles.
 * Background provider sync uses the stricter binding resolver instead.
 */
internal fun resolvePairingPortal(
    portalId: String,
    portalMac: String,
    profiles: List<PortalSettings>,
    activePortal: PortalSettings
): PortalSettings {
    if (portalId.isBlank()) return activePortal
    return profiles.firstOrNull { it.id == portalId }
        ?: PortalSettings(id = portalId, mac = portalMac)
}
