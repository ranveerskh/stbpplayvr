package com.example.stbplay.data

import com.example.stbplay.data.model.PortalStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.random.Random

class CatalogStreamLookupTest {
    private fun stream(id: String, type: String, name: String = "$type $id") =
        PortalStream(id, name, null, null, type)

    private fun lookup(id: String, type: String, sources: List<List<PortalStream>>, categories: List<PortalStream> = emptyList()) =
        findCatalogStream(id, type, sources[0], sources[1], sources[2], sources[3], sources[4], categories.asSequence())

    @Test fun identicalPortalIdsKeepTheirStreamTypes() {
        val live = stream("same", "live")
        val movie = stream("same", "movie")
        val series = stream("same", "series")
        val sources = listOf(listOf(live), listOf(movie), listOf(series), emptyList(), emptyList())
        assertSame(live, lookup("same", "live", sources))
        assertSame(movie, lookup("same", "movie", sources))
        assertSame(series, lookup("same", "series", sources))
    }

    @Test fun firstMatchKeepsTheExistingSourcePrecedenceAndOrder() {
        val copies = (0..5).map { stream("target", "movie", "Source $it") }
        val sources = copies.take(5).map { listOf(it, it.copy(name = "Later duplicate")) }
        for (firstSource in sources.indices) {
            val remaining = sources.mapIndexed { index, items -> if (index < firstSource) emptyList() else items }
            assertSame(copies[firstSource], lookup("target", "movie", remaining, listOf(copies[5])))
        }
        assertSame(copies[5], lookup("target", "movie", List(5) { emptyList() }, listOf(copies[5])))
    }

    @Test fun earlyMatchesDoNotReadLaterListsOrCategoryFallback() {
        val live = stream("target", "live")
        val unread = object : AbstractList<PortalStream>() {
            override val size: Int get() = error("Later source was read")
            override fun get(index: Int): PortalStream = error("Later source was read")
        }
        val unreadCategories = sequence<PortalStream> { error("Fallback was read") }
        assertSame(live, findCatalogStream("target", "live", listOf(live), unread, unread, unread, unread, unreadCategories))
    }

    @Test fun categoryFallbackAndMissingEntriesRemainSupported() {
        val movie = stream("category-only", "movie")
        val categories = listOf(stream("category-only", "live"), movie, movie.copy(name = "Later"))
        assertSame(movie, lookup(movie.id, movie.streamType, List(5) { emptyList() }, categories))
        assertNull(lookup("missing", "movie", List(5) { emptyList() }, categories))
    }

    @Test fun entriesAtTheEndOfLargeCataloguesRemainReachable() {
        val movies = (0 until 50_000).map { stream("$it", "movie") }
        assertSame(movies.last(), lookup("49999", "movie", listOf(emptyList(), movies, emptyList(), emptyList(), emptyList())))
    }

    @Test fun lookupMatchesTheReleasedAlgorithmAcrossMixedSources() {
        val random = Random(202020)
        val types = listOf("live", "movie", "series")
        repeat(100) {
            val sources = List(5) { source ->
                List(random.nextInt(0, 30)) { index -> stream("${random.nextInt(12)}", types.random(random), "$source/$index") }
            }
            val categories = List(20) { stream("${random.nextInt(12)}", types.random(random), "category/$it") }
            for (id in 0..12) for (type in types) {
                val expected = (sources[0] + sources[1] + sources[2] + sources[3] + sources[4])
                    .firstOrNull { it.id == "$id" && it.streamType == type }
                    ?: categories.firstOrNull { it.id == "$id" && it.streamType == type }
                assertEquals(expected, lookup("$id", type, sources, categories))
            }
        }
    }
}
