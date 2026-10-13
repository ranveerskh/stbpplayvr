package com.example.stbplay.data

import com.example.stbplay.data.model.PortalStream
import org.junit.Assert.*
import org.junit.Test

class HomeRecommendationsTest {
    private fun movie(id: String, category: String, genre: String? = null, language: String? = null) =
        PortalStream(id, id, null, category, "movie", genre = genre, language = language)

    @Test fun ranksFullCatalogueByActualFrequencyAndLatestTwoTitles() {
        val unrelated = (1..1300).map { movie("other$it", "other") }
        val matching = (1..12).map { movie("match$it", "punjabi", "Comedy, Drama", "Punjabi") }
        val taste = listOf(movie("watched", "punjabi", "Comedy", "Punjabi").viewingTaste(10).copy(sessions = 5))
        val result = recommendHome(unrelated + matching, taste, emptySet())
        assertEquals(10, result.size)
        assertEquals("match1", result.first().id)
        assertTrue(result.count { it.categoryId == "punjabi" } >= 7)
        assertTrue(result.any { it.categoryId == "other" })
        val recent = movie("latest", "recent", "Mystery", "Hindi").viewingTaste(20)
        assertEquals("recent-choice", recommendHome(listOf(movie("random", "other"),
            movie("recent-choice", "recent", "Mystery", "Hindi")), taste + recent, emptySet()).first().id)
    }

    @Test fun completedMoviesAndUnfinishedTitlesAreExcludedSeriesIsOneTitleAndMissingMetadataWorks() {
        val done = movie("done", "a").viewingTaste(20, true)
        val catalogue = listOf(movie("done", "a"), movie("continue", "a"),
            movie("new", "a"), PortalStream("show", "show", null, "b", "series"))
        assertEquals(setOf("new", "show"), recommendHome(catalogue, listOf(done), setOf("continue")).map { it.id }.toSet())
        assertEquals(10, recommendHome((1..20).map { PortalStream("$it", "$it", null, null, "movie") }, emptyList(), emptySet()).size)
        assertTrue(recommendHome(emptyList(), listOf(done), emptySet()).isEmpty())
    }

    @Test fun completedTasteSurvivesResumeRemovalAndRepeatSessionsDoNotCountProgressTicks() {
        val signal = movie("one", "a").viewingTaste(1)
        val once = updateViewingTaste(emptyList(), signal, true)
        val finished = updateViewingTaste(once, signal.copy(updatedAt = 2, completed = true), false)
        assertEquals(1, finished.single().sessions)
        assertTrue(finished.single().completed)
        assertEquals(2, updateViewingTaste(finished, signal.copy(updatedAt = 3), true).single().sessions)
    }

    @Test fun sessionRequiresActualPlaybackNotResumeOffsetSeekOrRepeatedPauseCallbacks() {
        val tracker = ViewingSessionTracker()
        assertNull(tracker.observe(600_000, 1_000_000, 0))
        assertNull(tracker.observe(900_000, 1_000_000, 5_000))
        repeat(5) { assertNull(tracker.observe(900_000, 1_000_000, (it + 2) * 5_000L)) }
        val playing = ViewingSessionTracker()
        assertNull(playing.observe(0, 600_000, 0))
        for (tick in 1..23) assertNull(playing.observe(tick * 5_000L, 600_000, tick * 5_000L))
        assertEquals(true to false, playing.observe(120_000, 600_000, 120_000))
        assertNull(playing.observe(125_000, 600_000, 125_000))
        assertEquals(false to true, playing.observe(575_000, 600_000, 130_000))
        assertNull(playing.observe(580_000, 600_000, 135_000))
    }

    @Test fun finishingLastMinuteOnResumeRetainsEarlierQualifiedWatchingWithoutAnotherSession() {
        val tracker = ViewingSessionTracker(previouslyQualified = true)
        assertNull(tracker.observe(940_000, 1_000_000, 0))
        assertEquals(false to true, tracker.observe(950_000, 1_000_000, 10_000))
        assertNull(tracker.observe(955_000, 1_000_000, 15_000))
    }
}
