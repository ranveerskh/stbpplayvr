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

    var onControlsInteraction: (() -> Unit)? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP)
            onControlsInteraction?.invoke()
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
    private val savedGlassLayout = mutableMapOf<View, IntArray>()

    private fun rememberLayout(view: View, params: android.view.ViewGroup.MarginLayoutParams) {
        savedGlassLayout[view] = intArrayOf(params.width, params.height, params.leftMargin,
            params.topMargin, params.rightMargin, params.bottomMargin,
            (params as? android.widget.FrameLayout.LayoutParams)?.gravity ?: -1)
    }

    fun configureGlassControls(enabled: Boolean, compactLandscape: Boolean = false) {
        findViewById<View>(androidx.media3.ui.R.id.exo_center_controls)?.translationY =
            if (enabled && compactLandscape) -48 * resources.displayMetrics.density else 0f
        if (enabled == glassControlsApplied) return
        val bar = findViewById<View>(androidx.media3.ui.R.id.exo_bottom_bar) ?: return
        val density = resources.displayMetrics.density
        val margins = bar.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (enabled) {
            originalControlBackground = bar.background
            if (margins != null) {
                rememberLayout(bar, margins)
                margins.height = (92 * density).toInt()
            }
            findViewById<View>(androidx.media3.ui.R.id.exo_progress)?.let { progress ->
                (progress.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let { params ->
                    rememberLayout(progress, params)
                    val inset = (28 * density).toInt()
                    params.setMargins(inset, params.topMargin, inset, (48 * density).toInt())
                    progress.layoutParams = params
                }
            }
            listOf(androidx.media3.ui.R.id.exo_time, androidx.media3.ui.R.id.exo_basic_controls,
                androidx.media3.ui.R.id.exo_extra_controls_scroll_view).forEach { id ->
                findViewById<View>(id)?.let { view ->
                    (view.layoutParams as? android.widget.FrameLayout.LayoutParams)?.let { params ->
                        rememberLayout(view, params)
                        params.gravity = android.view.Gravity.BOTTOM or
                            (if (id == androidx.media3.ui.R.id.exo_time) android.view.Gravity.START else android.view.Gravity.END)
                        params.bottomMargin = (6 * density).toInt()
                        view.layoutParams = params
                    }
                }
            }
            bar.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xD91C2735.toInt(), 0xD9101927.toInt(), 0xE608101B.toInt())
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
                        setColor(if (focused) 0xCC413910.toInt() else 0xB308101B.toInt())
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
            savedGlassLayout.forEach { (view, old) ->
                (view.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let { params ->
                    params.width = old[0]; params.height = old[1]
                    params.setMargins(old[2], old[3], old[4], old[5])
                    if (params is android.widget.FrameLayout.LayoutParams && old[6] != -1) params.gravity = old[6]
                    view.layoutParams = params
                }
            }
            savedGlassLayout.clear()
        }
        glassControlsApplied = enabled
    }

    private var liveTimeline = false
    fun configureLiveTimeline(live: Boolean) {
        if (liveTimeline == live) return
        liveTimeline = live
        val visibility = if (live) View.GONE else View.VISIBLE
        findViewById<View>(androidx.media3.ui.R.id.exo_bottom_bar)?.visibility = visibility
        findViewById<View>(androidx.media3.ui.R.id.exo_progress)?.visibility = visibility
    }

    var onSeekDirection: ((Int) -> Unit)? = null
    var onShowActions: (() -> Unit)? = null

    fun focusProgressBar() {
        showController()
        val progress = findViewById<View>(androidx.media3.ui.R.id.exo_progress)
        if (progress?.requestFocus() != true) requestFocus()
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode !in
            setOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BUTTON_B)) onControlsInteraction?.invoke()
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
