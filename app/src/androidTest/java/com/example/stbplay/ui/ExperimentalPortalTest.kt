package com.example.stbplay.ui

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.stbplay.ui.theme.STBPlayTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExperimentalPortalTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun clearOnlyExperimentPreferences() {
        context.getSharedPreferences("free_drama_experiment", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun selectLastEpisodeThenSwitchPortalsWithoutChangingIptvStore() {
        val iptvStore = context.getSharedPreferences("portal_subscription_cache", Context.MODE_PRIVATE)
        val before = iptvStore.all.toMap()
        var chosen = 0
        rule.setContent {
            STBPlayTheme { ExperimentalPortalHost(onPlayEpisode = { chosen = it }) { Text("IPTV fixture") } }
        }
        rule.onNodeWithTag("drama_zanjeerain").performClick()
        rule.onNodeWithTag("drama_episodes").performScrollToNode(hasTestTag("drama_episode_39"))
        rule.onNodeWithTag("drama_episode_39").performClick()
        rule.runOnIdle { assertEquals(39, chosen) }
        rule.onNodeWithTag("portal_iptv").performClick()
        rule.onNodeWithText("IPTV fixture").assertIsDisplayed()
        rule.onNodeWithTag("portal_default").performClick()
        rule.onNodeWithText("IPTV fixture").assertDoesNotExist()
        rule.runOnIdle { assertEquals(before, iptvStore.all) }
    }
    @Test fun selectedPortalIsRememberedInExperimentPreferences() {
        rule.setContent { STBPlayTheme { ExperimentalPortalHost { Text("IPTV fixture") } } }
        rule.onNodeWithTag("portal_iptv").performClick()
        rule.runOnIdle { assertTrue(context.getSharedPreferences("free_drama_experiment", Context.MODE_PRIVATE).getBoolean("iptv_selected", false)) }
    }
    @Test fun kidsProfileDoesNotExposeUnratedDramaEpisodes() {
        rule.setContent { STBPlayTheme { ExperimentalPortalHost(isKids = true) { Text("IPTV fixture") } } }
        rule.onNodeWithTag("drama_zanjeerain").assertDoesNotExist()
        rule.onNodeWithTag("portal_iptv").performClick()
        rule.onNodeWithText("IPTV fixture").assertIsDisplayed()
    }
}
