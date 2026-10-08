package com.example.stbplay.ui.screens

import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.media3.cast.Cast
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.stbplay.R
import com.example.stbplay.data.StalkerContentKind
import com.example.stbplay.data.StalkerPlayRequest
import com.example.stbplay.isAndroidTvDevice
import com.example.stbplay.ui.theme.STBPlayTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** Real decoder smoke checks against a tiny generated video served by the CI host, not a provider. */
@OptIn(UnstableApi::class)
class PlaybackSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val started = AtomicInteger()
    private val failures = CopyOnWriteArrayList<String>()
    private val isTv get() = rule.activity.isAndroidTvDevice()
    private val videoUrl = "http://10.0.2.2:8765/video.mp4"

    @Before fun initializePhoneCastAsTheAppDoes() {
        rule.runOnIdle {
            if (!isTv) runCatching { Cast.getSingletonInstance(rule.activity).initialize() }
        }
    }

    private fun View.findPlayerView(): SeekablePlayerView? {
        if (this is SeekablePlayerView) return this
        if (this is ViewGroup) for (index in 0 until childCount) {
            getChildAt(index).findPlayerView()?.let { return it }
        }
        return null
    }

    private fun playerView(): SeekablePlayerView = rule.runOnIdle {
        requireNotNull(rule.activity.window.decorView.findPlayerView())
    }

    private fun awaitVideo(): Pair<SeekablePlayerView, Player> {
        rule.waitUntil(30_000) { started.get() > 0 || failures.isNotEmpty() }
        assertTrue("Playback failure: $failures", failures.isEmpty())
        assertTrue(started.get() > 0)
        val view = playerView()
        val player = rule.runOnIdle { requireNotNull(view.player) }
        // Check decoded pixels, rather than treating STATE_READY as proof of a visible video.
        rule.waitUntil(15_000) {
            rule.runOnIdle {
                val bitmap = (view.videoSurfaceView as? TextureView)?.bitmap
                if (bitmap == null) false else {
                    val hasVideo = (1..3).any { x -> (1..3).any { y ->
                        bitmap.getPixel(bitmap.width * x / 4, bitmap.height * y / 4) and 0x00ffffff != 0
                    } }
                    bitmap.recycle()
                    hasVideo
                }
            }
        }
        rule.runOnIdle { assertTrue(player.duration > 0); player.volume = 0f }
        return view to player
    }

    @Test fun moviePlaysAndBackCallbackWorksOnTvAndPhone() {
        var backCount = 0
        rule.setContent {
            STBPlayTheme {
                PlaybackRoute(StalkerPlayRequest(videoUrl, StalkerContentKind.MOVIE, title = "Smoke movie"),
                    portalUiUrl = "http://10.0.2.2:8765", macAddress = "02:00:00:00:00:01", token = null,
                    onPlaybackStarted = { started.incrementAndGet() }, onPlaybackFailure = { failures.add(it) },
                    onBack = { backCount++ })
            }
        }
        val (_, player) = awaitVideo()
        rule.runOnIdle { assertTrue(player.isPlaying) }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(1, backCount)
        assertEquals(1, started.get())
        assertTrue(failures.isEmpty())
    }

    @Test fun tvPreviewAndFullscreenKeepTheSamePlayerAndPosition() {
        assumeTrue(isTv)
        var embedded by mutableStateOf(true)
        val request = StalkerPlayRequest(videoUrl, StalkerContentKind.LIVE, title = "Smoke channel")
        rule.setContent {
            STBPlayTheme {
                Box(if (embedded) Modifier.size(320.dp, 180.dp) else Modifier.fillMaxSize()) {
                    PlaybackRoute(request, portalUiUrl = "http://10.0.2.2:8765", macAddress = "02:00:00:00:00:01", token = null,
                        embedded = embedded, onPlaybackStarted = { started.incrementAndGet() },
                        onPlaybackFailure = { failures.add(it) }, onBack = { embedded = true })
                }
            }
        }
        val (originalView, originalPlayer) = awaitVideo()
        val position = rule.runOnIdle { originalPlayer.pause(); originalPlayer.currentPosition }
        repeat(3) {
            rule.runOnIdle { embedded = false }; rule.waitForIdle()
            assertSame(originalView, playerView())
            rule.runOnIdle {
                assertSame(originalPlayer, originalView.player)
                assertEquals(position, originalPlayer.currentPosition)
                assertTrue(originalView.isFocusable)
            }
            rule.runOnIdle { embedded = true }; rule.waitForIdle()
            rule.runOnIdle {
                assertSame(originalPlayer, originalView.player)
                assertFalse(originalView.isFocusable)
            }
        }
        assertEquals(1, started.get())
        assertTrue(failures.isEmpty())
    }

    @Test fun phoneFullscreenKeepsTheSamePlayerAndReturnsOnBack() {
        assumeTrue(!isTv)
        rule.setContent {
            STBPlayTheme {
                PlaybackRoute(StalkerPlayRequest(videoUrl, StalkerContentKind.MOVIE, title = "Phone smoke"),
                    portalUiUrl = "http://10.0.2.2:8765", macAddress = "02:00:00:00:00:01", token = null,
                    onPlaybackStarted = { started.incrementAndGet() }, onPlaybackFailure = { failures.add(it) }, onBack = {})
            }
        }
        val (view, player) = awaitVideo()
        // Phone controls open on user input, just as they do in the installed app.
        rule.onRoot().performTouchInput { click() }
        val position = rule.runOnIdle { player.pause(); player.currentPosition }
        rule.onNodeWithText("Full").performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertSame(player, view.player); assertEquals(position, player.currentPosition) }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        val returnedView = playerView()
        rule.runOnIdle { assertSame(player, returnedView.player) }
        assertEquals(1, started.get())
        assertTrue(failures.isEmpty())
    }

    @Test fun questPlayerLayoutAndControllerCallbacksKeepTheirExistingBehaviour() {
        // This verifies the Quest layout/controller code on the emulator, not Quest hardware.
        rule.runOnIdle {
            val view = LayoutInflater.from(rule.activity).inflate(R.layout.player_view_meta, null) as SeekablePlayerView
            assertTrue(view.videoSurfaceView is TextureView)
            val seeks = mutableListOf<Int>()
            var actions = 0
            view.onSeekDirection = { seeks.add(it) }
            view.onShowActions = { actions++ }
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
            assertEquals(listOf(-1, 1), seeks)
            assertEquals(1, actions)
        }
    }
}
