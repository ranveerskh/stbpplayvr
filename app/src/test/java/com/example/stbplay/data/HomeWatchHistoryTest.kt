package com.example.stbplay.data

import com.example.stbplay.data.model.PortalStream
import org.junit.Assert.*
import org.junit.Test

class HomeWatchHistoryTest {
    private fun stream(id: String, type: String) = PortalStream(id, id, null, null, type)
    @Test fun newestWatchWinsRegardlessOfPercentageAndEpisodesGroupUnderSeries() {
        val vod = listOf(stream("old", "movie"), stream("new", "movie"), stream("show", "series")).associateBy { it.id }
        val episodes = mapOf("e4" to SeriesHistoryEntry("show", "e4", "Episode 4", "s1"), "e5" to SeriesHistoryEntry("show", "e5", "Episode 5", "s1"))
        val rows = projectHomeWatchHistory(listOf(WatchProgress("old", .8f, 1), WatchProgress("e4", .7f, 2),
            WatchProgress("new", .1f, 4), WatchProgress("e5", .15f, 5), WatchProgress("live", .001f, 3)),
            episodes, vod, mapOf("live" to stream("live", "live")))
        assertEquals(listOf("show", "new", "live", "old"), rows.map { it.stream.id })
        assertEquals("e5", rows.first().episode!!.episodeId)
        assertEquals(.15f, rows.first().progress.fraction, .001f)
    }
    @Test fun hiddenUnavailableFinishedAndEmptyHistoryDoNotProduceCards() {
        val history = listOf(WatchProgress("hidden", .2f, 4), WatchProgress("done", .95f, 3), WatchProgress("zero", 0f, 2))
        val vod = listOf(stream("done", "movie"), stream("zero", "movie")).associateBy { it.id }
        assertTrue(projectHomeWatchHistory(history, emptyMap(), vod, emptyMap()).isEmpty())
        assertTrue(projectHomeWatchHistory(emptyList(), emptyMap(), vod, emptyMap()).isEmpty())
    }
    @Test fun homePreviewDoesNotTruncateFullHistory() {
        val vod = (1..30).associate { "$it" to stream("$it", "movie") }
        val all = projectHomeWatchHistory((1..30).map { WatchProgress("$it", .1f, it.toLong()) }, emptyMap(), vod, emptyMap())
        assertEquals(30, all.size)
        assertEquals("30", all.first().stream.id)
        assertEquals("21", all.take(10).last().stream.id)
        assertEquals("1", all.last().stream.id)
    }
}
