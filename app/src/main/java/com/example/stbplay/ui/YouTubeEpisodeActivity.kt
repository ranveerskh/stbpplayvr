package com.example.stbplay.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.stbplay.BuildConfig
import com.example.stbplay.data.FreeDramaCatalog

/** Official iframe playback only. No downloads, proxies, JavaScript bridge or ad filtering. */
class YouTubeEpisodeActivity : Activity() {
    private var browser: WebView? = null
    private lateinit var normal: LinearLayout
    private lateinit var container: FrameLayout
    private lateinit var heading: TextView
    private lateinit var previous: Button
    private lateinit var next: Button
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private var number = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.PORTAL_EXPERIMENT) { finish(); return }
        number = savedInstanceState?.getInt("episode") ?: intent.getIntExtra("episode", 1)
        if (FreeDramaCatalog.episode(number) == null) { finish(); return }
        container = FrameLayout(this)
        normal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 15, 25))
        }
        heading = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 18f
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        normal.addView(heading)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun action(label: String, click: () -> Unit): Button = Button(this).apply {
            text = label; setOnClickListener { click() }
            setOnFocusChangeListener { _, focused ->
                setTextColor(if (focused) Color.rgb(255, 209, 102) else Color.BLACK)
            }
            actions.addView(this, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        action("Back") { finish() }
        previous = action("Previous") { if (number > 1) { number--; loadEpisode() } }
        next = action("Next") { if (number < FreeDramaCatalog.episodes.size) { number++; loadEpisode() } }
        action("Open in YouTube") { openExternal(FreeDramaCatalog.episode(number)!!.watchUrl) }
        normal.addView(actions)
        container.addView(normal, FrameLayout.LayoutParams(-1, -1))
        setContentView(container)
        updateHeading()
        val prefs = getSharedPreferences("free_drama_experiment", Context.MODE_PRIVATE)
        if (prefs.getBoolean("youtube_policy_accepted_v1", false)) trySetupPlayer()
        else AlertDialog.Builder(this)
            .setTitle("YouTube playback")
            .setMessage("By continuing you agree to YouTube's Terms of Service and this playback privacy notice.\n\n" + FreeDramaCatalog.playbackPrivacy)
            .setPositiveButton("Agree & continue") { _, _ ->
                prefs.edit().putBoolean("youtube_policy_accepted_v1", true).apply(); trySetupPlayer()
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setNeutralButton("Read policies") { _, _ -> openExternal("https://policies.google.com/privacy"); finish() }
            .setOnCancelListener { finish() }.show()
    }

    private fun trySetupPlayer() {
        runCatching { setupPlayer() }.onFailure {
            normal.addView(TextView(this).apply {
                text = "YouTube player could not start on this device. Use Open in YouTube."
                setTextColor(Color.WHITE)
                setPadding(dp(12), dp(12), dp(12), dp(12))
            })
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupPlayer() {
        browser = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    openExternal(request.url.toString())
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (customView != null) { callback.onCustomViewHidden(); return }
                    customView = view; customCallback = callback
                    normal.visibility = View.GONE
                    container.addView(view, FrameLayout.LayoutParams(-1, -1))
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    view.requestFocus()
                }
                override fun onHideCustomView() { hideFullscreen() }
            }
            normal.addView(this, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        loadEpisode()
    }

    private fun updateHeading() {
        heading.text = "Zanjeerain · Episode $number · HUM TV on YouTube"
        previous.isEnabled = number > 1
        next.isEnabled = number < FreeDramaCatalog.episodes.size
    }

    private fun loadEpisode() {
        updateHeading()
        val episode = FreeDramaCatalog.episode(number) ?: return
        browser?.loadDataWithBaseURL("https://$packageName/", FreeDramaCatalog.playerHtml(episode, packageName),
            "text/html", "UTF-8", null)
    }

    private fun openExternal(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme != "https") return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    private fun hideFullscreen() {
        customView?.let { container.removeView(it) }
        customView = null
        normal.visibility = View.VISIBLE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        customCallback?.onCustomViewHidden()
        customCallback = null
    }

    @Deprecated("Legacy Activity back dispatch supports the test's minSdk")
    override fun onBackPressed() {
        if (customView != null) hideFullscreen() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("episode", number); super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        browser?.evaluateJavascript("if(window.player && player.pauseVideo){player.pauseVideo();}", null)
        browser?.onPause()
        super.onPause()
    }
    override fun onResume() { super.onResume(); browser?.onResume() }
    override fun onDestroy() {
        hideFullscreen()
        browser?.let { normal.removeView(it); it.stopLoading(); it.loadUrl("about:blank"); it.destroy() }
        browser = null
        super.onDestroy()
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
