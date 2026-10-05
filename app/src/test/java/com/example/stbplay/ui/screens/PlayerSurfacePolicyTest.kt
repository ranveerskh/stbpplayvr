package com.example.stbplay.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSurfacePolicyTest {
    @Test
    fun phoneKeepsTextureViewEvenWhenCompatibilityIsEnabled() {
        assertTrue(
            shouldPreferTextureSurface(
                compactLayout = true,
                isAndroidTv = false,
                isMetaQuest = false,
                hasTouchscreen = true,
                androidBoxVideoCompatibility = true
            )
        )
    }

    @Test
    fun televisionKeepsTextureViewByDefaultAndUsesSurfaceViewOnlyWhenOptedIn() {
        val common = mapOf(
            "compactLayout" to false,
            "isAndroidTv" to true,
            "isMetaQuest" to false,
            "hasTouchscreen" to false
        )
        assertTrue(
            shouldPreferTextureSurface(
                compactLayout = common.getValue("compactLayout") as Boolean,
                isAndroidTv = common.getValue("isAndroidTv") as Boolean,
                isMetaQuest = common.getValue("isMetaQuest") as Boolean,
                hasTouchscreen = common.getValue("hasTouchscreen") as Boolean,
                androidBoxVideoCompatibility = false
            )
        )
        assertFalse(
            shouldPreferTextureSurface(
                compactLayout = false,
                isAndroidTv = true,
                isMetaQuest = false,
                hasTouchscreen = false,
                androidBoxVideoCompatibility = true
            )
        )
    }

    @Test
    fun touchlessAndroidBoxCanSelectSurfaceViewWithoutLeanbackFeature() {
        assertTrue(
            shouldPreferTextureSurface(
                compactLayout = true,
                isAndroidTv = false,
                isMetaQuest = false,
                hasTouchscreen = false,
                androidBoxVideoCompatibility = false
            )
        )
        assertFalse(
            shouldPreferTextureSurface(
                compactLayout = true,
                isAndroidTv = false,
                isMetaQuest = false,
                hasTouchscreen = false,
                androidBoxVideoCompatibility = true
            )
        )
    }

    @Test
    fun questKeepsExistingTextureViewChoiceWhenCompatibilityIsEnabled() {
        assertTrue(
            shouldPreferTextureSurface(
                compactLayout = true,
                isAndroidTv = false,
                isMetaQuest = true,
                hasTouchscreen = false,
                androidBoxVideoCompatibility = true
            )
        )
    }
}
