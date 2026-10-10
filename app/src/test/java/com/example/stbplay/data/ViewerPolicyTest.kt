package com.example.stbplay.data
import com.example.stbplay.data.model.PortalCategory
import com.example.stbplay.data.model.PortalStream
import org.junit.Assert.*
import org.junit.Test
class ViewerPolicyTest {
    @Test fun entryPinRespectsKidsAndPinMode() {
        assertFalse(ViewerProfile("k", "Kid", 10, pinHash = "legacy").needsEntryPin)
        assertTrue(ViewerProfile("a", "Adult", 30).needsEntryPin)
        assertFalse(ViewerProfile("a", "Adult", 30, pinMode = ViewerPinMode.RESTRICTED_ONLY).needsEntryPin)
    }
    @Test fun restrictedPinCoversFlagsLocksAndAdultCategoriesOnly() {
        val profile = ViewerProfile("a", "Adult", 30, pinMode = ViewerPinMode.RESTRICTED_ONLY)
        val plain = PortalStream("normal", "Example", null, "general", "live")
        assertFalse(profile.requiresRestrictedPin(plain, false))
        assertTrue(profile.requiresRestrictedPin(plain.copy(isLocked = true), false))
        assertTrue(profile.requiresRestrictedPin(plain.copy(rating = "18+"), false))
        assertTrue(profile.requiresRestrictedPin(plain, true))
        assertFalse(profile.copy(pinMode = ViewerPinMode.PROFILE_ENTRY).requiresRestrictedPin(plain, true))
        assertFalse(profile.copy(age = 10).requiresRestrictedPin(plain, true))
    }
    @Test fun sharedEntryDoesNotGrantPortalAdministration() {
        val shared = ViewerProfile("shared", "Adult", 30, pinMode = ViewerPinMode.RESTRICTED_ONLY)
        assertTrue(shared.needsOwnerForPortalChanges)
        assertFalse(shared.copy(id = "owner").needsOwnerForPortalChanges)
        assertFalse(shared.copy(age = 10).needsOwnerForPortalChanges)
        assertFalse(shared.copy(pinMode = ViewerPinMode.PROFILE_ENTRY).needsOwnerForPortalChanges)
    }
    @Test fun ageBoundary() { assertTrue(ViewerProfile("a", "a", 17).isKids); assertFalse(ViewerProfile("a", "a", 18).isKids) }
    @Test fun pinsUseSaltAndRejectIncorrectValues() {
        val one = encodeViewerPin("1234"); val two = encodeViewerPin("1234")
        assertNotEquals(one, two); assertTrue(verifyViewerPin("1234", one)); assertFalse(verifyViewerPin("9876", one)); assertFalse(verifyViewerPin("1234", "bad"))
    }
    @Test fun kidsUnknownAndAdultFlagsAreDenied() {
        val safe = PortalStream("a", "Example", null, "kids", "movie")
        assertTrue(isKidsContentAllowed(safe, setOf("kids")))
        assertFalse(isKidsContentAllowed(safe.copy(categoryId = "unknown"), setOf("kids")))
        assertFalse(isKidsContentAllowed(safe.copy(rating = "18+"), setOf("kids")))
        assertFalse(isKidsContentAllowed(safe.copy(isLocked = true), setOf("kids")))
    }
    @Test fun adultCategoryCannotBeKidsCategory() {
        assertTrue(isKidsCategory(PortalCategory("a", "EN | Kids", "vod")))
        assertFalse(isKidsCategory(PortalCategory("a", "Kids 18+", "vod")))
        assertFalse(isKidsCategory(PortalCategory("a", "Movies", "vod")))
    }
}
