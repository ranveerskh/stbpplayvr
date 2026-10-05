package com.example.stbplay.data

import java.util.concurrent.TimeUnit

data class AppAccessInfo(val name: String, val status: String, val expiresAtMillis: Long?)

/** Assigned licenses always take precedence, including an expired provider trial. */
fun appAccessInfo(license: PlatformLicenseDetails, providerAssigned: Boolean, trialStartedAt: Long, now: Long): AppAccessInfo {
    if (!license.hasKey && providerAssigned) return AppAccessInfo("Provider app access", "DETAILS UNAVAILABLE", null)
    if (!license.hasKey) {
        val expiry = trialStartedAt + TimeUnit.DAYS.toMillis(30)
        return AppAccessInfo("Free 30-day app trial", if (now < expiry) "ACTIVE" else "EXPIRED", expiry)
    }
    val expiry = if (license.trial) license.trialExpiresAtMillis ?: license.expiresAtMillis else license.expiresAtMillis
    val grace = license.inGrace && (license.graceUntilMillis ?: 0L) > now
    val status = when {
        grace -> "GRACE PERIOD"
        expiry != null && expiry <= now -> "EXPIRED"
        else -> "ACTIVE"
    }
    val plan = when {
        license.trial -> "Provider trial"
        providerAssigned -> "Provider yearly plan"
        else -> "Activated app plan"
    }
    return AppAccessInfo("$plan · ${license.label}", status, if (grace) license.graceUntilMillis else expiry)
}
