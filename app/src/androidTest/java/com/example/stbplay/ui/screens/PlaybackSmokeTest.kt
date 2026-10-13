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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.example.stbplay.ui.saveThemePreview
import com.example.stbplay.ui.theme.STBPlayTheme
import com.google.android.gms.cast.framework.CastContext
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

    @Suppress("DEPRECATION")
    @Before fun initializePhoneEmulatorCast() {
        rule.runOnIdle {
            if (!isTv) {
                // Warm the old emulator module on main before Media3's async initialization.
                CastContext.getSharedInstance(rule.activity)
                Cast.getSingletonInstance(rule.activity).initialize()
            }
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

    private fun pauseAtKnownPosition(player: Player): Long {
        // A pause acknowledgement can advance a freshly started player's reported position.
        // Anchor the paused fixture to a known seek position before testing layout changes.
        val position = 5_000L
        rule.runOnIdle { player.pause(); player.seekTo(position) }
        rule.waitUntil(10_000) {
            rule.runOnIdle { !player.isPlaying && player.currentPosition == position }
        }
        return position
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
        val (movieView, player) = awaitVideo()
        rule.runOnIdle {
            // Reveal both native and Compose controls through the existing input path.
            movieView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
            movieView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
            movieView.showController()
        }
        rule.runOnIdle {
            val panel = movieView.findViewById<View>(androidx.media3.ui.R.id.exo_bottom_bar)
            val timeline = movieView.findViewById<View>(androidx.media3.ui.R.id.exo_progress)
            val panelBounds = android.graphics.Rect(); val timelineBounds = android.graphics.Rect()
            assertTrue(panel.getGlobalVisibleRect(panelBounds))
            assertTrue(timeline.getGlobalVisibleRect(timelineBounds))
            assertTrue("Timeline must stay within Glass panel: $timelineBounds / $panelBounds", panelBounds.contains(timelineBounds))
        }
        rule.onNodeWithText("← Back").assertIsDisplayed()
        if (!isTv) {
            val icon = rule.onNodeWithContentDescription("Fullscreen", useUnmergedTree = true).assertIsDisplayed().captureToImage().toPixelMap()
            val whitePixels = (0 until icon.width).sumOf { x -> (0 until icon.height).count { y ->
                val pixel = icon[x, y]; pixel.red > 0.9f && pixel.green > 0.9f && pixel.blue > 0.9f && pixel.alpha > 0.9f
            } }
            assertTrue("Fullscreen icon must remain white against dark glass", whitePixels > 20)
        }
        saveThemePreview(rule, "player-glass-" + if (isTv) "tv" else "phone")
        rule.runOnIdle {
            assertTrue(player.isPlaying)
            assertEquals(10_000L, player.seekForwardIncrement)
            assertEquals(5_000L, player.seekBackIncrement)
        }
        // Compose delay uses virtual time; wall-clock polling can expire before
        // the virtual clock has advanced through the three-second idle interval.
        // Keep it fixed while Android finishes hiding the native controller.
        val originalAutoAdvance = rule.mainClock.autoAdvance
        rule.mainClock.autoAdvance = false
        try {
            rule.runOnIdle {
                movieView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
                movieView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
            }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.onNodeWithText("← Back").assertIsDisplayed()
            rule.runOnIdle { assertTrue(movieView.isControllerFullyVisible) }
            rule.mainClock.advanceTimeBy(2_500)
            rule.waitForIdle()
            rule.onNodeWithText("← Back").assertIsDisplayed()
            rule.runOnIdle { assertTrue(movieView.isControllerFullyVisible) }
            rule.mainClock.advanceTimeBy(600)
            rule.waitForIdle()
            // Media3's Android hide animation finishes outside the Compose clock.
            // Wait for GONE, then commit its visibility callback on one Compose frame.
            rule.waitUntil(7_000) { rule.runOnIdle {
                movieView.findViewById<View>(androidx.media3.ui.R.id.exo_controller).visibility != View.VISIBLE
            } }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.onNodeWithText("← Back").assertDoesNotExist()
        } finally {
            rule.mainClock.autoAdvance = originalAutoAdvance
        }
        val seekStart = pauseAtKnownPosition(player)
        rule.runOnIdle { player.seekForward() }
        rule.waitUntil(10_000) { rule.runOnIdle { player.currentPosition == seekStart + 10_000L } }
        rule.runOnIdle { player.seekBack() }
        rule.waitUntil(10_000) { rule.runOnIdle { player.currentPosition == seekStart + 5_000L } }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(1, backCount)
        assertEquals(1, started.get())
        assertTrue(failures.isEmpty())
    }

    @Test fun realProgressPersistenceWithLargeCatalogueDoesNotRemapOrReplacePlayingVideo() {
        val manager = com.example.stbplay.data.SettingsManager(rule.activity)
        val previousPortal = runBlocking { manager.portalSettings.first() }
        val previousViewer = runBlocking { manager.activeViewer.first() }
        val portal = com.example.stbplay.domain.model.PortalSettings(id = "vod-progress-fixture",
            name = "Fixture", url = "https://fixture.invalid", mac = "00:1A:79:00:00:01")
        runBlocking { manager.upsertPortal(portal); manager.activateViewer("owner") }
        val catalogue = (0 until 100_000).map { com.example.stbplay.data.model.PortalStream("vod-$it", "Movie $it", null, null, "movie") }
        var playing by mutableStateOf(true)
        var mappings = 0
        var projected: List<com.example.stbplay.ui.UiMedia> = emptyList()
        val savedTicks = AtomicInteger()
        val saveDurations = CopyOnWriteArrayList<Long>()
        val request = StalkerPlayRequest("http://10.0.2.2:8765/progress-long.mp4", StalkerContentKind.MOVIE, contentId = "vod-42", title = "Progress fixture")
        try {
            rule.setContent {
                val latestProgress by manager.vodProgress.collectAsState(initial = emptyMap())
                val scope = rememberCoroutineScope()
                val catalogueProgress = com.example.stbplay.ui.rememberPlaybackCatalogueProgress(latestProgress, playing)
                val rows = remember(catalogue, catalogueProgress) {
                    mappings++
                    catalogue.map { com.example.stbplay.ui.UiMedia(it.id, it.name, progress = catalogueProgress[it.id] ?: 0f) }
                }
                SideEffect { projected = rows }
                STBPlayTheme {
                    if (playing) PlaybackRoute(request, portalUiUrl = "http://10.0.2.2:8765",
                        macAddress = "02:00:00:00:00:01", token = null,
                        onPlaybackStarted = { started.incrementAndGet() }, onPlaybackFailure = { failures.add(it) },
                        onProgress = { position, duration -> scope.launch {
                            val began = android.os.SystemClock.elapsedRealtime()
                            manager.saveProgress(request.contentId, position, duration)
                            saveDurations.add(android.os.SystemClock.elapsedRealtime() - began)
                            savedTicks.incrementAndGet()
                        } }, onBack = { playing = false })
                }
            }
            val (view, player) = awaitVideo()
            rule.runOnIdle { player.volume = 0f }
            val initialMappings = rule.runOnIdle { mappings }
            val initialRows = projected
            rule.waitUntil(40_000) { savedTicks.get() >= 5 }
            rule.runOnIdle {
                assertEquals("Real five-second saves must not remap 100k hidden rows", initialMappings, mappings)
                assertSame(initialRows, projected)
                assertSame(player, view.player)
                assertTrue(player.isPlaying)
            }
            assertEquals(1, started.get())
            assertTrue(failures.isEmpty())
            assertTrue("A non-completed fixture must retain its resume record: position=${rule.runOnIdle { player.currentPosition }} duration=${rule.runOnIdle { player.duration }} ticks=${savedTicks.get()}",
                runBlocking { manager.vodProgress.first() }.containsKey(request.contentId))
            val beforeExitSaves = savedTicks.get()
            // Leaving playback applies the latest saved progress to the catalogue immediately.
            rule.runOnIdle { playing = false }
            rule.waitUntil(10_000) { savedTicks.get() > beforeExitSaves }
            val persisted = runBlocking { manager.vodProgress.first() }[request.contentId]!!
            rule.waitUntil(10_000) { rule.runOnIdle { kotlin.math.abs(projected[42].progress - persisted) < 0.001f } }
            assertEquals(persisted, projected[42].progress, 0.001f)
            assertEquals(100_000, projected.size)
            android.util.Log.i("VODProgressRegression", "rows=100000 saves=${savedTicks.get()} remapsDuringPlayback=0 saveMillis=$saveDurations deviceTv=$isTv")
        } finally {
            runBlocking {
                manager.activateViewer(previousViewer.id)
                manager.deletePortal(portal.id)
                if (previousPortal.id.isNotBlank()) manager.activatePortal(previousPortal.id)
            }
        }
    }

    @Test fun liveTimelineIsHiddenAndPhoneChannelActionsRemainCompact() {
        rule.setContent {
            STBPlayTheme {
                PlaybackRoute(StalkerPlayRequest(videoUrl, StalkerContentKind.LIVE, title = "Live smoke"),
                    portalUiUrl = "http://10.0.2.2:8765", macAddress = "02:00:00:00:00:01", token = null,
                    onPlaybackStarted = { started.incrementAndGet() }, onPlaybackFailure = { failures.add(it) }, onBack = {})
            }
        }
        val (view, _) = awaitVideo()
        rule.onRoot().performTouchInput { click() }
        rule.runOnIdle {
            assertEquals(View.GONE, view.findViewById<View>(androidx.media3.ui.R.id.exo_bottom_bar).visibility)
            assertEquals(View.GONE, view.findViewById<View>(androidx.media3.ui.R.id.exo_progress).visibility)
        }
        rule.onNodeWithContentDescription("Previous channel").assertIsDisplayed()
        rule.onNodeWithContentDescription("Next channel").assertIsDisplayed()
        if (!isTv) rule.onNodeWithContentDescription("Fullscreen").assertIsDisplayed()
        saveThemePreview(rule, "live-player-glass-" + if (isTv) "tv" else "phone")
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
        val position = pauseAtKnownPosition(originalPlayer)
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
        val position = pauseAtKnownPosition(player)
        rule.onNodeWithContentDescription("Fullscreen").performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertSame(player, view.player); assertEquals(position, player.currentPosition) }
        var originalBrightness = -1f
        rule.runOnIdle {
            originalBrightness = rule.activity.window.attributes.screenBrightness
            view.hideController()
            val now = android.os.SystemClock.uptimeMillis()
            fun finger(action: Int, x: Float, y: Float): Boolean {
                val properties = android.view.MotionEvent.PointerProperties().apply {
                    id = 0; toolType = android.view.MotionEvent.TOOL_TYPE_FINGER
                }
                val coords = android.view.MotionEvent.PointerCoords().apply {
                    this.x = x; this.y = y; pressure = 1f; size = 1f
                }
                val event = android.view.MotionEvent.obtain(now, now, action, 1, arrayOf(properties),
                    arrayOf(coords), 0, 0, 1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0)
                return try { view.dispatchTouchEvent(event) } finally { event.recycle() }
            }
            val x = view.width * 0.22f
            finger(android.view.MotionEvent.ACTION_DOWN, x, view.height * 0.6f)
            assertTrue(finger(android.view.MotionEvent.ACTION_MOVE, x, view.height * 0.3f))
            finger(android.view.MotionEvent.ACTION_UP, x, view.height * 0.3f)
            assertTrue("Fullscreen swipe must set a player-window brightness", rule.activity.window.attributes.screenBrightness >= 0f)
            assertEquals("Brightness gesture must not seek the video", position, player.currentPosition)
        }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertEquals(originalBrightness, rule.activity.window.attributes.screenBrightness, 0.001f) }
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
