package com.example.stbplay.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalSettingsMacTest {
    @Test
    fun androidIdProducesStableDeviceSpecificLocallyAdministeredMac() {
        val first = generateStbPlayMac("android-id-device-a")
        val repeated = generateStbPlayMac("android-id-device-a")
        val otherDevice = generateStbPlayMac("android-id-device-b")

        assertEquals(first, repeated)
        assertNotEquals(first, otherDevice)
        assertTrue(Regex("^02(:[0-9A-F]{2}){5}$").matches(first))
    }
}
