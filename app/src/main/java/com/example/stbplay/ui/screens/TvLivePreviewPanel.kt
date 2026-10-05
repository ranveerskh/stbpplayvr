@file:kotlin.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.stbplay.ui.screens

import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.example.stbplay.data.PortalUrl
import com.example.stbplay.data.StalkerPlayRequest
import com.example.stbplay.data.StalkerPlaybackResolver
import com.example.stbplay.data.SubtitlePreference
import com.example.stbplay.ui.TvHomeSurface
import com.example.stbplay.ui.UiMedia
import com.example.stbplay.ui.theme.LocalStbPalette
import kotlinx.coroutines.delay

/**
 * The panel owns one resolved URL and one player. Fullscreen only changes the
 * target view, never the request, resolver, or playback session.
 */
@Composable
fun TvLivePreviewPanel(
    channel: UiMedia?,
    request: StalkerPlayRequest?,
    portalUiUrl: String,
    macAddress: String,
    token: String?,
    sessionCookie: String,
    subtitlePreference: SubtitlePreference,
    androidBoxVideoCompatibility: Boolean,
    fullscreenSignal: Int,
    actionsRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onFavorite: () -> Unit,
    onPlaybackStarted: () -> Unit,
    onChannelStep: (Int) -> Unit,
    onBack: () -> Unit,
    onReturnToChannels: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalStbPalette.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val resolver = remember(portalUiUrl, macAddress, token, sessionCookie) {
        StalkerPlaybackResolver(portalUiUrl, macAddress, token, sessionCookie)
    }
    var url by remember(request, resolver) { mutableStateOf<String?>(null) }
    var error by remember(request, resolver) { mutableStateOf<String?>(null) }
    var retry by remember(request) { mutableIntStateOf(0) }
    var fullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlGeneration by remember { mutableIntStateOf(0) }
    var targetView by remember { mutableStateOf<PlayerView?>(null) }
    var focusVlcSignal by remember { mutableIntStateOf(0) }
    var controlsHaveFocus by remember { mutableStateOf(false) }
    val vlcRequester = remember { FocusRequester() }
    val startedCallback by rememberUpdatedState(onPlaybackStarted)
    val returnCallback by rememberUpdatedState(onReturnToChannels)
    var didStart by remember(request) { mutableStateOf(false) }

    LaunchedEffect(request, resolver, retry) {
        url = null
        error = null
        if (request != null) {
            runCatching { resolver.resolve(request) }
                .onSuccess { url = it }
                .onFailure { error = it.message ?: "Could not open this channel." }
        }
    }
    LaunchedEffect(fullscreenSignal) {
        if (fullscreenSignal > 0 && request != null) fullscreen = true
    }

    val player = remember(url, portalUiUrl, token, sessionCookie, subtitlePreference) {
        url?.let { streamUrl ->
            val headers = mutableMapOf(
                "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 MAG254",
                "Referer" to PortalUrl.refererUrl(portalUiUrl)
            )
            if (!token.isNullOrBlank()) headers["Authorization"] = "Bearer $token"
            if (sessionCookie.isNotBlank()) headers["Cookie"] = sessionCookie
            val factory = DefaultHttpDataSource.Factory()
                .setUserAgent(headers.getValue("User-Agent"))
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers)
            val selector = DefaultTrackSelector(context).apply {
                val language = subtitlePreference.languageCode()
                if (language != null) setParameters(buildUponParameters().setPreferredTextLanguage(language))
                else if (subtitlePreference == SubtitlePreference.OFF)
                    setParameters(buildUponParameters().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true))
            }
            ExoPlayer.Builder(context, DefaultRenderersFactory(context).setEnableDecoderFallback(true))
                .setTrackSelector(selector)
                .setMediaSourceFactory(DefaultMediaSourceFactory(factory)).build().also { exo ->
                    exo.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY && !didStart) {
                                didStart = true
                                startedCallback()
                            }
                        }
                        override fun onPlayerError(exception: PlaybackException) {
                            error = "Playback failed: ${exception.errorCodeName}"
                        }
                    })
                    val media = MediaItem.Builder().setUri(streamUrl).apply {
                        if (streamUrl.substringBefore('?').endsWith(".m3u8", true))
                            setMimeType(MimeTypes.APPLICATION_M3U8)
                    }.build()
                    exo.setMediaItem(media)
                    exo.prepare()
                    exo.playWhenReady = true
                }
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player?.pause()
            else if (event == Lifecycle.Event.ON_START) player?.play()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun openVlc() {
        val currentUrl = url ?: return
        if (launchVlc(context, currentUrl, channel?.title.orEmpty())) player?.pause()
        else android.widget.Toast.makeText(context, "VLC could not open this stream.", android.widget.Toast.LENGTH_SHORT).show()
    }
    fun revealControls() { controlsVisible = true; controlGeneration++ }
    LaunchedEffect(fullscreen, controlGeneration) {
        if (fullscreen) {
            controlsVisible = true
            delay(3_000)
            controlsVisible = false
            if (controlsHaveFocus) targetView?.requestFocus()
        }
    }
    LaunchedEffect(focusVlcSignal) {
        if (focusVlcSignal > 0) {
            withFrameNanos { }
            runCatching { vlcRequester.requestFocus() }
        }
    }
    fun closeFullscreen() {
        fullscreen = false
        returnCallback()
    }

    @Composable
    fun video(mod: Modifier, full: Boolean) {
        Box(mod.background(Color.Black), contentAlignment = Alignment.Center) {
            if (player != null && error == null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        val view = if (androidBoxVideoCompatibility)
                            LayoutInflater.from(viewContext).inflate(com.example.stbplay.R.layout.player_view_phone, null) as PlayerView
                        else PlayerView(viewContext)
                        view.apply {
                            this.player = player
                            useController = false
                            keepScreenOn = true
                            isFocusable = full
                            isFocusableInTouchMode = full
                            targetView = this
                            setOnKeyListener { _, keyCode, event ->
                                if (!full) return@setOnKeyListener false
                                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                                    if (event.action == KeyEvent.ACTION_UP) closeFullscreen()
                                    true
                                } else if (keyCode in setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE)) {
                                    if (event.action == KeyEvent.ACTION_DOWN) revealControls()
                                    false
                                } else if (event.action == KeyEvent.ACTION_DOWN) {
                                    revealControls()
                                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) focusVlcSignal++
                                    true
                                } else true
                            }
                            if (full) requestFocus()
                        }
                    },
                    update = { it.player = player },
                    onReset = null,
                    onRelease = { it.player = null }
                )
            } else Text(
                error ?: if (request == null) "Select a channel to preview" else "Opening channel…",
                color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(14.dp)
            )
        }
    }

    Column(modifier.background(palette.panel, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!fullscreen) video(Modifier.fillMaxWidth().aspectRatio(16f / 9f), false)
        else Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black))
        Text(channel?.title ?: "Channel preview", color = palette.text, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (channel != null) {
            Text(listOfNotNull(channel.badge, channel.subtitle).filter { it.isNotBlank() }.joinToString(" • "),
                color = palette.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            channel.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        PreviewAction("Full screen", Modifier.focusRequester(actionsRequester),
            onClick = { if (request != null) fullscreen = true })
        if (channel != null) {
            PreviewAction(if (channel.isFavorite) "Remove from favourites" else "Add to favourites", onClick = onFavorite)
            PreviewAction("Not working? Play in VLC", onClick = ::openVlc)
            if (error != null) PreviewAction("Retry", onClick = { retry++ })
            PreviewAction("Back", onClick = onBack)
        }
    }

    if (fullscreen) Dialog(
        onDismissRequest = ::closeFullscreen,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { event ->
            val native = event.nativeKeyEvent
            if (native.keyCode in setOf(KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_MEDIA_NEXT,
                    KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS)) {
                if (native.action == KeyEvent.ACTION_UP)
                    onChannelStep(if (native.keyCode in setOf(KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_MEDIA_NEXT)) 1 else -1)
                true
            } else {
                if (native.action == KeyEvent.ACTION_DOWN) revealControls()
                false
            }
        }) {
            video(Modifier.fillMaxSize(), true)
            if (controlsVisible) {
                Text(channel?.title.orEmpty(), color = Color.White, fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(18.dp).background(Color(0xB3000000), RoundedCornerShape(6.dp)).padding(8.dp))
                TvHomeSurface(
                    onClick = ::openVlc,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).height(24.dp)
                        .focusRequester(vlcRequester)
                        .onFocusChanged { controlsHaveFocus = it.hasFocus },
                    containerColor = Color(0xCC303030)
                ) {
                    Text("Not working? Play in VLC", color = Color.White, fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewAction(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalStbPalette.current
    TvHomeSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(36.dp),
        containerColor = palette.panelSoft
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            Text(title, color = palette.text, fontSize = 12.sp)
        }
    }
}
