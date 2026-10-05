package com.example.stbplay.ui.screens

import android.view.KeyEvent

internal enum class TvPlaybackKeyAction {
    SEEK_BACK, SEEK_FORWARD, SHOW_ACTIONS, SHOW_TIMELINE, TOGGLE_PLAY_PAUSE, REVEAL, NATIVE
}

/** Left/right stay on the timeline until Up explicitly enters the action row. */
internal fun tvPlaybackKeyAction(
    keyCode: Int,
    actionsHaveFocus: Boolean,
    allowSeeking: Boolean
): TvPlaybackKeyAction = when (keyCode) {
    KeyEvent.KEYCODE_MEDIA_REWIND -> if (allowSeeking) TvPlaybackKeyAction.SEEK_BACK else TvPlaybackKeyAction.REVEAL
    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> if (allowSeeking) TvPlaybackKeyAction.SEEK_FORWARD else TvPlaybackKeyAction.REVEAL
    KeyEvent.KEYCODE_DPAD_LEFT -> when {
        actionsHaveFocus -> TvPlaybackKeyAction.NATIVE
        allowSeeking -> TvPlaybackKeyAction.SEEK_BACK
        else -> TvPlaybackKeyAction.REVEAL
    }
    KeyEvent.KEYCODE_DPAD_RIGHT -> when {
        actionsHaveFocus -> TvPlaybackKeyAction.NATIVE
        allowSeeking -> TvPlaybackKeyAction.SEEK_FORWARD
        else -> TvPlaybackKeyAction.REVEAL
    }
    KeyEvent.KEYCODE_DPAD_UP -> if (actionsHaveFocus) TvPlaybackKeyAction.REVEAL else TvPlaybackKeyAction.SHOW_ACTIONS
    KeyEvent.KEYCODE_DPAD_DOWN -> if (actionsHaveFocus) TvPlaybackKeyAction.SHOW_TIMELINE else TvPlaybackKeyAction.REVEAL
    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> when {
        actionsHaveFocus -> TvPlaybackKeyAction.NATIVE
        allowSeeking -> TvPlaybackKeyAction.TOGGLE_PLAY_PAUSE
        else -> TvPlaybackKeyAction.REVEAL
    }
    else -> TvPlaybackKeyAction.NATIVE
}
