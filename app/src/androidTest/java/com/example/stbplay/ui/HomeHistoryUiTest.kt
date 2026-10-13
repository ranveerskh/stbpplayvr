package com.example.stbplay.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.stbplay.data.SeriesHistoryEntry
import com.example.stbplay.data.WatchProgress
import com.example.stbplay.ui.screens.TvMediaDetailsScreen
import com.example.stbplay.ui.theme.STBPlayTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeHistoryUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun liveHistoryHasActionsAndNoResumeOrFakeProgress() {
        var played = 0; var favorite = 0; var removed = 0; var backed = 0
        rule.setContent { STBPlayTheme {
            TvMediaDetailsScreen(UiMedia("ch", "News", streamType = "live", progress = .001f),
                onPlay = { played++ }, onResume = { error("Live cannot resume") },
                onToggleFavorite = { favorite++ }, onRemoveHistory = { removed++ }, onBack = { backed++ })
        } }
        rule.onNodeWithText("Play").performClick()
        rule.onNodeWithText("Add to favourites").performClick()
        rule.onNodeWithText("Remove from history").performClick()
        rule.onNodeWithText("Back").performClick()
        assertEquals(listOf(1, 1, 1, 1), listOf(played, favorite, removed, backed))
        rule.onNodeWithText("Start over").assertDoesNotExist()
    }
    @Test fun seriesHistoryOffersExactEpisodeResumeStartOverAndEpisodes() {
        var resumed = 0; var started = 0; var browsed = 0
        rule.setContent { STBPlayTheme {
            TvMediaDetailsScreen(UiMedia("s", "Show", streamType = "series", progress = .15f, resumeLabel = "Resume Episode 5 · 15%"),
                onPlay = { started++ }, onResume = { resumed++ }, onToggleFavorite = {}, onBack = {},
                onBrowseEpisodes = { browsed++ })
        } }
        rule.onNodeWithText("Resume Episode 5 · 15%").performClick()
        rule.onNodeWithText("Start over").performClick()
        rule.onNodeWithText("Episodes").performClick()
        assertEquals(listOf(1, 1, 1), listOf(resumed, started, browsed))
    }
    @Test fun historyTimestampsAndEpisodeUpdatesStayFrozenDuringVodThenRefresh() {
        var playing by mutableStateOf(false)
        var history by mutableStateOf(listOf(WatchProgress("e5", .1f, 1)))
        var episodes by mutableStateOf(mapOf("e5" to SeriesHistoryEntry("show", "e5", "Episode 5", "s1")))
        var displayed: List<WatchProgress> = emptyList()
        var projections = 0
        rule.setContent {
            val (stableHistory, stableEpisodes) = rememberPlaybackHomeHistory(history, episodes, playing)
            val rows = remember(stableHistory, stableEpisodes) { projections++; stableHistory }
            SideEffect { displayed = rows }
        }
        rule.waitForIdle(); rule.runOnIdle { playing = true }; rule.waitForIdle()
        val before = projections
        repeat(5) { tick ->
            rule.runOnIdle { history = listOf(WatchProgress("e5", .2f + tick / 10f, tick + 2L)) }
            rule.waitForIdle(); assertEquals(before, projections)
        }
        assertEquals(.1f, displayed.first().fraction, .001f)
        rule.runOnIdle { playing = false }; rule.waitForIdle()
        assertEquals(.6f, displayed.first().fraction, .001f)
        assertEquals(before + 1, projections)
    }
}
