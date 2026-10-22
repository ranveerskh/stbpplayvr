package com.example.stbplay.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.media3.ui.TimeBar
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

/** Intercepts only deliberate single-finger vertical movement on unobstructed video. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class PhonePlaybackGestures(private val host: SeekablePlayerView) {
    private var activity: Activity? = null
    private var originalBrightness = -1f
    private var candidate = false
    private var captured = false
    private var cancelled = false
    private var startX = 0f
    private var startY = 0f
    private var brightnessSide = false
    private var startLevel = 0f
    private val audio = host.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val slop = ViewConfiguration.get(host.context).scaledTouchSlop * 2f
    private val dp = host.resources.displayMetrics.density
    private var indicator: TextView? = null
    private val hide = Runnable { indicator?.visibility = View.GONE }

    fun configure(enabled: Boolean, owner: Activity?) {
        val next = owner.takeIf { enabled }
        if (next === activity) return
        restore()
        activity = next
        originalBrightness = next?.window?.attributes?.screenBrightness ?: -1f
    }

    fun restore() {
        activity?.window?.let { window ->
            window.attributes = window.attributes.apply { screenBrightness = originalBrightness }
        }
        activity = null
        candidate = false
        captured = false
        host.removeCallbacks(hide)
        indicator?.visibility = View.GONE
    }

    fun intercept(event: MotionEvent): Boolean {
        val owner = activity ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                captured = false
                cancelled = false
                startX = event.x
                startY = event.y
                // Keep Android's edge gestures and top/bottom playback controls untouched.
                val edge = 32f * dp
                candidate = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER &&
                    event.x in edge..(host.width - edge) &&
                    event.y in (80f * dp)..(host.height - 96f * dp) &&
                    // Hiding controls can retain VISIBLE hit boxes while fading out.
                    (!host.isControllerFullyVisible || !hitsControl(host, event.x, event.y))
                brightnessSide = event.x < host.width / 2f
                startLevel = if (brightnessSide) {
                    owner.window.attributes.screenBrightness.takeIf { it >= 0f } ?: runCatching {
                        Settings.System.getInt(host.context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                    }.getOrDefault(0.5f)
                } else audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() /
                    audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                candidate = false
                cancelled = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!cancelled && candidate) {
                    val dx = event.x - startX
                    val dy = event.y - startY
                    if (!captured && abs(dx) > slop && abs(dx) >= abs(dy)) candidate = false
                    if (!captured && abs(dy) > slop && abs(dy) > abs(dx) * 1.5f) {
                        captured = true
                        host.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (captured) {
                        val level = (startLevel - dy / (host.height * 0.65f)).coerceIn(0f, 1f)
                        val shown = if (brightnessSide) {
                            owner.window.attributes = owner.window.attributes.apply {
                                screenBrightness = level.coerceAtLeast(0.01f)
                            }
                            level
                        } else {
                            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            // Android enforces fixed-volume devices and safe media volume limits.
                            runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max).roundToInt(), 0) }
                            audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
                        }
                        show(shown)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val consumed = captured
                candidate = false
                captured = false
                host.parent?.requestDisallowInterceptTouchEvent(false)
                host.removeCallbacks(hide)
                host.postDelayed(hide, 450)
                return consumed
            }
        }
        return captured
    }

    private fun hitsControl(group: ViewGroup, x: Float, y: Float): Boolean {
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            if (child.visibility != View.VISIBLE || child === indicator) continue
            val localX = x - child.left + group.scrollX
            val localY = y - child.top + group.scrollY
            if (localX < 0 || localY < 0 || localX >= child.width || localY >= child.height) continue
            if (child is TimeBar || child.isClickable || child.isLongClickable) return true
            if (child is ViewGroup && hitsControl(child, localX, localY)) return true
        }
        return false
    }

    private fun show(level: Float) {
        val label = indicator ?: TextView(host.context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            background = GradientDrawable().apply {
                setColor(0xCC111827.toInt())
                cornerRadius = 12 * dp
                setStroke(dp.toInt().coerceAtLeast(1), 0x40FFFFFF)
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            host.addView(this, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
            indicator = this
        }
        label.text = "${if (brightnessSide) "☀ Brightness" else "🔊 Volume"} ${(level * 100).roundToInt()}%"
        label.visibility = View.VISIBLE
        host.removeCallbacks(hide)
        host.postDelayed(hide, 700)
    }
}
