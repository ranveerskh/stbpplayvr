package com.example.stbplay.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.domain.model.PortalSettings
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CatalogueProjectionsTest {
    @get:Rule val rule = createComposeRule()
    private fun stream(id: String, type: String = "movie", language: String? = null, searchText: String? = null) =
        PortalStream(id, "$type $id", null, null, type, language = language, searchText = searchText)

    @Test fun focusOnlyUpdatesReuseSearchAndFavouriteObjects() {
        val vod = (0 until 20_000).map { stream("$it", language = "Punjabi") }
        val favorites = vod.take(80)
        var focus by mutableIntStateOf(0)
        var search: List<PortalStream> = emptyList()
        var mapped: List<UiMedia> = emptyList()
        var mappingCalls = 0
        var rootRuns = 0
        rule.setContent {
            // Read the changing focus in this scope, as the app root does.
            val currentFocus = focus
            val nextSearch = rememberSearchCatalog(StbPlayTab.CONTENT, emptyList(), vod, favorites, "Punjabi")
            val nextMapped = rememberFavouriteUiItems(favorites, emptyMap(), emptySet(), PortalSettings(), 0, null, "") {
                mappingCalls++
                UiMedia(it.id, it.name)
            }
            SideEffect { search = nextSearch; mapped = nextMapped; rootRuns++ }
            Text("Focus $currentFocus")
        }
        rule.waitForIdle()
        val originalSearch = search
        val originalMapped = mapped
        repeat(20) {
            rule.runOnIdle { focus++ }
            rule.waitForIdle()
            assertSame(originalSearch, search)
            assertSame(originalMapped, mapped)
        }
        assertEquals(20_000, search.size)
        assertEquals(favorites.size, mappingCalls)
        assertEquals(21, rootRuns)
    }

    @Test fun searchTracksLanguageFilteredInputsTabAndAdditionalPages() {
        val punjabi = stream("same", language = "PUNJABI")
        val metadataMatch = stream("metadata", searchText = "Language: Punjabi")
        val english = stream("english", language = "English")
        val live = listOf(stream("same", "live"))
        var vod by mutableStateOf(listOf(punjabi, metadataMatch, english))
        var favorites by mutableStateOf(listOf(english))
        var language by mutableStateOf("Punjabi")
        var tab by mutableStateOf(StbPlayTab.CONTENT)
        var result: List<PortalStream> = emptyList()
        rule.setContent {
            val projected = rememberSearchCatalog(tab, live, vod, favorites, language)
            SideEffect { result = projected }
        }
        rule.waitForIdle()
        assertEquals(listOf(punjabi, metadataMatch), result)
        rule.runOnIdle { language = "All" }; rule.waitForIdle()
        assertEquals(vod, result)
        val nextPage = (0 until 10_000).map { stream("page-$it", "series", "Punjabi") }
        rule.runOnIdle { vod = vod + nextPage }; rule.waitForIdle()
        assertEquals(10_003, result.size)
        assertSame(nextPage.last(), result.last())
        // Root parental filtering supplies changed safe inputs; old entries must disappear.
        rule.runOnIdle { vod = listOf(english) }; rule.waitForIdle()
        assertEquals(listOf(english), result)
        rule.runOnIdle { tab = StbPlayTab.LIVE }; rule.waitForIdle()
        assertSame(live, result)
        rule.runOnIdle { tab = StbPlayTab.HOME }; rule.waitForIdle()
        assertEquals(live + vod, result)
        rule.runOnIdle { tab = StbPlayTab.SETTINGS }; rule.waitForIdle()
        assertEquals(live + vod, result)
        rule.runOnIdle { tab = StbPlayTab.FAVOURITES }; rule.waitForIdle()
        assertSame(favorites, result)
        rule.runOnIdle { favorites = emptyList() }; rule.waitForIdle()
        assertTrue(result.isEmpty())
    }

    @Test fun favouriteMappingRefreshesProgressFlagsPortalAndArtworkSession() {
        var streams by mutableStateOf(listOf(stream("movie")))
        var progress by mutableStateOf(emptyMap<String, Float>())
        var favoriteIds by mutableStateOf(emptySet<String>())
        var portal by mutableStateOf(PortalSettings(id = "a", url = "https://a.test", mac = "02:00:00:00:00:01"))
        var generation by mutableIntStateOf(0)
        var token by mutableStateOf<String?>(null)
        var cookie by mutableStateOf("")
        var result: List<UiMedia> = emptyList()
        var mappingCalls = 0
        rule.setContent {
            val mapped = rememberFavouriteUiItems(streams, progress, favoriteIds, portal, generation, token, cookie) {
                mappingCalls++
                UiMedia(it.id, it.name, imageUrl = "${portal.url}/${it.id}",
                    imageHeaders = mapOf("Authorization" to token.orEmpty(), "Cookie" to cookie, "MAC" to portal.mac),
                    progress = progress[it.id] ?: 0f, isFavorite = it.id in favoriteIds,
                    subtitle = "${portal.id}/$generation")
            }
            SideEffect { result = mapped }
        }
        rule.waitForIdle()
        fun change(block: () -> Unit) {
            val previous = result
            rule.runOnIdle(block); rule.waitForIdle()
            assertNotSame(previous, result)
        }
        change { progress = mapOf("movie" to 0.4f) }
        assertEquals(0.4f, result.single().progress, 0f)
        change { favoriteIds = setOf("movie") }
        assertTrue(result.single().isFavorite)
        change { token = "refreshed" }
        assertEquals("refreshed", result.single().imageHeaders["Authorization"])
        change { cookie = "session=new" }
        assertEquals("session=new", result.single().imageHeaders["Cookie"])
        change { portal = portal.copy(url = "https://b.test") }
        assertEquals("https://b.test/movie", result.single().imageUrl)
        change { portal = portal.copy(mac = "02:00:00:00:00:02") }
        assertEquals(portal.mac, result.single().imageHeaders["MAC"])
        change { portal = portal.copy(id = "b") }
        assertEquals("b/0", result.single().subtitle)
        change { generation++ }
        assertEquals("b/1", result.single().subtitle)
        change { streams = listOf(stream("new", "series")) }
        assertEquals("new", result.single().id)
        assertEquals(10, mappingCalls)
    }
}
