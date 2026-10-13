package com.example.stbplay.ui

import androidx.compose.ui.focus.FocusRequester
import org.junit.Assert.*
import org.junit.Test

class TvFocusNavigationTest {
    @Test fun attachedTargetFocusLossIsRepairableButIntentionalRailNavigationWins() {
        var railCalls = 0
        val navigation = TvFocusNavigation({ railCalls++ }, {})
        val target = FocusRequester()
        navigation.attach("live:channel", target); navigation.focused("live:channel", target)
        navigation.blurred(target)
        assertFalse(navigation.pending); assertFalse(navigation.hasPageFocus)
        navigation.restorePage(); assertTrue(navigation.pending)
        assertSame(target, navigation.restoreTarget)
        navigation.returnToRail(); assertTrue(navigation.railOwnsFocus); assertFalse(navigation.pending)
        navigation.restorePage(); assertFalse(navigation.pending); assertEquals(1, railCalls)
        navigation.enterPage(); assertTrue(navigation.pending); assertFalse(navigation.railOwnsFocus)
    }
    @Test fun explicitPageEntryUsesRememberedChannelInsteadOfLastFocusedCategory() {
        val navigation = TvFocusNavigation({}, {})
        val category = FocusRequester(); val channel = FocusRequester()
        navigation.attach("category:all", category); navigation.attach("live:channel25", channel)
        navigation.setEntry("category:all")
        navigation.focused("category:all", category)
        navigation.setEntry("live:channel25")
        navigation.blurred(category); navigation.restorePage()
        assertSame(category, navigation.restoreTarget)
        navigation.enterPage()
        assertSame(channel, navigation.restoreTarget)
        navigation.restorePage() // A pending load/entry must not become a stale-focus recovery.
        assertSame(channel, navigation.restoreTarget)
    }
    @Test fun replacingLoadingCategoryTargetCannotLoseThePendingHandover() {
        val navigation = TvFocusNavigation({ error("Unexpected rail bounce") }, {})
        val old = FocusRequester(); val replacement = FocusRequester()
        navigation.setEntry("category:all")
        navigation.attach("category:all", old)
        navigation.focused("category:all", old)
        navigation.attach("category:all", replacement)
        navigation.detach("category:all", old)
        assertTrue(navigation.pending)
        assertSame(replacement, navigation.targets[navigation.entryId])
        navigation.focused("category:all", replacement)
        assertFalse(navigation.pending)
    }
    @Test fun disposingOldTargetCannotStealFocusFromAlreadyFocusedReplacement() {
        val navigation = TvFocusNavigation({}, {})
        val old = FocusRequester(); val replacement = FocusRequester()
        navigation.attach("settings:back", old); navigation.focused("settings:back", old)
        navigation.attach("settings:back", replacement); navigation.focused("settings:back", replacement)
        navigation.detach("settings:back", old)
        assertFalse(navigation.pending)
        assertEquals("settings:back", navigation.focusedId)
    }
}
