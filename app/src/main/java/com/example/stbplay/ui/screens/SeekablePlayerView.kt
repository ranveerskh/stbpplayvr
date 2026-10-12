package com.example.stbplay.ui.screens

import android.app.Activity
import android.view.MotionEvent
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

/** Handle TV transport keys before the player's controls consume focus navigation. */
@OptIn(UnstableApi::class)
class SeekablePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PlayerView(context, attrs) {
    private val phoneGestures by lazy { PhonePlaybackGestures(this) }

    fun configurePhoneGestures(enabled: Boolean, activity: Activity?) {
        phoneGestures.configure(enabled, activity)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val wasCaptured = phoneGestureCaptured
        val consumed = phoneGestures.intercept(event)
        if (consumed && !wasCaptured && event.actionMasked == MotionEvent.ACTION_MOVE) {
            val cancel = MotionEvent.obtain(event)
            cancel.action = MotionEvent.ACTION_CANCEL
            super.dispatchTouchEvent(cancel)
            cancel.recycle()
        }
        phoneGestureCaptured = consumed && event.actionMasked != MotionEvent.ACTION_UP &&
            event.actionMasked != MotionEvent.ACTION_CANCEL
        return if (consumed) true else super.dispatchTouchEvent(event)
    }

    private var phoneGestureCaptured = false

    override fun onDetachedFromWindow() {
        phoneGestures.restore()
        super.onDetachedFromWindow()
    }

    private var originalControlBackground: android.graphics.drawable.Drawable? = null
    private var originalControlMargins: IntArray? = null
    private val originalButtonBackgrounds = mutableMapOf<View, android.graphics.drawable.Drawable?>()
    private var glassControlsApplied = false

    fun configureGlassControls(enabled: Boolean) {
        if (enabled == glassControlsApplied) return
        val bar = findViewById<View>(androidx.media3.ui.R.id.exo_bottom_bar) ?: return
        val density = resources.displayMetrics.density
        val margins = bar.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (enabled) {
            originalControlBackground = bar.background
            bar.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xB34A5665.toInt(), 0xB3101927.toInt(), 0xD908101B.toInt())
            ).apply {
                cornerRadius = 18 * density
                setStroke(density.toInt().coerceAtLeast(1), 0x66DAE6FF)
            }
            if (margins != null) {
                originalControlMargins = intArrayOf(margins.leftMargin, margins.topMargin, margins.rightMargin, margins.bottomMargin)
                val inset = (14 * density).toInt()
                margins.setMargins(inset, margins.topMargin, inset, inset)
                bar.layoutParams = margins
            }
            listOf(androidx.media3.ui.R.id.exo_play_pause, androidx.media3.ui.R.id.exo_ffwd,
                androidx.media3.ui.R.id.exo_rew, androidx.media3.ui.R.id.exo_ffwd_with_amount,
                androidx.media3.ui.R.id.exo_rew_with_amount).forEach { id ->
                findViewById<View>(id)?.let { button ->
                    originalButtonBackgrounds[button] = button.background
                    fun surface(focused: Boolean) = android.graphics.drawable.GradientDrawable().apply {
                        setColor(if (focused) 0x66413910 else 0x22FFFFFF)
                        cornerRadius = 12 * density
                        setStroke((if (focused) 2 * density else density).toInt().coerceAtLeast(1),
                            if (focused) 0xFFFFD166.toInt() else 0x55DAE6FF)
                    }
                    button.background = android.graphics.drawable.StateListDrawable().apply {
                        addState(intArrayOf(android.R.attr.state_focused), surface(true))
                        addState(intArrayOf(), surface(false))
                    }
                }
            }
        } else {
            bar.background = originalControlBackground
            originalControlMargins?.let { old ->
                margins?.let { it.setMargins(old[0], old[1], old[2], old[3]); bar.layoutParams = it }
            }
            originalButtonBackgrounds.forEach { (button, background) -> button.background = background }
            originalButtonBackgrounds.clear()
        }
        glassControlsApplied = enabled
    }

    var onSeekDirection: ((Int) -> Unit)? = null
    var onShowActions: (() -> Unit)? = null

    fun focusProgressBar() {
        showController()
        val progress = findViewById<View>(androidx.media3.ui.R.id.exo_progress)
        if (progress?.requestFocus() != true) requestFocus()
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP && onShowActions != null) {
            if (event.action == KeyEvent.ACTION_DOWN && !event.isCanceled) onShowActions?.invoke()
            return true
        }
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> -1
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> 1
            else -> 0
        }
        if (direction != 0 && onSeekDirection != null) {
            if (event.action == KeyEvent.ACTION_DOWN && !event.isCanceled) onSeekDirection?.invoke(direction)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
