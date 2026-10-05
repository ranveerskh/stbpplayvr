package com.example.stbplay.data

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class AppAccessInfoTest {
    private val start = 1_000_000L
    private val month = TimeUnit.DAYS.toMillis(30)

    @Test fun manualUserTrialUsesOriginalInstallDate() {
        val result = appAccessInfo(PlatformLicenseDetails(), false, start, start + month - 1)
        assertEquals(start + month, result.expiresAtMillis)
        assertEquals("ACTIVE", result.status)
        assertEquals("EXPIRED", appAccessInfo(PlatformLicenseDetails(), false, start, start + month).status)
    }

    @Test fun expiredProviderTrialNeverFallsBackToThirtyDays() {
        val license = PlatformLicenseDetails("Trial", hasKey = true, trial = true, trialExpiresAtMillis = start + 10)
        val result = appAccessInfo(license, true, start, start + 11)
        assertEquals("EXPIRED", result.status)
        assertEquals(start + 10, result.expiresAtMillis)
        assertTrue(result.name.startsWith("Provider trial"))
    }

    @Test fun providerPaidPlanWinsOverLocalTrial() {
        val result = appAccessInfo(PlatformLicenseDetails("Customer", start + month * 12, true), true, start, start)
        assertTrue(result.name.startsWith("Provider yearly plan"))
        assertEquals(start + month * 12, result.expiresAtMillis)
    }

    @Test fun assignedDeviceWithMissingLicenseDoesNotReceiveExtraTrial() {
        assertEquals("DETAILS UNAVAILABLE", appAccessInfo(PlatformLicenseDetails(), true, start, start).status)
    }
}
