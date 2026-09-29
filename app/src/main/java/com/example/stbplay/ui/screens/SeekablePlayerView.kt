package com.example.stbplay.ui.screens

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

/** Handle TV transport keys before the player's controls consume focus navigation. */
@OptIn(UnstableApi::class)
class SeekablePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PlayerView(context, attrs) {
    var onSeekDirection: ((Int) -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> -1
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> 1
            else -> 0
        }
        if (direction != 0 && onSeekDirection != null) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) onSeekDirection?.invoke(direction)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
