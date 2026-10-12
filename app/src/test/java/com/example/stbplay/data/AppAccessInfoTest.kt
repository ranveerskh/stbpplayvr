package com.example.stbplay.data

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class AppAccessInfoTest {
    private val start = 1_000_000L
    private val year = TimeUnit.DAYS.toMillis(365)

    @Test fun manualUserTrialUsesOriginalInstallDate() {
        val result = appAccessInfo(PlatformLicenseDetails(), false, start, start + year - 1)
        assertEquals(start + year, result.expiresAtMillis)
        assertEquals("Free 1-year app trial", result.name)
        assertEquals("ACTIVE", appAccessInfo(PlatformLicenseDetails(), false, start, start + TimeUnit.DAYS.toMillis(31)).status)
        assertEquals("ACTIVE", result.status)
        assertEquals("EXPIRED", appAccessInfo(PlatformLicenseDetails(), false, start, start + year).status)
    }

    @Test fun expiredProviderTrialNeverFallsBackToLocalYear() {
        val license = PlatformLicenseDetails("Trial", hasKey = true, trial = true, trialExpiresAtMillis = start + 10)
        val result = appAccessInfo(license, true, start, start + 11)
        assertEquals("EXPIRED", result.status)
        assertEquals(start + 10, result.expiresAtMillis)
        assertTrue(result.name.startsWith("Provider trial"))
    }

    @Test fun providerPaidPlanWinsOverLocalTrial() {
        val result = appAccessInfo(PlatformLicenseDetails("Customer", start + year, true), true, start, start)
        assertTrue(result.name.startsWith("Provider yearly plan"))
        assertEquals(start + year, result.expiresAtMillis)
    }

    @Test fun assignedDeviceWithMissingLicenseDoesNotReceiveExtraTrial() {
        assertEquals("DETAILS UNAVAILABLE", appAccessInfo(PlatformLicenseDetails(), true, start, start).status)
    }
}
