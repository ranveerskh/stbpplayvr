package com.example.stbplay.data

import org.junit.Assert.*
import org.junit.Test

class FreeDramaCatalogTest {
    @Test fun completeOfficialPlaylistIsOrderedAndUniqueBeyondFourteen() {
        assertEquals((1..39).toList(), FreeDramaCatalog.episodes.map { it.number })
        assertEquals(39, FreeDramaCatalog.episodes.map { it.videoId }.distinct().size)
        assertTrue(FreeDramaCatalog.episodes.all { it.videoId.matches(Regex("[A-Za-z0-9_-]{11}")) })
        assertEquals("gabuP8xlpzc", FreeDramaCatalog.episode(15)?.videoId)
        assertEquals("1chlB7wVwpE", FreeDramaCatalog.episode(39)?.videoId)
        assertNull(FreeDramaCatalog.episode(0))
        assertNull(FreeDramaCatalog.episode(40))
    }
    @Test fun officialPlayerHasInstalledAppOriginAndUnmodifiedControls() {
        val html = FreeDramaCatalog.playerHtml(FreeDramaCatalog.episodes.first(), "com.example.stbplay.portaltest")
        assertTrue(html.contains("https://www.youtube.com/iframe_api"))
        assertTrue(html.contains("origin:'https://com.example.stbplay.portaltest'"))
        assertTrue(html.contains("controls:1"))
        assertFalse(html.contains("autoplay:1"))
        assertTrue(html.contains("onError"))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectAnUncuratedVideo() {
        FreeDramaCatalog.playerHtml(FreeDramaEpisode(1, "notapproved"), "com.example.stbplay.portaltest")
    }
}
