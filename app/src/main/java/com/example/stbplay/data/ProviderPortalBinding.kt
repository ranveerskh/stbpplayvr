package com.example.stbplay.data

import com.example.stbplay.domain.model.PortalSettings

/** Legacy tokens can only be associated with an unambiguous matching URL. */
internal fun resolveProviderPortalId(
    boundPortalId: String?, assignmentUrl: String, profiles: List<PortalSettings>
): String? {
    if (!boundPortalId.isNullOrBlank()) return boundPortalId.takeIf { id -> profiles.any { it.id == id } }
    val url = assignmentUrl.trim().trimEnd('/')
    return profiles.singleOrNull { it.url.trim().trimEnd('/') == url }?.id?.takeIf { it.isNotBlank() }
}
