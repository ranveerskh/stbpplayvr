@file:kotlin.OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.stbplay.ui.QuestButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.example.stbplay.data.PlayerPreference
import com.example.stbplay.data.PortalUrl
import com.example.stbplay.data.StalkerContentKind
import com.example.stbplay.data.StalkerPlayRequest
import com.example.stbplay.data.StalkerPlaybackResolver
import com.example.stbplay.ui.questInitialFocus
import com.example.stbplay.data.SubtitlePreference
import kotlinx.coroutines.delay

@Composable
fun PlaybackRoute(
    request: StalkerPlayRequest,
    portalUiUrl: String,
    macAddress: String,
    token: String?,
    sessionCookie: String = "",
    playerPreference: PlayerPreference = PlayerPreference.AUTO,
    subtitlePreference: SubtitlePreference = SubtitlePreference.AUTO,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onPlaybackStarted: () -> Unit = {},
    onPlaybackFailure: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resolver = remember(portalUiUrl, macAddress, token, sessionCookie) {
        StalkerPlaybackResolver(portalUiUrl, macAddress, token, sessionCookie)
    }
    var playbackUrl by remember(request) { mutableStateOf<String?>(null) }
    var errorMessage by remember(request) { mutableStateOf<String?>(null) }
    var retryNumber by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onBack)
    LaunchedEffect(request, retryNumber) {
        playbackUrl = null
        errorMessage = null
        runCatching { resolver.resolve(request) }
            .onSuccess { playbackUrl = it }
            .onFailure {
                val message = it.message ?: "Content temporarily unavailable"
                errorMessage = message
                onPlaybackFailure(message)
            }
    }

    when {
        errorMessage != null -> PlaybackErrorScreen(errorMessage!!, { retryNumber++ }, onBack)
        playbackUrl == null -> PlaybackLoadingScreen(
            when (request.kind) {
                StalkerContentKind.LIVE -> "Opening channel…"
                StalkerContentKind.MOVIE -> "Loading movie…"
                StalkerContentKind.SERIES_EPISODE -> "Loading episode…"
            }
        )
        playerPreference == PlayerPreference.VLC -> {
            LaunchedEffect(playbackUrl) {
                val opened = launchVlc(context, playbackUrl!!, request.title)
                if (opened) onPlaybackStarted() else {
                    errorMessage = "VLC is not installed on this device. Select Internal player in Settings."
                    onPlaybackFailure(errorMessage!!)
                }
            }
            PlaybackLoadingScreen("Opening VLC…")
        }
        else -> NativePlayerScreen(
            playbackUrl = playbackUrl!!,
            portalUiUrl = portalUiUrl,
            token = token,
            sessionCookie = sessionCookie,
            playerPreference = playerPreference,
            subtitlePreference = subtitlePreference,
            title = request.title,
            resumeFraction = request.resumeFraction,
            onProgress = onProgress,
            onPlaybackStarted = onPlaybackStarted,
            onPlaybackFailure = onPlaybackFailure,
            onBack = onBack
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun NativePlayerScreen(
    playbackUrl: String,
    portalUiUrl: String,
    token: String?,
    sessionCookie: String,
    playerPreference: PlayerPreference,
    subtitlePreference: SubtitlePreference,
    title: String,
    resumeFraction: Float,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onPlaybackStarted: () -> Unit,
    onPlaybackFailure: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var playerError by remember(playbackUrl) { mutableStateOf<String?>(null) }
    var decoderError by remember(playbackUrl) { mutableStateOf(false) }
    var didStart by remember(playbackUrl) { mutableStateOf(false) }
    var didRestore by remember(playbackUrl) { mutableStateOf(false) }
    var autoVlcTried by remember(playbackUrl) { mutableStateOf(false) }
    val progressCallback by rememberUpdatedState(onProgress)
    val startedCallback by rememberUpdatedState(onPlaybackStarted)
    val failureCallback by rememberUpdatedState(onPlaybackFailure)

    val player = remember(playbackUrl, portalUiUrl, token, sessionCookie, subtitlePreference, resumeFraction) {
        val headers = mutableMapOf(
            "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 MAG254",
            "Referer" to PortalUrl.refererUrl(portalUiUrl)
        )
        if (!token.isNullOrBlank()) headers["Authorization"] = "Bearer $token"
        if (sessionCookie.isNotBlank()) headers["Cookie"] = sessionCookie

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(headers["User-Agent"]!!)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)
        val trackSelector = DefaultTrackSelector(context).apply {
            val language = subtitlePreference.languageCode()
            if (language != null) {
                setParameters(buildUponParameters().setPreferredTextLanguage(language).setSelectUndeterminedTextLanguage(false))
            } else if (subtitlePreference == SubtitlePreference.OFF) {
                setParameters(buildUponParameters().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true))
            }
        }
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            if (!didRestore && resumeFraction > 0f && exoPlayer.duration > 0L) {
                                didRestore = true
                                exoPlayer.seekTo((exoPlayer.duration * resumeFraction.coerceIn(0f, 0.94f)).toLong())
                            }
                            if (!didStart) {
                                didStart = true
                                startedCallback()
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        decoderError = error.errorCode in setOf(
                            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                            PlaybackException.ERROR_CODE_DECODING_FAILED,
                            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
                        )
                        val message = "Playback failed: ${error.errorCodeName}"
                        playerError = message
                        failureCallback(message)
                    }
                })
                val item = MediaItem.Builder().setUri(playbackUrl).apply {
                    if (playbackUrl.substringBefore('?').endsWith(".m3u8", true)) setMimeType(MimeTypes.APPLICATION_M3U8)
                }.build()
                exoPlayer.setMediaItem(item)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
    }

    var volumePercent by remember(player) { mutableIntStateOf((player.volume * 100f).toInt().coerceIn(0, 100)) }
    fun adjustVolume(delta: Int) {
        volumePercent = (volumePercent + delta).coerceIn(0, 100)
        player.volume = volumePercent / 100f
    }
    var playerControlsVisible by remember(playbackUrl) { mutableStateOf(false) }
    var controlsInteraction by remember(playbackUrl) { mutableIntStateOf(0) }
    fun revealPlayerControls() {
        playerControlsVisible = true
        controlsInteraction++
    }
    LaunchedEffect(controlsInteraction, playerControlsVisible) {
        if (playerControlsVisible) {
            delay(3_000)
            playerControlsVisible = false
        }
    }

    DisposableEffect(player) {
        onDispose {
            runCatching {
                if (player.duration > 0L && player.currentPosition > 0L) progressCallback(player.currentPosition, player.duration)
            }
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(5_000)
            if (player.duration > 0L && player.currentPosition > 0L) progressCallback(player.currentPosition, player.duration)
        }
    }
    LaunchedEffect(playerError, playerPreference, decoderError) {
        if (playerError == null) return@LaunchedEffect
        if (playerPreference == PlayerPreference.AUTO && decoderError && !autoVlcTried) {
            autoVlcTried = true
            if (launchVlc(context, playbackUrl, title)) {
                playerError = "Opening VLC because this device could not decode the stream."
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    controllerShowTimeoutMs = 3_000
                    keepScreenOn = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setOnKeyListener { _, keyCode, event ->
                        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) && event.action == KeyEvent.ACTION_UP) {
                            onBack()
                            true
                        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                            true
                        } else {
                            if (event.action == KeyEvent.ACTION_DOWN) {
                                revealPlayerControls()
                                showController()
                            }
                            false
                        }
                    }
                    setOnTouchListener { _, event ->
                        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                            revealPlayerControls()
                            showController()
                        }
                        false
                    }
                    post { requestFocus() }
                }
            },
            update = { it.player = player }
        )
        if (playerControlsVisible && playerError == null) {
            QuestButton(
                onClick = { revealPlayerControls(); onBack() },
                modifier = Modifier.align(Alignment.TopStart).padding(18.dp)
                    .onFocusChanged { if (it.hasFocus) revealPlayerControls() }
                    .questInitialFocus(),
                colors = ButtonDefaults.colors(containerColor = Color(0xCC070707), contentColor = Color.White)
            ) { Text("← Back") }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(18.dp)
                    .onFocusChanged { if (it.hasFocus) revealPlayerControls() }
                    .background(Color(0xCC070707)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Volume $volumePercent%", color = Color.White)
                QuestButton(onClick = { adjustVolume(-10); revealPlayerControls() }) { Text("−") }
                QuestButton(onClick = { adjustVolume(10); revealPlayerControls() }) { Text("+") }
            }
        }
        playerError?.let { error ->
            Column(
                modifier = Modifier.align(Alignment.Center).background(Color(0xEE070707)).padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(error, color = Color.White)
                if (playerPreference == PlayerPreference.AUTO && autoVlcTried) Text("VLC handoff attempted.", color = Color.LightGray)
                QuestButton(onClick = onBack, modifier = Modifier.questInitialFocus()) { Text("Back") }
            }
        }
    }
}

private fun SubtitlePreference.languageCode(): String? = when (this) {
    SubtitlePreference.ENGLISH -> "en"
    SubtitlePreference.HINDI -> "hi"
    SubtitlePreference.PUNJABI -> "pa"
    else -> null
}

private fun launchVlc(context: Context, stream: String, title: String): Boolean = try {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(stream), "video/*")
        .setPackage("org.videolan.vlc")
        .putExtra("title", title)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: Throwable) {
    false
}

@Composable
private fun PlaybackLoadingScreen(title: String) {
    Box(Modifier.fillMaxSize().background(Color(0xFF070707)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            CircularProgressIndicator(color = Color(0xFFDDB32F))
            Text(title, color = Color.White)
        }
    }
}

@Composable
private fun PlaybackErrorScreen(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF070707)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(30.dp)) {
            Text("Unable to start playback", color = Color.White)
            Text(message, color = Color.LightGray)
            QuestButton(onClick = onRetry, modifier = Modifier.questInitialFocus()) { Text("Retry") }
            QuestButton(onClick = onBack) { Text("Back") }
        }
    }
}
