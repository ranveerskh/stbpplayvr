@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.stbplay.data.VodCatalogBatch
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.data.updatedSearchHistory
import com.example.stbplay.isAndroidTvDevice
import com.example.stbplay.ui.theme.STBPlayTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SearchReturnRegressionTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @org.junit.Before fun setupPhoneCast() = prewarmBatch2PhoneCast(rule)

    private fun logSearchBounds(tag: String) {
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val screen = rule.onNodeWithTag("search-screen-root").fetchSemanticsNode().boundsInRoot
        val parts = listOf("search-header", "search-input", "search-count").joinToString(" ") { part ->
            "$part=${rule.onNodeWithTag(part).fetchSemanticsNode().boundsInRoot}"
        }
        val container = rule.onNodeWithTag("search-results-container").fetchSemanticsNode().boundsInRoot
        val grid = rule.onNodeWithTag("search-results").fetchSemanticsNode().boundsInRoot
        val result = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        println("SEARCH_BOUNDS tag=$tag root=$root screen=$screen parts=[$parts] container=$container grid=$grid result=$result")
    }

    private fun activateResult(tvFocusTag: String, phoneTag: String) {
        if (InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice()) {
            logSearchBounds(tvFocusTag)
            rule.onNodeWithTag(tvFocusTag).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            rule.waitForIdle()
            logSearchBounds(tvFocusTag)
            rule.onNodeWithTag(tvFocusTag).assertIsFocused().assertIsDisplayed()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressDPadCenter()
            rule.waitForIdle()
        } else {
            logSearchBounds(phoneTag)
            rule.onNodeWithTag(phoneTag).assertIsDisplayed().performClick()
        }
    }

    private fun returnFromPlayer() {
        val action = rule.onNodeWithTag("return-from-player")
        if (InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice()) {
            action.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            rule.waitForIdle()
            action.assertIsFocused()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressDPadCenter()
        } else action.performClick()
        rule.waitForIdle()
    }

    @Test fun movieResultSurvivesBrowserDisposalAndReturnsWithoutAnotherPortalRequest() {
        val session = SearchSession().apply { show(StbPlayTab.CONTENT); updateQuery("Film") }
        var playing by mutableStateOf(false)
        var history by mutableStateOf(emptyList<String>())
        var calls = 0
        var selectedMedia: UiMedia? = null
        val streams = (1..48).map { PortalStream("film$it", "Film $it", null, null, "movie") }
        rule.setContent {
            STBPlayTheme {
                if (playing) Box(Modifier.fillMaxSize()) { QuestButton({ playing = false }, Modifier.testTag("return-from-player")) { androidx.tv.material3.Text("Return from player") } }
                else Batch2AppFixture(StbPlayTab.CONTENT, session = session,
                    onPlay = { selectedMedia = it; playing = true }, history = history,
                    rememberSearch = { history = updatedSearchHistory(history, it) }, clearHistory = { history = emptyList() },
                    remote = { _, _ -> calls++; VodCatalogBatch(streams, 1, 48, false) })
            }
        }
        rule.waitUntil(10_000) { session.results.size == 48 && !session.searching }
        if (!InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice())
            rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()
        rule.onNodeWithTag("search-results").performScrollToIndex(12)
        rule.waitForIdle()
        println("SEARCH_VISIBLE_ITEMS viewport=${session.gridState.layoutInfo.viewportSize} items=${session.gridState.layoutInfo.visibleItemsInfo.map { it.index to (it.offset to it.size) }}")
        val index = session.gridState.firstVisibleItemIndex
        val offset = session.gridState.firstVisibleItemScrollOffset
        activateResult("tv-focus:media:movie:film13", "search-result:movie:film13")
        assertEquals("film13", selectedMedia?.id)
        rule.onNodeWithTag("return-from-player").assertIsDisplayed()
        returnFromPlayer()
        rule.waitForIdle()
        assertEquals("film13", selectedMedia?.id)
        assertEquals("Film", session.query)
        assertEquals(48, session.results.size)
        assertEquals(1, calls)
        assertEquals(index, session.gridState.firstVisibleItemIndex)
        assertEquals(offset, session.gridState.firstVisibleItemScrollOffset)
        rule.onNodeWithText("Search Movies & Series").assertIsDisplayed()
        rule.onNodeWithText("Film 13").assertIsDisplayed()
        if (InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice())
            rule.onNodeWithTag("tv-focus:media:movie:film13").assertIsFocused()
        else rule.onNodeWithText("Film 13").assertIsFocused()
        rule.onNodeWithContentDescription("Clear search").performClick()
        rule.onNodeWithTag("recent-search:Film").assertIsDisplayed()
        rule.onNodeWithTag("clear-search-history").performClick()
        rule.onNodeWithTag("recent-search:Film").assertDoesNotExist()
        assertTrue(session.open)
        rule.onNodeWithText("Back").performClick()
        assertFalse(session.open)
    }

    @Test fun liveResultReturnsWithTheSameQueryAndChannelType() {
        val session = SearchSession().apply { show(StbPlayTab.LIVE); updateQuery("Channel") }
        var playing by mutableStateOf(false)
        var clicked: UiMedia? = null
        rule.setContent {
            STBPlayTheme {
                if (playing) QuestButton({ playing = false }, Modifier.testTag("return-from-player")) { androidx.tv.material3.Text("Return from player") }
                else Batch2AppFixture(StbPlayTab.LIVE, session = session, onPlay = { clicked = it; playing = true })
            }
        }
        rule.waitUntil(10_000) { session.results.isNotEmpty() && !session.searching }
        if (!InstrumentationRegistry.getInstrumentation().targetContext.isAndroidTvDevice())
            rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()
        println("SEARCH_VISIBLE_ITEMS viewport=${session.gridState.layoutInfo.viewportSize} items=${session.gridState.layoutInfo.visibleItemsInfo.map { it.index to (it.offset to it.size) }}")
        activateResult("tv-focus:media:live:ch1", "search-result:live:ch1")
        assertEquals("ch1", clicked?.id)
        rule.onNodeWithTag("return-from-player").assertIsDisplayed()
        returnFromPlayer()
        assertEquals("Channel", session.query)
        assertEquals("live", clicked?.streamType)
        rule.onNodeWithText("Search Live TV").assertIsDisplayed()
        rule.onNodeWithText("Channel 1").assertIsDisplayed()
    }
}
