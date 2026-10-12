package com.example.stbplay.ui.screens

import android.media.AudioManager
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(UnstableApi::class)
class PhonePlaybackGesturesTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun event(action: Int, x: Float, y: Float): MotionEvent {
        val properties = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER }
        val coords = MotionEvent.PointerCoords().apply { this.x = x; this.y = y; pressure = 1f; size = 1f }
        val now = SystemClock.uptimeMillis()
        return MotionEvent.obtain(now, now, action, 1, arrayOf(properties), arrayOf(coords), 0, 0,
            1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0)
    }

    private fun PhonePlaybackGestures.send(action: Int, x: Float, y: Float): Boolean {
        val event = event(action, x, y)
        return try { intercept(event) } finally { event.recycle() }
    }

    @Test fun brightnessIsTemporaryAndHorizontalAndEdgeInputRemainUntouched() {
        rule.runOnIdle {
            val activity = rule.activity
            val original = activity.window.attributes.screenBrightness
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = 0.5f }
            val host = SeekablePlayerView(activity)
            val dp = activity.resources.displayMetrics.density
            host.layout(0, 0, (800 * dp).toInt(), (480 * dp).toInt())
            val gestures = PhonePlaybackGestures(host)
            try {
                gestures.configure(true, activity)
                assertFalse(gestures.send(MotionEvent.ACTION_DOWN, 180 * dp, 250 * dp))
                assertFalse(gestures.send(MotionEvent.ACTION_MOVE, 250 * dp, 252 * dp))
                assertFalse(gestures.send(MotionEvent.ACTION_UP, 250 * dp, 252 * dp))
                assertEquals(0.5f, activity.window.attributes.screenBrightness, 0.001f)
                gestures.send(MotionEvent.ACTION_DOWN, 5 * dp, 250 * dp)
                assertFalse(gestures.send(MotionEvent.ACTION_MOVE, 5 * dp, 160 * dp))
                gestures.send(MotionEvent.ACTION_UP, 5 * dp, 160 * dp)
                gestures.send(MotionEvent.ACTION_DOWN, 180 * dp, 250 * dp)
                assertTrue(gestures.send(MotionEvent.ACTION_MOVE, 180 * dp, 160 * dp))
                assertTrue(activity.window.attributes.screenBrightness > 0.5f)
                gestures.send(MotionEvent.ACTION_UP, 180 * dp, 160 * dp)
                gestures.configure(false, activity)
                assertEquals(0.5f, activity.window.attributes.screenBrightness, 0.001f)
                gestures.send(MotionEvent.ACTION_DOWN, 180 * dp, 250 * dp)
                assertFalse(gestures.send(MotionEvent.ACTION_MOVE, 180 * dp, 160 * dp))
            } finally {
                gestures.restore()
                activity.window.attributes = activity.window.attributes.apply { screenBrightness = original }
            }
        }
    }

    @Test fun rightSwipeUsesBoundedMediaVolumeAndLeavesBrightnessAlone() {
        rule.runOnIdle {
            val activity = rule.activity
            val audio = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val oldVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val brightness = activity.window.attributes.screenBrightness
            val dp = activity.resources.displayMetrics.density
            val host = SeekablePlayerView(activity)
            host.layout(0, 0, (800 * dp).toInt(), (480 * dp).toInt())
            val gestures = PhonePlaybackGestures(host)
            try {
                gestures.configure(true, activity)
                gestures.send(MotionEvent.ACTION_DOWN, 620 * dp, 250 * dp)
                assertTrue(gestures.send(MotionEvent.ACTION_MOVE, 620 * dp, 400 * dp))
                assertTrue(audio.getStreamVolume(AudioManager.STREAM_MUSIC) <= oldVolume)
                assertTrue(audio.getStreamVolume(AudioManager.STREAM_MUSIC) >= 0)
                assertEquals(brightness, activity.window.attributes.screenBrightness, 0.001f)
                gestures.send(MotionEvent.ACTION_UP, 620 * dp, 400 * dp)
            } finally {
                gestures.restore()
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, oldVolume, 0)
            }
        }
    }
}
