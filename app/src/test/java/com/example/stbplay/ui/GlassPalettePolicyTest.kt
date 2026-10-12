package com.example.stbplay.ui

import com.example.stbplay.ui.theme.GlassPalette
import com.example.stbplay.ui.theme.StaticGlassPalette
import com.example.stbplay.ui.theme.glassPaletteForDevice
import org.junit.Assert.*
import org.junit.Test

class GlassPalettePolicyTest {
    @Test fun tvAndQuestUseOpaqueStaticPanels() {
        assertEquals(StaticGlassPalette, glassPaletteForDevice(true, "Generic", "Box", "TV"))
        assertEquals(StaticGlassPalette, glassPaletteForDevice(false, "Meta", "Meta", "Quest 3"))
        assertEquals(StaticGlassPalette, glassPaletteForDevice(false, "Oculus", "Oculus", "Quest 2"))
        assertEquals(1f, StaticGlassPalette.panel.alpha, 0f)
        assertEquals(1f, StaticGlassPalette.rail.alpha, 0f)
        assertEquals(1f, StaticGlassPalette.panelSoft.alpha, 0f)
    }
    @Test fun phonesKeepSubtleTransparencyAndYellowFocus() {
        assertEquals(GlassPalette, glassPaletteForDevice(false, "Google", "Google", "Pixel"))
        assertTrue(GlassPalette.panel.alpha < 1f)
        assertTrue(GlassPalette.panel.alpha > 0.9f)
        assertEquals(GlassPalette.accent, StaticGlassPalette.accent)
        assertTrue(GlassPalette.accent.red > 0.9f && GlassPalette.accent.green > 0.7f)
    }
}
