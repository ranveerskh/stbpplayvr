package com.example.stbplay.ui

import androidx.compose.ui.graphics.Color
import com.example.stbplay.ui.theme.LightPalette
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class IvoryContrastTest {
    private fun luminance(color: Color): Double {
        fun linear(value: Float): Double = if (value <= 0.04045f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
    private fun check(text: Color, background: Color) {
        val a = luminance(text); val b = luminance(background)
        assertTrue("Contrast must be >= 4.5:1", (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05) >= 4.5)
    }
    @Test fun heroAndFocusedActionsRemainReadable() {
        check(LightPalette.text, LightPalette.panelSoft)
        check(LightPalette.muted, LightPalette.panelSoft)
        check(LightPalette.onAccent, LightPalette.focusedAccent)
    }
    @Test fun warningAndPlaceholderTextRemainReadable() {
        check(LightPalette.text, Color(0xFFFFF3D6))
        check(LightPalette.accentLight, Color(0xFFFFF3D6))
        check(LightPalette.accentLight, LightPalette.panelSoft)
    }
}
