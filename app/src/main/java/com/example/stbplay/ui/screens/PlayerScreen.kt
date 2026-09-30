@file:kotlin.OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui.screens

import android.os.Build
import android.os.SystemClock
import android.content.ActivityNotFoundException
import android.app.Activity
import android.content.ContextWrapper
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.runtime.CompositionLocalProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.MediaRouteButton
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import android.view.LayoutInflater
import com.example.stbplay.ui.QuestButton
import com.example.stbplay.ui.QuestSurface
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.example.stbplay.data.PlayerPreference
import com.example.stbplay.data.PortalUrl
import com.example.stbplay.data.StalkerContentKind
import com.example.stbplay.data.StalkerPlayRequest
import com.example.stbplay.data.StalkerPlaybackResolver
import com.example.stbplay.ui.questInitialFocus
import com.example.stbplay.data.SubtitlePreference
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    episodeTitles: List<String> = emptyList(),
    currentEpisodeIndex: Int = -1,
    onEpisodeSelected: (Int) -> Unit = {},
    onPlaybackEnded: () -> Unit = {},
    onChannelStep: (Int) -> Unit = {},
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
            allowSeeking = request.kind != StalkerContentKind.LIVE,
            title = request.title,
            resumeFraction = request.resumeFraction,
            onProgress = onProgress,
            onPlaybackStarted = onPlaybackStarted,
            onPlaybackFailure = onPlaybackFailure,
            episodeTitles = episodeTitles,
            currentEpisodeIndex = currentEpisodeIndex,
            onEpisodeSelected = onEpisodeSelected,
            onPlaybackEnded = onPlaybackEnded,
            onChannelStep = onChannelStep,
            showChannelStepButtons = request.kind == StalkerContentKind.LIVE,
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
    allowSeeking: Boolean,
    title: String,
    resumeFraction: Float,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onPlaybackStarted: () -> Unit,
    onPlaybackFailure: (String) -> Unit,
    episodeTitles: List<String>,
    currentEpisodeIndex: Int,
    onEpisodeSelected: (Int) -> Unit,
    onPlaybackEnded: () -> Unit,
    onChannelStep: (Int) -> Unit,
    showChannelStepButtons: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var playerError by remember(playbackUrl) { mutableStateOf<String?>(null) }
    var didStart by remember(playbackUrl) { mutableStateOf(false) }
    var didRestore by remember(playbackUrl) { mutableStateOf(false) }
    var renderedFirstFrame by remember(playbackUrl) { mutableStateOf(false) }
    var useAlternateSurface by remember(playbackUrl) { mutableStateOf(false) }
    val progressCallback by rememberUpdatedState(onProgress)
    val startedCallback by rememberUpdatedState(onPlaybackStarted)
    val failureCallback by rememberUpdatedState(onPlaybackFailure)
    val endedCallback by rememberUpdatedState(onPlaybackEnded)
    var episodePickerVisible by remember(playbackUrl) { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isAndroidTv = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
    val isMetaQuest = remember(context) {
        val maker = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()
        maker.contains("oculus") || maker.contains("meta") || brand.contains("oculus") || brand.contains("meta") || model.contains("quest")
    }
    // Use the device's stable short-side width so rotating a phone into fullscreen does not
    // replace the player/cast controls with the TV layout.
    val compactLayout = configuration.smallestScreenWidthDp < 900 && !isAndroidTv
    // Match the working v1.8.37 TV path: prefer TextureView first, then try SurfaceView if needed.
    val preferTextureSurface = compactLayout || isAndroidTv
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
        val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) endedCallback()
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
                        val message = "Playback failed: ${error.errorCodeName}"
                        playerError = message
                        failureCallback(message)
                    }

                    override fun onRenderedFirstFrame() {
                        renderedFirstFrame = true
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
    val castPlayer = remember(player, compactLayout) {
        if (compactLayout) CastPlayer.Builder(context).setLocalPlayer(player).build() else null
    }
    val activePlayer: Player = castPlayer ?: player

    var volumePercent by remember(activePlayer) { mutableIntStateOf((activePlayer.volume * 100f).toInt().coerceIn(0, 100)) }
    fun adjustVolume(delta: Int) {
        volumePercent = (volumePercent + delta).coerceIn(0, 100)
        activePlayer.volume = volumePercent / 100f
    }
    var remoteSeekCount by remember(activePlayer) { mutableIntStateOf(0) }
    var lastRemoteSeekAt by remember(activePlayer) { mutableLongStateOf(0L) }
    var lastRemoteSeekDirection by remember(activePlayer) { mutableIntStateOf(0) }
    fun seekByRemote(direction: Int) {
        if (!activePlayer.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) return
        val now = SystemClock.elapsedRealtime()
        if (direction != lastRemoteSeekDirection || now - lastRemoteSeekAt > 1_200L) remoteSeekCount = 0
        remoteSeekCount += 1
        lastRemoteSeekDirection = direction
        lastRemoteSeekAt = now
        val stepMs = when (remoteSeekCount) {
            in 1..3 -> 15_000L
            in 4..6 -> 30_000L
            in 7..11 -> 60_000L
            else -> 120_000L
        }
        val target = (activePlayer.currentPosition + direction.toLong() * stepMs)
            .coerceIn(0L, activePlayer.duration.takeIf { it > 0L } ?: Long.MAX_VALUE)
        activePlayer.seekTo(target)
    }
    var playerControlsVisible by remember(playbackUrl) { mutableStateOf(false) }
    var controlsInteraction by remember(playbackUrl) { mutableIntStateOf(0) }
    var pointerInsidePlayer by remember(playbackUrl) { mutableStateOf(false) }
    fun revealPlayerControls() {
        playerControlsVisible = true
        controlsInteraction++
    }
    LaunchedEffect(controlsInteraction, playerControlsVisible, pointerInsidePlayer) {
        if (playerControlsVisible && !pointerInsidePlayer) {
            delay(3_000)
            playerControlsVisible = false
        }
    }

    val phoneActivity = remember(context) { context.findActivity() }
    val originalOrientation = remember(phoneActivity) {
        phoneActivity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    var phoneFullscreen by remember(playbackUrl) { mutableStateOf(false) }
    fun setPhoneFullscreen(enabled: Boolean) {
        phoneActivity?.let { applyPhoneFullscreen(it, enabled, originalOrientation) }
        phoneFullscreen = enabled
        revealPlayerControls()
    }
    // A rotation can make Android restore the system bars after the click handler ran.
    // Re-apply immersive mode once Compose observes the new orientation as well.
    LaunchedEffect(phoneFullscreen, configuration.orientation) {
        if (phoneFullscreen) {
            delay(180)
            phoneActivity?.let { applyPhoneFullscreen(it, true, originalOrientation) }
        }
    }
    BackHandler(enabled = compactLayout && phoneActivity != null && phoneFullscreen) {
        setPhoneFullscreen(false)
    }
    // Quest's controller Back action can be delivered as a system back event,
    // independently of focus inside the native Media3 view.
    BackHandler(enabled = isMetaQuest) { onBack() }
    DisposableEffect(phoneActivity, phoneFullscreen) {
        onDispose {
            if (phoneFullscreen) phoneActivity?.let { applyPhoneFullscreen(it, false, originalOrientation) }
        }
    }

    DisposableEffect(player, castPlayer) {
        onDispose {
            runCatching {
                if (activePlayer.duration > 0L && activePlayer.currentPosition > 0L) progressCallback(activePlayer.currentPosition, activePlayer.duration)
            }
            castPlayer?.release()
            player.release()
        }
    }
    LaunchedEffect(activePlayer) {
        while (true) {
            delay(5_000)
            if (activePlayer.duration > 0L && activePlayer.currentPosition > 0L) progressCallback(activePlayer.currentPosition, activePlayer.duration)
        }
    }
    LaunchedEffect(player, didStart, renderedFirstFrame, useAlternateSurface) {
        if (!didStart || renderedFirstFrame || playerError != null) return@LaunchedEffect
        delay(4_000)
        if (renderedFirstFrame || playerError != null || !player.isPlaying) return@LaunchedEffect
        if (!useAlternateSurface) {
            useAlternateSurface = true
        } else {
            val message = "Video did not appear on this device."
            playerError = message
            failureCallback(message)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        key(useAlternateSurface) { AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                (if (preferTextureSurface != useAlternateSurface) {
                    LayoutInflater.from(viewContext).inflate(com.example.stbplay.R.layout.player_view_phone, null) as SeekablePlayerView
                } else SeekablePlayerView(viewContext)).apply {
                    this.player = activePlayer
                    onSeekDirection = if (allowSeeking) ({ direction ->
                        seekByRemote(direction)
                        revealPlayerControls()
                        showController()
                    }) else null
                    useController = true
                    controllerShowTimeoutMs = 3_000
                    keepScreenOn = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    if (isMetaQuest) post {
                        expandSeekTouchTarget((72f * resources.displayMetrics.density).roundToInt())
                    }
                    setOnKeyListener { _, keyCode, event ->
                        when (keyCode) {
                            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BUTTON_B -> {
                                if (event.action == KeyEvent.ACTION_UP) onBack()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                if (event.action == KeyEvent.ACTION_UP) {
                                    when (keyCode) {
                                        KeyEvent.KEYCODE_MEDIA_PLAY -> activePlayer.play()
                                        KeyEvent.KEYCODE_MEDIA_PAUSE -> activePlayer.pause()
                                        else -> if (activePlayer.isPlaying) activePlayer.pause() else activePlayer.play()
                                    }
                                    revealPlayerControls()
                                    showController()
                                }
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND,
                            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                revealPlayerControls()
                                showController()
                                if (activePlayer.isCurrentMediaItemSeekable) {
                                    if (event.action == KeyEvent.ACTION_UP) {
                                        val direction = if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                            keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) -1 else 1
                                        seekByRemote(direction)
                                    }
                                    true
                                } else false
                            }
                            else -> {
                                if (event.action == KeyEvent.ACTION_DOWN) {
                                    revealPlayerControls()
                                    showController()
                                }
                                false
                            }
                        }
                    }
                    setOnTouchListener { _, event ->
                        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                            revealPlayerControls()
                            showController()
                        }
                        false
                    }
                    setOnHoverListener { _, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_HOVER_ENTER -> {
                                pointerInsidePlayer = true
                                revealPlayerControls()
                            }
                            MotionEvent.ACTION_HOVER_MOVE -> {
                                pointerInsidePlayer = true
                            }
                            MotionEvent.ACTION_HOVER_EXIT -> {
                                pointerInsidePlayer = false
                                controlsInteraction++
                            }
                        }
                        false
                    }
                    post { requestFocus() }
                }
            },
            update = { if (it.player !== activePlayer) it.player = activePlayer }
        ) }
        if (playerControlsVisible && playerError == null) {
            if (!isAndroidTv && !isMetaQuest) Row(
                modifier = Modifier.align(Alignment.TopStart).padding(if (compactLayout) 12.dp else 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuestButton(
                    onClick = { revealPlayerControls(); onBack() },
                    modifier = Modifier.widthIn(min = if (compactLayout) 108.dp else 116.dp).heightIn(min = 48.dp)
                        .onFocusChanged { if (it.hasFocus) revealPlayerControls() }
                        .questInitialFocus(),
                    colors = ButtonDefaults.colors(containerColor = Color(0xCC070707), contentColor = Color.White)
                ) { Text("←  Back", fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
                if (compactLayout && !isMetaQuest && phoneActivity != null) {
                    IconButton(
                        onClick = { setPhoneFullscreen(!phoneFullscreen) },
                        modifier = Modifier.size(48.dp).background(Color(0xCC070707), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (phoneFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = if (phoneFullscreen) "Exit full screen" else "Full screen",
                            tint = Color.White
                        )
                    }
                }
            }
            if (compactLayout && !isMetaQuest) Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(52.dp)
                    .background(Color(0xCC070707), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    MediaRouteButton(modifier = Modifier.size(44.dp))
                }
            }
            if (episodeTitles.isNotEmpty()) QuestButton(
                onClick = { episodePickerVisible = true; revealPlayerControls() },
                modifier = Modifier.align(Alignment.TopCenter).padding(18.dp)
            ) { Text("Episodes") }
            if (isMetaQuest) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp)
                        .background(Color(0x99070707), RoundedCornerShape(12.dp)).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuestButton(
                        onClick = { revealPlayerControls(); onBack() },
                        modifier = Modifier.widthIn(min = 92.dp).height(52.dp),
                        colors = ButtonDefaults.colors(containerColor = Color(0xAA070707), contentColor = Color.White)
                    ) { Text("← Back", fontSize = 15.sp) }
                    if (showChannelStepButtons) {
                        ChannelStepControl(Icons.Filled.SkipPrevious, "Previous channel", {
                            onChannelStep(-1); revealPlayerControls()
                        }, cornerRadius = 7.dp)
                        ChannelStepControl(Icons.Filled.SkipNext, "Next channel", {
                            onChannelStep(1); revealPlayerControls()
                        }, cornerRadius = 7.dp)
                    }
                }
            }
            if (showChannelStepButtons && compactLayout && !isMetaQuest) {
                if (compactLayout && !isMetaQuest) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)
                            .background(Color(0x99070707), RoundedCornerShape(9.dp)).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChannelStepControl(Icons.Filled.SkipPrevious, "Previous channel", {
                            onChannelStep(-1); revealPlayerControls()
                        }, cornerRadius = 5.dp)
                        ChannelStepControl(Icons.Filled.SkipNext, "Next channel", {
                            onChannelStep(1); revealPlayerControls()
                        }, cornerRadius = 5.dp)
                    }
                } else Column(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 42.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QuestButton(
                        onClick = { onChannelStep(-1); revealPlayerControls() },
                        modifier = Modifier.width(68.dp).height(58.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xAA070707), contentColor = Color.White,
                            focusedContainerColor = Color(0xCC070707), focusedContentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous channel", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    QuestButton(
                        onClick = { onChannelStep(1); revealPlayerControls() },
                        modifier = Modifier.width(68.dp).height(58.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xAA070707), contentColor = Color.White,
                            focusedContainerColor = Color(0xCC070707), focusedContentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next channel", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
            if (isMetaQuest) {
                MetaVolumeControl(
                    volumePercent = volumePercent,
                    onVolumeChange = { value ->
                        volumePercent = value.coerceIn(0, 100)
                        activePlayer.volume = volumePercent / 100f
                        revealPlayerControls()
                    },
                    onMuteToggle = {
                        if (volumePercent == 0) {
                            volumePercent = 35
                            activePlayer.volume = 0.35f
                        } else {
                            volumePercent = 0
                            activePlayer.volume = 0f
                        }
                        revealPlayerControls()
                    },
                    onMaxVolume = {
                        volumePercent = 100
                        activePlayer.volume = 1f
                        revealPlayerControls()
                    },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp)
                )
            }
            if (!isAndroidTv && !isMetaQuest && !compactLayout) {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(18.dp)
                        .onFocusChanged { if (it.hasFocus) revealPlayerControls() }
                        .background(Color(0xCC070707)).padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Volume $volumePercent%", color = Color.White)
                    QuestButton(onClick = { adjustVolume(-10); revealPlayerControls() }) { Text("−") }
                    QuestButton(onClick = { adjustVolume(10); revealPlayerControls() }) { Text("+") }
                }
            }
        }
        if (episodePickerVisible) {
            Column(
                modifier = Modifier.align(Alignment.Center).width(320.dp).heightIn(max = 470.dp)
                    .background(Color(0xF5111111), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Episodes", color = Color.White)
                LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(episodeTitles) { index, name ->
                        QuestButton(onClick = { episodePickerVisible = false; onEpisodeSelected(index) },
                            modifier = Modifier.fillMaxWidth()) {
                            Text("${index + 1}. $name${if (index == currentEpisodeIndex) " • Playing" else ""}")
                        }
                    }
                }
                QuestButton(onClick = { episodePickerVisible = false }) { Text("Close") }
            }
        }
        playerError?.let { error ->
            Column(
                modifier = Modifier.align(Alignment.Center).background(Color(0xEE070707)).padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(error, color = Color.White)
                if (isVlcInstalled(context)) QuestButton(onClick = {
                    if (launchVlc(context, playbackUrl, title)) activePlayer.pause()
                    else playerError = "VLC could not open this stream."
                }) { Text("Open in VLC") }
                if (!isAndroidTv) QuestButton(onClick = onBack, modifier = Modifier.questInitialFocus()) { Text("Back") }
            }
        }
    }
}

@Composable
private fun ChannelStepControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    cornerRadius: Dp
) {
    var focused by remember { mutableStateOf(false) }
    QuestSurface(
        onClick = onClick,
        modifier = Modifier.size(56.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(cornerRadius)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x99070707),
            focusedContainerColor = Color(0xBB070707)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(if (focused) 32.dp else 30.dp)
        )
    }
}

@Composable
private fun MetaVolumeControl(
    volumePercent: Int,
    onVolumeChange: (Int) -> Unit,
    onMuteToggle: () -> Unit,
    onMaxVolume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(Color(0x99070707), RoundedCornerShape(12.dp)).padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        QuestButton(
            onClick = onMaxVolume,
            modifier = Modifier.width(54.dp).height(40.dp),
            colors = ButtonDefaults.colors(containerColor = Color(0xAA070707), contentColor = Color.White)
        ) { Icon(Icons.Filled.VolumeUp, contentDescription = "Maximum volume", tint = Color.White) }
        Box(
            modifier = Modifier.width(54.dp).height(150.dp)
                .background(Color(0xFF202020), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectTapGestures { point ->
                        onVolumeChange((100f * (1f - point.y / size.height)).roundToInt())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onVolumeChange((100f * (1f - change.position.y / size.height)).roundToInt())
                        change.consume()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.width(17.dp).fillMaxHeight().padding(vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                (9 downTo 0).forEach { level ->
                    Box(
                        Modifier.fillMaxWidth().weight(1f)
                            .background(
                                if ((level + 1) * 10 <= volumePercent) Color(0xFFFFD36B)
                                else Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }
        QuestButton(
            onClick = onMuteToggle,
            modifier = Modifier.width(54.dp).height(42.dp),
            colors = ButtonDefaults.colors(containerColor = Color(0xAA070707), contentColor = Color.White)
        ) {
            Icon(
                if (volumePercent == 0) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                contentDescription = if (volumePercent == 0) "Unmute" else "Mute",
                tint = Color.White
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

private fun applyPhoneFullscreen(activity: Activity, enabled: Boolean, originalOrientation: Int) {
    if (enabled) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.decorView.post {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    } else {
        activity.requestedOrientation = originalOrientation
        WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }
}

private fun isVlcInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo("org.videolan.vlc", 0)
    true
} catch (_: Throwable) {
    false
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
    val context = LocalContext.current
    val isAndroidTv = remember(context) {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF070707)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(30.dp)) {
            Text("Unable to start playback", color = Color.White)
            Text(message, color = Color.LightGray)
            QuestButton(onClick = onRetry, modifier = Modifier.questInitialFocus()) { Text("Retry") }
            if (!isAndroidTv) QuestButton(onClick = onBack) { Text("Back") }
        }
    }
}
