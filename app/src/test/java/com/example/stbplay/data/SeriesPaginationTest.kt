package com.example.stbplay.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SeriesPaginationTest {
    @Test fun loadsEveryFourteenItemPageBeyondOldTwentyPageCap() = runBlocking {
        val episodes = (330 downTo 1).map { "episode$it" }
        val result = loadAllSeriesPages({ it: String -> it }) { page ->
            SeriesPage(episodes.drop((page - 1) * 14).take(14), episodes.size)
        }
        assertEquals(episodes, result)
    }
    @Test fun skipsAliasedFirstPagesAndDoesNotTrustPageSizeAsTotal() = runBlocking {
        val result = loadAllSeriesPages({ it: Int -> "$it" }) { page ->
            SeriesPage(when (page) { 1, 2 -> (28 downTo 15).toList(); 3 -> (14 downTo 1).toList(); else -> emptyList() }, 14)
        }
        assertEquals((28 downTo 1).toList(), result)
    }
    @Test fun zeroBasedFallbackAndNonPagedEndpointRemainSupported() = runBlocking {
        val zero = loadAllSeriesPages({ it: Int -> "$it" }) { page -> SeriesPage(if (page == 0) listOf(1, 2) else emptyList()) }
        assertEquals(listOf(1, 2), zero)
        var calls = 0
        val full = loadAllSeriesPages({ it: Int -> "$it" }) { calls++; SeriesPage((1..35).toList()) }
        assertEquals(35, full.size); assertEquals(5, calls)
    }
    @Test fun trueZeroBasedNonEmptyPageOneDoesNotLoseFirstFourteenEpisodes() = runBlocking {
        val all = (42 downTo 1).toList()
        val result = loadAllSeriesPages({ it: Int -> "$it" }) { page ->
            SeriesPage(all.drop(page * 14).take(14), 42)
        }
        assertEquals(all, result)
    }
    @Test fun laterPageFailureDoesNotReturnSilentFourteenEpisodePartialList() = runBlocking {
        var failed = false
        try { loadAllSeriesPages({ it: Int -> "$it" }) { page ->
            if (page > 1) error("HTTP timeout") else SeriesPage((1..14).toList(), 50)
        } } catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
    }
}
