package com.example.stbplay.ui.screens

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class TvPlaybackInputTest {
    @Test fun repeatedDirectionalSeekingStaysOnTimeline() {
        repeat(20) {
            assertEquals(TvPlaybackKeyAction.SEEK_FORWARD,
                tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_RIGHT, false, true))
            assertEquals(TvPlaybackKeyAction.SEEK_BACK,
                tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_LEFT, false, true))
        }
        assertEquals(TvPlaybackKeyAction.REVEAL,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_DOWN, false, true))
    }

    @Test fun upEntersActionsAndDownReturnsToTimeline() {
        assertEquals(TvPlaybackKeyAction.SHOW_ACTIONS,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_UP, false, true))
        assertEquals(TvPlaybackKeyAction.NATIVE,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_RIGHT, true, true))
        assertEquals(TvPlaybackKeyAction.NATIVE,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_CENTER, true, true))
        assertEquals(TvPlaybackKeyAction.SHOW_TIMELINE,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_DOWN, true, true))
    }

    @Test fun transportSeekingReturnsFromActionsToTimeline() {
        assertEquals(TvPlaybackKeyAction.SEEK_FORWARD,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, true, true))
        assertEquals(TvPlaybackKeyAction.SEEK_BACK,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_MEDIA_REWIND, true, true))
    }

    @Test fun liveTvCannotSeekOrPauseWithDirectionalControls() {
        listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_DPAD_CENTER).forEach { key ->
            assertEquals(TvPlaybackKeyAction.REVEAL, tvPlaybackKeyAction(key, false, false))
        }
        assertEquals(TvPlaybackKeyAction.SHOW_ACTIONS,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_UP, false, false))
    }

    @Test fun moviePauseAndBackKeepTheirExpectedActions() {
        assertEquals(TvPlaybackKeyAction.TOGGLE_PLAY_PAUSE,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_DPAD_CENTER, false, true))
        assertEquals(TvPlaybackKeyAction.NATIVE,
            tvPlaybackKeyAction(KeyEvent.KEYCODE_BACK, false, true))
    }
}
