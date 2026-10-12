package com.example.stbplay.ui

import org.junit.Assert.*
import org.junit.Test

class UiScalePolicyTest {
    @Test fun phonesAndTabletsStayStatic() {
        assertTrue(useStaticUiScale(false, "Samsung", "Samsung", "SM-A510"))
        assertTrue(useStaticUiScale(false, "Google", "Google", "Pixel Tablet"))
    }
    @Test fun questKeepsExistingControllerScale() {
        assertFalse(useStaticUiScale(false, "Oculus", "oculus", "Quest 2"))
        assertFalse(useStaticUiScale(false, "Meta", "Meta", "Quest 3"))
    }
    @Test fun televisionAlwaysStaysStatic() {
        assertTrue(useStaticUiScale(true, "Generic", "Box", "Android TV"))
        assertTrue(useStaticUiScale(true, "Meta", "Meta", "Quest"))
    }
}
