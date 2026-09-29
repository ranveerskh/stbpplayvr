package com.example.stbplay

import android.app.DownloadManager
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.media3.cast.Cast
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn as AndroidXOptIn
import com.example.stbplay.data.PlayerPreference
import com.example.stbplay.data.ParentalMode
import com.example.stbplay.data.PortalRepository
import com.example.stbplay.data.CatalogCacheStore
import com.example.stbplay.data.CatalogSnapshot
import com.example.stbplay.data.CachedVodCatalog
import com.example.stbplay.data.SettingsManager
import com.example.stbplay.data.LauncherIconManager
import com.example.stbplay.data.StalkerContentKind
import com.example.stbplay.data.StalkerPlayRequest
import com.example.stbplay.data.ThemePreference
import com.example.stbplay.data.UpdateInfo
import com.example.stbplay.data.UpdateCheckWorker
import com.example.stbplay.data.UpdateManager
import com.example.stbplay.data.model.PortalCategory
import com.example.stbplay.data.model.PortalEpisode
import com.example.stbplay.data.model.PortalQualityOption
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.data.model.PortalSubscription
import com.example.stbplay.domain.model.PortalSettings
import com.example.stbplay.ui.ChangePinPrompt
import com.example.stbplay.ui.FirstStartDisclaimer
import com.example.stbplay.ui.PinPrompt
import com.example.stbplay.ui.StbPlayApp
import com.example.stbplay.ui.StbPlayHomeState
import com.example.stbplay.ui.StbPlayLibraryState
import com.example.stbplay.ui.StbPlaySettingsState
import com.example.stbplay.ui.StbPlayTab
import com.example.stbplay.ui.UiCategory
import com.example.stbplay.ui.UiMedia
import com.example.stbplay.ui.UiMediaRow
import com.example.stbplay.ui.screens.LoadingScreen
import com.example.stbplay.ui.screens.MovieDetailsScreen
import com.example.stbplay.ui.screens.PlaybackRoute
import com.example.stbplay.ui.screens.QualitySelectionScreen
import com.example.stbplay.ui.screens.SeriesDetailsScreen
import com.example.stbplay.ui.screens.SetupScreen
import com.example.stbplay.ui.screens.UpdateNotice
import com.example.stbplay.ui.theme.STBPlayTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private enum class AppScreen { SETUP, LOADING, APP }

private data class QualityContext(
    val title: String,
    val stream: PortalStream,
    val episode: PortalEpisode? = null,
    val resumeFraction: Float = 0f
)

private data class PendingCategory(val tab: StbPlayTab, val index: Int, val title: String)

private data class VodCatalogState(
    val items: List<PortalStream> = emptyList(),
    val nextPage: Int = 0,
    val totalItems: Int? = null,
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null
)

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) updateManager.cachedAvailable(BuildConfig.VERSION_NAME)?.let(updateManager::notifyIfNew)
    }

    private lateinit var settingsManager: SettingsManager
    private lateinit var updateManager: UpdateManager
    private var openUpdates by mutableStateOf(false)
    private var pendingUpdateDownloadId = -1L
    private var lastControllerDirection = 0
    private var lastControllerDirectionAt = 0L
    private var controllerTriggerHeld = false
    internal var channelStepHandler: ((Int) -> Unit)? = null

    /** Quest controllers may arrive as gamepad keys instead of TV remote keys. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val channelStep = when (event.keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> 1
            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> -1
            else -> 0
        }
        if (channelStep != 0 && channelStepHandler != null) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) channelStepHandler?.invoke(channelStep)
            return true
        }
        val mappedKeyCode = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_R2 -> KeyEvent.KEYCODE_DPAD_CENTER
            KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK
            else -> return super.dispatchKeyEvent(event)
        }

        val mapped = KeyEvent(
            event.downTime,
            event.eventTime,
            event.action,
            mappedKeyCode,
            event.repeatCount,
            event.metaState,
            event.deviceId,
            0,
            event.flags,
            InputDevice.SOURCE_DPAD
        )
        super.dispatchKeyEvent(mapped)
        return true
    }

    /** Translate joystick axes and the Quest right trigger into focused TV controls. */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val isController = event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
            event.isFromSource(InputDevice.SOURCE_GAMEPAD)
        if (!isController || event.action != MotionEvent.ACTION_MOVE) {
            return super.dispatchGenericMotionEvent(event)
        }

        var handled = false
        val trigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
        if (trigger >= 0.65f && !controllerTriggerHeld) {
            dispatchControllerKey(KeyEvent.KEYCODE_DPAD_CENTER, event.deviceId, event.eventTime)
            controllerTriggerHeld = true
            handled = true
        } else if (trigger <= 0.3f) {
            controllerTriggerHeld = false
        }

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val axisX = if (kotlin.math.abs(hatX) >= 0.55f) hatX else event.getAxisValue(MotionEvent.AXIS_X)
        val axisY = if (kotlin.math.abs(hatY) >= 0.55f) hatY else event.getAxisValue(MotionEvent.AXIS_Y)
        val direction = when {
            kotlin.math.abs(axisX) >= 0.55f && kotlin.math.abs(axisX) >= kotlin.math.abs(axisY) ->
                if (axisX < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
            kotlin.math.abs(axisY) >= 0.55f ->
                if (axisY < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
            else -> 0
        }

        if (direction == 0) {
            lastControllerDirection = 0
            lastControllerDirectionAt = 0L
        } else if (direction != lastControllerDirection || event.eventTime - lastControllerDirectionAt >= 150L) {
            dispatchControllerKey(direction, event.deviceId, event.eventTime)
            lastControllerDirection = direction
            lastControllerDirectionAt = event.eventTime
            handled = true
        }

        return if (handled) true else super.dispatchGenericMotionEvent(event)
    }

    private fun dispatchControllerKey(keyCode: Int, deviceId: Int, eventTime: Long) {
        val down = KeyEvent(
            eventTime,
            eventTime,
            KeyEvent.ACTION_DOWN,
            keyCode,
            0,
            0,
            deviceId,
            0,
            0,
            InputDevice.SOURCE_DPAD
        )
        super.dispatchKeyEvent(down)
        super.dispatchKeyEvent(KeyEvent.changeAction(down, KeyEvent.ACTION_UP))
    }

    private val updateDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id > 0L && (id == pendingUpdateDownloadId || id == updateManager.pendingDownloadId())) {
                pendingUpdateDownloadId = -1L
                runCatching { updateManager.openInstaller(id) }
            }
        }
    }

    @AndroidXOptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openUpdates = intent?.getBooleanExtra("stb_open_updates", false) == true
        if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)) {
            runCatching { Cast.getSingletonInstance(this).initialize() }
        }
        settingsManager = SettingsManager(this)
        updateManager = UpdateManager(applicationContext)
        UpdateCheckWorker.schedule(applicationContext)
        registerUpdateReceiver()

        setContent {
            val themePreference by settingsManager.themePreference.collectAsState(initial = ThemePreference.BLUE)
            val parentalMode by settingsManager.parentalMode.collectAsState(initial = ParentalMode.ALL_CONTENT)
            LaunchedEffect(themePreference) { LauncherIconManager.apply(applicationContext, themePreference) }
            STBPlayTheme(preference = themePreference, adultOnly = parentalMode == ParentalMode.ADULT_ONLY) {
                StbPlayRoot(settingsManager, updateManager, ::queueUpdateDownload, ::shareApp, openUpdates)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val prefs = getSharedPreferences("android_updates", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("notification_permission_asked", false)) {
                prefs.edit().putBoolean("notification_permission_asked", true).apply()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::updateManager.isInitialized) {
            val pending = updateManager.pendingDownloadId()
            if (pending > 0) runCatching { updateManager.openInstaller(pending) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openUpdates = intent.getBooleanExtra("stb_open_updates", false)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(updateDownloadReceiver) }
        super.onDestroy()
    }

    private fun registerUpdateReceiver() {
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateDownloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(updateDownloadReceiver, filter)
        }
    }

    private fun queueUpdateDownload(info: UpdateInfo): Result<Long> = runCatching {
        updateManager.download(info).also { pendingUpdateDownloadId = it }
    }

    private fun shareApp() {
        runCatching {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, "STB Play for Android TV"),
                    "Share STB Play"
                )
            )
        }
    }
}

@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
private fun StbPlayRoot(
    settingsManager: SettingsManager,
    updateManager: UpdateManager,
    queueUpdateDownload: (UpdateInfo) -> Result<Long>,
    onShare: () -> Unit,
    openUpdates: Boolean
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    val catalogCache = remember(appContext) { CatalogCacheStore(appContext) }
    val contentGridState = rememberLazyGridState()
    val liveChannelListState = rememberLazyListState()
    val storedSettings by settingsManager.portalSettings.collectAsState(initial = PortalSettings())
    val profiles by settingsManager.portalProfiles.collectAsState(initial = emptyList())
    val favoriteIds by settingsManager.favoriteIds.collectAsState(initial = emptySet())
    val progressById by settingsManager.vodProgress.collectAsState(initial = emptyMap())
    val playerPreference by settingsManager.playerPreference.collectAsState(initial = PlayerPreference.AUTO)
    val subtitlePreference by settingsManager.subtitlePreference.collectAsState(initial = com.example.stbplay.data.SubtitlePreference.AUTO)
    val themePreference by settingsManager.themePreference.collectAsState(initial = ThemePreference.BLUE)
    val parentalMode by settingsManager.parentalMode.collectAsState(initial = ParentalMode.ALL_CONTENT)
    val catalogueLanguage by settingsManager.catalogueLanguage.collectAsState(initial = "All")
    val analyticsEnabled by settingsManager.analyticsEnabled.collectAsState(initial = true)
    val disclaimerAcknowledged by settingsManager.disclaimerAcknowledged.collectAsState(initial = false)
    val lastRefreshAt by settingsManager.lastRefreshAt.collectAsState(initial = 0L)

    val portalRepository = remember { PortalRepository() }
    var screen by remember { mutableStateOf(AppScreen.SETUP) }
    var loadingStage by remember { mutableStateOf("Preparing portal…") }
    var loadingProgress by remember { mutableStateOf(0f) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    var autoConnectKey by remember { mutableStateOf<String?>(null) }

    var liveStreams by remember { mutableStateOf<List<PortalStream>>(emptyList()) }
    var movieStreams by remember { mutableStateOf<List<PortalStream>>(emptyList()) }
    var seriesStreams by remember { mutableStateOf<List<PortalStream>>(emptyList()) }
    var remoteSearchStreams by remember { mutableStateOf<List<PortalStream>>(emptyList()) }
    var vodCatalogs by remember { mutableStateOf<Map<String, VodCatalogState>>(emptyMap()) }
    var categoryPreloadJob by remember { mutableStateOf<Job?>(null) }
    var searchVisible by remember { mutableStateOf(false) }
    var loadingLiveCategoryId by remember { mutableStateOf<String?>(null) }
    var liveCategoryError by remember { mutableStateOf<String?>(null) }
    var catalogGeneration by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var liveCategories by remember { mutableStateOf<List<PortalCategory>>(emptyList()) }
    var movieCategories by remember { mutableStateOf<List<PortalCategory>>(emptyList()) }
    var seriesCategories by remember { mutableStateOf<List<PortalCategory>>(emptyList()) }
    var subscription by remember { mutableStateOf(PortalSubscription()) }

    var selectedTab by remember { mutableStateOf(StbPlayTab.HOME) }
    var liveCategoryIndex by remember { mutableStateOf(0) }
    var contentCategoryIndex by remember { mutableStateOf(0) }
    var focusedLiveChannelId by remember { mutableStateOf<String?>(null) }
    var selectedMovie by remember { mutableStateOf<PortalStream?>(null) }
    var selectedSeries by remember { mutableStateOf<PortalStream?>(null) }
    var qualityContext by remember { mutableStateOf<QualityContext?>(null) }
    var qualityOptions by remember { mutableStateOf<List<PortalQualityOption>>(emptyList()) }
    var qualityLoading by remember { mutableStateOf(false) }
    var qualityError by remember { mutableStateOf<String?>(null) }
    var playRequest by remember { mutableStateOf<StalkerPlayRequest?>(null) }
    var playingSeries by remember { mutableStateOf<PortalStream?>(null) }
    var playingEpisodes by remember { mutableStateOf<List<PortalEpisode>>(emptyList()) }
    var playingEpisodeIndex by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    var pendingLockedMedia by remember { mutableStateOf<PortalStream?>(null) }
    var pendingCategory by remember { mutableStateOf<PendingCategory?>(null) }
    var pendingParentalMode by remember { mutableStateOf<ParentalMode?>(null) }
    var unlockedAdultCategoryKey by remember { mutableStateOf<String?>(null) }
    var editingPortal by remember { mutableStateOf<PortalSettings?>(null) }
    var changingPin by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf(updateManager.cachedAvailable(BuildConfig.VERSION_NAME)) }
    var updateText by remember { mutableStateOf(updateInfo?.let { "STB Play ${it.version} is available. Update by ${it.deadlineText()}." }
        ?: "Automatic update checks are on. You can check now too.") }
    var promptUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    fun checkUpdates(manual: Boolean = false) {
        if (manual) updateText = "Checking for updates…"
        scope.launch {
            runCatching { updateManager.check(BuildConfig.VERSION_NAME) }
                .onSuccess { checkResult ->
                    val result = checkResult.update
                    updateInfo = result
                    updateText = when {
                        result != null -> "STB Play ${result.version} is available. Update by ${result.deadlineText()}."
                        !checkResult.hasPublishedRelease -> "No update has been published yet. Automatic checks remain on."
                        else -> "STB Play is up to date. Automatic checks remain on."
                    }
                    if (result != null) {
                        updateManager.notifyIfNew(result)
                        if (updateManager.shouldPrompt(result)) promptUpdate = result
                    } else promptUpdate = null
                }
                .onFailure {
                    updateText = if (updateInfo == null) "Could not check for updates. Try again later."
                    else "Offline. Last known update: ${updateInfo!!.version}, due ${updateInfo!!.deadlineText()}."
                }
        }
    }

    LaunchedEffect(Unit) {
        updateInfo?.takeIf(updateManager::shouldPrompt)?.let { promptUpdate = it }
        checkUpdates()
        while (true) {
            delay(TimeUnit.HOURS.toMillis(1))
            currentTime = System.currentTimeMillis()
        }
    }
    LaunchedEffect(openUpdates) {
        if (openUpdates) {
            selectedTab = StbPlayTab.SETTINGS
            updateInfo?.let { promptUpdate = it }
        }
    }

    fun portalKey(settings: PortalSettings) = "${settings.id}|${settings.url.trim()}|${settings.mac.trim()}"

    fun catalogSnapshot() = CatalogSnapshot(
        liveStreams = liveStreams,
        movieStreams = movieStreams,
        seriesStreams = seriesStreams,
        liveCategories = liveCategories,
        movieCategories = movieCategories,
        seriesCategories = seriesCategories,
        vodCatalogs = vodCatalogs.mapValues { (_, value) ->
            CachedVodCatalog(value.items, value.nextPage, value.totalItems, value.hasMore)
        }
    )

    fun startCategoryPreload(settings: PortalSettings) {
        if (searchVisible) return
        if (categoryPreloadJob?.isActive == true) return
        categoryPreloadJob = scope.launch {
            delay(400)
            val generation = catalogGeneration
            val categories = (movieCategories + seriesCategories)
                .distinctBy { it.id }
                .filterNot { it.isLocked }
            val pending = mutableMapOf<String, VodCatalogState>()
            val pendingStreams = mutableListOf<PortalStream>()
            suspend fun flush() {
                if (pending.isEmpty() || !isActive || generation != catalogGeneration) return
                vodCatalogs = vodCatalogs + pending.filterKeys { key -> vodCatalogs[key]?.items.isNullOrEmpty() }
                val movies = pendingStreams.filter { it.streamType == "movie" }
                val series = pendingStreams.filter { it.streamType == "series" }
                if (movies.isNotEmpty()) movieStreams = (movieStreams + movies).distinctBy { it.id }
                if (series.isNotEmpty()) seriesStreams = (seriesStreams + series).distinctBy { it.id }
                pending.clear()
                pendingStreams.clear()
                catalogCache.write(portalKey(settings), catalogSnapshot())
            }
            for (category in categories) {
                if (!isActive || generation != catalogGeneration) break
                val cached = vodCatalogs[category.id]
                if (!cached?.items.isNullOrEmpty() || cached?.hasMore == false) continue
                runCatching { portalRepository.getVodCatalogBatch(category.id, cached?.nextPage ?: 0, maxPages = 1) }
                    .onSuccess { batch ->
                        if (!isActive || generation != catalogGeneration) return@onSuccess
                        val previous = vodCatalogs[category.id] ?: VodCatalogState()
                        val merged = (previous.items + batch.items).distinctBy { "${it.streamType}:${it.id}" }
                        pending[category.id] = VodCatalogState(
                            items = merged,
                            nextPage = batch.nextPage,
                            totalItems = batch.totalItems ?: previous.totalItems,
                            hasMore = batch.hasMore
                        )
                        pendingStreams += batch.items
                    }
                if (pending.size >= 5) flush()
                delay(400)
            }
            flush()
        }
    }

    fun startConnection(input: PortalSettings) {
        if (connecting || input.url.isBlank() || input.mac.isBlank()) return
        categoryPreloadJob?.cancel()
        val refreshingVisibleCatalogue = screen == AppScreen.APP && autoConnectKey == portalKey(input) &&
            (liveStreams.isNotEmpty() || movieStreams.isNotEmpty() || seriesStreams.isNotEmpty())
        if (!refreshingVisibleCatalogue) scope.launch { contentGridState.scrollToItem(0) }
        connecting = true
        catalogGeneration++
        if (!refreshingVisibleCatalogue) {
            vodCatalogs = emptyMap()
            liveStreams = emptyList()
            focusedLiveChannelId = null
            movieStreams = emptyList()
            seriesStreams = emptyList()
            liveCategories = emptyList()
            movieCategories = emptyList()
            seriesCategories = emptyList()
            scope.launch { liveChannelListState.scrollToItem(0) }
            screen = AppScreen.LOADING
        }
        connectionError = null
        loadingStage = "Authenticating portal…"
        loadingProgress = 0.08f
        autoConnectKey = portalKey(input)

        scope.launch {
            try {
                if (!refreshingVisibleCatalogue) {
                    val cached = catalogCache.read(portalKey(input))
                    if (cached != null) {
                        liveStreams = cached.liveStreams
                        movieStreams = cached.movieStreams
                        seriesStreams = cached.seriesStreams
                        liveCategories = cached.liveCategories
                        movieCategories = cached.movieCategories
                        seriesCategories = cached.seriesCategories
                        vodCatalogs = cached.vodCatalogs.mapValues { (_, value) ->
                            VodCatalogState(value.items, value.nextPage, value.totalItems, value.hasMore)
                        }
                        screen = AppScreen.APP
                        loadingStage = "Updating catalogue in background…"
                    }
                }
                val login = portalRepository.initialize(input)
                if (!login.success) throw IllegalStateException(login.errorMessage ?: "Portal authentication failed.")
                subscription = portalRepository.getSubscription()

                loadingStage = "Loading live TV categories…"
                loadingProgress = 0.22f
                liveCategories = portalRepository.getLiveCategories()
                loadingStage = "Loading live channels…"
                loadingProgress = 0.36f
                liveStreams = portalRepository.getLiveStreams()

                loadingStage = "Loading Movies & Series categories…"
                loadingProgress = 0.52f
                movieCategories = portalRepository.getVodCategories()
                seriesCategories = portalRepository.getSeriesCategories()
                loadingStage = "Loading Movies & Series…"
                loadingProgress = 0.67f
                val firstBatch = portalRepository.getVodCatalogBatch(maxPages = 1)
                val categoryCatalogs = vodCatalogs.filterKeys { it != "all" }
                movieStreams = (movieStreams + firstBatch.items.filter { it.streamType == "movie" })
                    .distinctBy { it.id }
                seriesStreams = (seriesStreams + firstBatch.items.filter { it.streamType == "series" })
                    .distinctBy { it.id }
                vodCatalogs = categoryCatalogs + ("all" to VodCatalogState(
                    items = firstBatch.items,
                    nextPage = firstBatch.nextPage,
                    totalItems = firstBatch.totalItems,
                    hasMore = firstBatch.hasMore
                ))
                catalogCache.write(portalKey(input), catalogSnapshot())
                loadingProgress = 0.84f

                liveCategoryIndex = 0
                contentCategoryIndex = 0
                loadingStage = "Ready"
                loadingProgress = 1f
                settingsManager.markRefreshNow()
                screen = AppScreen.APP
                startCategoryPreload(input)
            } catch (error: Throwable) {
                connectionError = error.message ?: "Could not load this portal."
                if (refreshingVisibleCatalogue) screen = AppScreen.APP
            } finally {
                connecting = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(disclaimerAcknowledged, storedSettings.id, storedSettings.url, storedSettings.mac) {
        if (!disclaimerAcknowledged) return@LaunchedEffect
        if (storedSettings.url.isBlank() || storedSettings.mac.isBlank()) {
            if (!connecting) screen = AppScreen.SETUP
            return@LaunchedEffect
        }
        val key = portalKey(storedSettings)
        if (!connecting && autoConnectKey != key) startConnection(storedSettings)
    }

    fun allStreamFor(media: UiMedia): PortalStream? = (liveStreams + movieStreams + seriesStreams + remoteSearchStreams)
        .firstOrNull { it.id == media.id && it.streamType == media.streamType }
        ?: vodCatalogs.values.asSequence().flatMap { it.items.asSequence() }
            .firstOrNull { it.id == media.id && it.streamType == media.streamType }

    fun setFavorite(media: UiMedia) {
        if (media.isLocked) return
        scope.launch { settingsManager.setFavorite(media.id, media.id !in favoriteIds) }
    }

    fun launchPlayback(
        stream: PortalStream,
        command: String? = stream.cmd,
        seriesValue: String = stream.series.orEmpty(),
        episode: Boolean = false,
        resumeFraction: Float = 0f,
        contentId: String = stream.id
    ) {
        if (stream.streamType == "live") focusedLiveChannelId = stream.id
        playRequest = StalkerPlayRequest(
            command = command,
            kind = when {
                episode -> StalkerContentKind.SERIES_EPISODE
                stream.streamType == "live" -> StalkerContentKind.LIVE
                else -> StalkerContentKind.MOVIE
            },
            seriesValue = seriesValue,
            contentId = contentId,
            title = stream.name,
            resumeFraction = resumeFraction
        )
        selectedMovie = null
        selectedSeries = null
        qualityContext = null
    }

    fun openMedia(stream: PortalStream, resume: Float = 0f) {
        when (stream.streamType) {
            "live" -> launchPlayback(stream)
            "series" -> selectedSeries = stream
            else -> selectedMovie = stream
        }
    }

    fun startQualityChoice(context: QualityContext) {
        qualityContext = context
        qualityOptions = emptyList()
        qualityError = null
        qualityLoading = true
        scope.launch {
            runCatching {
                if (context.episode == null) portalRepository.getMovieQualityOptions(context.stream)
                else portalRepository.getEpisodeQualityOptions(context.stream, context.episode)
            }.onSuccess { options ->
                qualityOptions = options
                if (options.size == 1) {
                    val option = options.first()
                    launchPlayback(
                        stream = context.stream,
                        command = option.command,
                        seriesValue = option.seriesValue,
                        episode = context.episode != null,
                        resumeFraction = context.resumeFraction,
                        contentId = context.episode?.id ?: context.stream.id
                    )
                }
            }.onFailure { qualityError = it.message ?: "Could not read provider quality options." }
            qualityLoading = false
        }
    }

    val allVod = remember(movieStreams, seriesStreams) {
        (movieStreams + seriesStreams).distinctBy { "${it.streamType}:${it.id}" }
    }
    val explicitAdultVodCategoryIds = remember(movieCategories, seriesCategories) {
        (movieCategories + seriesCategories).filter { it.isAdultCategory() }.mapTo(HashSet()) { it.id }
    }
    val explicitAdultLiveCategoryIds = remember(liveCategories) {
        liveCategories.filter { it.isAdultCategory() }.mapTo(HashSet()) { it.id }
    }
    val flaggedAdultVodCategoryIds = remember(allVod) { allVod.filter { it.isAdultContent() }.mapNotNullTo(HashSet()) { it.categoryId } }
    val flaggedAdultLiveCategoryIds = remember(liveStreams) { liveStreams.filter { it.isAdultContent() }.mapNotNullTo(HashSet()) { it.categoryId } }
    val contentCategories = remember(movieCategories, seriesCategories, parentalMode, allVod) {
        val categories = (movieCategories + seriesCategories).distinctBy { it.id }
        val flaggedIds = allVod.filter { it.isAdultContent() }.mapNotNullTo(HashSet()) { it.categoryId }
        val visible = when (parentalMode) {
            ParentalMode.ALL_CONTENT -> categories
            ParentalMode.HIDE_ADULT -> categories.filterNot { it.isAdultCategory() }
            ParentalMode.ADULT_ONLY -> categories.filter { it.isAdultCategory() || it.id in flaggedIds }
        }.map { category ->
            UiCategory(category.id, category.name, category.isLocked && parentalMode == ParentalMode.ALL_CONTENT,
                category.isAdultCategory() || category.id in flaggedIds)
        }
        if (parentalMode == ParentalMode.ADULT_ONLY) listOf(UiCategory("adult-only", "Adult only", isAdult = true)) + visible
        else listOf(UiCategory("all", "All")) + visible
    }
    val uiLiveCategories = remember(liveCategories, liveStreams, parentalMode) {
        val flaggedIds = liveStreams.filter { it.isAdultContent() }.mapNotNullTo(HashSet()) { it.categoryId }
        val visible = when (parentalMode) {
            ParentalMode.ALL_CONTENT -> liveCategories
            ParentalMode.HIDE_ADULT -> liveCategories.filterNot { it.isAdultCategory() }
            ParentalMode.ADULT_ONLY -> liveCategories.filter { it.isAdultCategory() || it.id in flaggedIds }
        }.map { category ->
            UiCategory(category.id, category.name, category.isLocked && parentalMode == ParentalMode.ALL_CONTENT,
                category.isAdultCategory() || category.id in flaggedIds)
        }
        if (parentalMode == ParentalMode.ADULT_ONLY) listOf(UiCategory("adult-only", "Adult only", isAdult = true)) + visible
        else listOf(UiCategory("all", "All")) + visible
    }
    LaunchedEffect(parentalMode) {
        unlockedAdultCategoryKey = null
        liveCategoryIndex = 0
        contentCategoryIndex = 0
        focusedLiveChannelId = null
        scope.launch { liveChannelListState.scrollToItem(0); contentGridState.scrollToItem(0) }
    }
    fun requestMedia(media: UiMedia) {
        val stream = allStreamFor(media) ?: return
        categoryPreloadJob?.cancel()
        val categoryIndex = if (selectedTab == StbPlayTab.LIVE) liveCategoryIndex else contentCategoryIndex
        val category = when (selectedTab) {
            StbPlayTab.LIVE -> uiLiveCategories.getOrNull(categoryIndex)
            StbPlayTab.CONTENT -> contentCategories.getOrNull(categoryIndex)
            else -> null
        }
        val categoryUnlocked = category?.isAdult == true &&
            unlockedAdultCategoryKey == "${selectedTab.name}:$categoryIndex" &&
            stream.categoryId == category?.id &&
            ((selectedTab == StbPlayTab.LIVE && stream.streamType == "live") ||
                (selectedTab == StbPlayTab.CONTENT && stream.streamType != "live"))
        if (stream.isLocked && parentalMode != ParentalMode.ADULT_ONLY && !categoryUnlocked) pendingLockedMedia = stream else openMedia(stream, media.progress)
    }

    fun filterByCategory(items: List<PortalStream>, categories: List<UiCategory>, selectedIndex: Int): List<PortalStream> {
        val category = categories.getOrNull(selectedIndex) ?: categories.firstOrNull()
        val isLive = items.firstOrNull()?.streamType == "live"
        val explicitCategoryIds = if (isLive) explicitAdultLiveCategoryIds else explicitAdultVodCategoryIds
        val adultCategoryIds = if (isLive) explicitAdultLiveCategoryIds + flaggedAdultLiveCategoryIds
            else explicitAdultVodCategoryIds + flaggedAdultVodCategoryIds
        val eligible = when (parentalMode) {
            ParentalMode.ALL_CONTENT -> items
            ParentalMode.HIDE_ADULT -> items.filterNot { it.isAdultContent() || it.categoryId in adultCategoryIds }
            ParentalMode.ADULT_ONLY -> items.filter { stream ->
                stream.isAdultContent() || stream.categoryId in explicitCategoryIds
            }
        }
        return when {
            category == null || category.id == "all" -> if (parentalMode == ParentalMode.ALL_CONTENT)
                eligible.filterNot { it.isAdultContent() || it.categoryId in adultCategoryIds } else eligible
            category.id == "adult-only" -> eligible
            category.isAdult && parentalMode == ParentalMode.ADULT_ONLY -> eligible.filter {
                it.categoryId == category.id && (it.isAdultContent() || category.id in explicitCategoryIds)
            }
            else -> eligible.filter { it.categoryId == category.id }
        }
    }

    val filteredLive = remember(liveStreams, uiLiveCategories, liveCategoryIndex, parentalMode, explicitAdultVodCategoryIds, explicitAdultLiveCategoryIds, flaggedAdultVodCategoryIds, flaggedAdultLiveCategoryIds) {
        filterByCategory(liveStreams, uiLiveCategories, liveCategoryIndex)
    }
    fun stepLiveChannel(direction: Int) {
        val currentId = playRequest?.contentId
        val currentCategory = uiLiveCategories.getOrNull(liveCategoryIndex)
        val categoryUnlocked = parentalMode == ParentalMode.ADULT_ONLY ||
            (currentCategory?.isAdult == true && unlockedAdultCategoryKey == "LIVE:$liveCategoryIndex")
        val channels = (if (filteredLive.any { it.id == currentId }) filteredLive else filterByCategory(liveStreams, uiLiveCategories, liveCategoryIndex))
            .filter { !it.isLocked || categoryUnlocked }
        if (channels.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.id == currentId }
            val nextIndex = if (currentIndex < 0) 0 else (currentIndex + direction + channels.size) % channels.size
            launchPlayback(channels[nextIndex])
        }
    }
    val activity = LocalContext.current as? MainActivity
    DisposableEffect(activity, playRequest, filteredLive, liveStreams, unlockedAdultCategoryKey) {
        val handler: ((Int) -> Unit)? = if (playRequest?.kind == StalkerContentKind.LIVE) {
            { direction: Int -> stepLiveChannel(direction) }
        } else null
        activity?.channelStepHandler = handler
        onDispose { activity?.let { if (it.channelStepHandler === handler) it.channelStepHandler = null } }
    }
    val selectedContentCategory = contentCategories.getOrNull(contentCategoryIndex)
    val selectedVodKey = selectedContentCategory?.id ?: "all"
    val selectedVodCatalog = vodCatalogs[selectedVodKey]
    val categoryVod = if (selectedVodKey == "adult-only") allVod else selectedVodCatalog?.items.orEmpty()
    val filteredVod = remember(categoryVod, catalogueLanguage, parentalMode, explicitAdultVodCategoryIds, explicitAdultLiveCategoryIds, flaggedAdultVodCategoryIds, flaggedAdultLiveCategoryIds, contentCategories, contentCategoryIndex) {
        val parentalFiltered = filterByCategory(categoryVod, contentCategories, contentCategoryIndex)
        parentalFiltered.filter { stream ->
            val languageMatches = catalogueLanguage == "All" ||
                stream.language?.contains(catalogueLanguage, ignoreCase = true) == true ||
                stream.searchText?.contains(catalogueLanguage, ignoreCase = true) == true
            languageMatches
        }
    }

    fun toUi(stream: PortalStream): UiMedia = stream.toUiMedia(
        portrait = stream.streamType != "live",
        progress = progressById[stream.id] ?: 0f,
        favorite = stream.id in favoriteIds,
        imageUrl = portalRepository.resolveArtworkUrl(stream.iconUrl),
        imageHeaders = portalRepository.artworkRequestHeaders(stream.iconUrl)
    )

    // Preloading VOD changes catalogue state in the background. Keep the visible
    // screen's mapped rows stable so those updates do not rebuild every channel.
    val liveUiItems = remember(filteredLive, progressById, favoriteIds, catalogGeneration, selectedTab) {
        if (selectedTab == StbPlayTab.LIVE) filteredLive.map(::toUi) else emptyList()
    }
    val contentUiItems = remember(filteredVod, progressById, favoriteIds, catalogGeneration, selectedTab) {
        if (selectedTab == StbPlayTab.CONTENT) filteredVod.map(::toUi) else emptyList()
    }

    fun isAllowedInMode(stream: PortalStream, adultCategoryIds: Set<String>): Boolean = when (parentalMode) {
        ParentalMode.ALL_CONTENT -> !stream.isAdultContent() && stream.categoryId !in adultCategoryIds
        ParentalMode.HIDE_ADULT -> !stream.isAdultContent() && stream.categoryId !in adultCategoryIds
        ParentalMode.ADULT_ONLY -> stream.isAdultContent() || stream.categoryId in adultCategoryIds
    }
    val safeLive = remember(liveStreams, parentalMode, explicitAdultLiveCategoryIds) {
        liveStreams.filter { isAllowedInMode(it, explicitAdultLiveCategoryIds) }
    }
    val safeVod = remember(allVod, parentalMode, explicitAdultVodCategoryIds) {
        allVod.filter { isAllowedInMode(it, explicitAdultVodCategoryIds) }
    }
    val latestMovies = remember(safeVod) { safeVod.asSequence().filter { it.streamType == "movie" }.take(18).toList() }
    val latestSeries = remember(safeVod) { safeVod.asSequence().filter { it.streamType == "series" }.take(18).toList() }
    val favoriteStreams = remember(liveStreams, allVod, favoriteIds, parentalMode, explicitAdultLiveCategoryIds, explicitAdultVodCategoryIds) {
        (liveStreams + allVod).filter { stream ->
            stream.id in favoriteIds && isAllowedInMode(
                stream,
                if (stream.streamType == "live") explicitAdultLiveCategoryIds else explicitAdultVodCategoryIds
            )
        }.distinctBy { "${it.streamType}:${it.id}" }
    }
    val continueStreams = remember(safeVod, progressById) {
        safeVod.filter { (progressById[it.id] ?: 0f) > 0f }.sortedByDescending { progressById[it.id] ?: 0f }
    }
    val recommendationStreams = remember(safeVod, continueStreams, favoriteStreams) {
        val signalGenres = (continueStreams + favoriteStreams).mapNotNull { it.genre?.lowercase() }.toSet()
        val signalLanguages = (continueStreams + favoriteStreams).mapNotNull { it.language?.lowercase() }.toSet()
        val watchedIds = continueStreams.mapTo(HashSet()) { it.id }
        // Provider catalogues can contain 100k+ titles. Score a bounded, recent
        // candidate window instead of sorting the full catalogue on the UI thread.
        safeVod.asSequence()
            .take(1_200)
            .filter { it.id !in watchedIds }
            .sortedByDescending { stream ->
                (if (stream.genre?.lowercase() in signalGenres) 2 else 0) +
                    (if (stream.language?.lowercase() in signalLanguages) 1 else 0)
            }
            .take(18)
            .toList()
    }

    val homeHeroes = remember(safeVod, safeLive, progressById, favoriteIds, catalogGeneration) {
        (safeVod.asSequence() + safeLive.asSequence()).take(7).map(::toUi).toList()
    }
    val favouriteLive = remember(favoriteStreams) { favoriteStreams.filter { it.streamType == "live" } }

    val homeState = StbPlayHomeState(
        loading = screen == AppScreen.LOADING,
        heroes = homeHeroes,
        rows = buildList {
            if (continueStreams.isNotEmpty()) add(UiMediaRow("continue", "Continue Watching", "Saved on this device", continueStreams.take(16).map(::toUi)))
            if (recommendationStreams.isNotEmpty()) add(UiMediaRow("recommended", "Recommended for you", "Based on what you watch", recommendationStreams.map(::toUi)))
            if (favouriteLive.isNotEmpty()) add(UiMediaRow("favorites-live", "Favourite channels", items = favouriteLive.take(16).map(::toUi)))
            if (safeLive.isNotEmpty()) add(UiMediaRow("live", "Live TV", items = safeLive.take(16).map(::toUi)))
            if (latestMovies.isNotEmpty()) add(UiMediaRow("latest-movies", "Latest releases", "Movies", latestMovies.map(::toUi)))
            if (latestSeries.isNotEmpty()) add(UiMediaRow("latest-series", "Latest releases", "Series", latestSeries.map(::toUi)))
        },
        expiryText = subscription.toExpiryText()
    )
    val liveState = StbPlayLibraryState(
        loading = loadingLiveCategoryId == uiLiveCategories.getOrNull(liveCategoryIndex)?.id,
        categories = uiLiveCategories,
        selectedCategory = liveCategoryIndex.coerceIn(0, (uiLiveCategories.size - 1).coerceAtLeast(0)),
        items = liveUiItems,
        totalItemsText = "${filteredLive.size} channels",
        emptyMessage = liveCategoryError ?: "No channels in this category."
    )
    val contentState = StbPlayLibraryState(
        loading = selectedVodCatalog?.loading == true && categoryVod.isEmpty(),
        categories = contentCategories,
        selectedCategory = contentCategoryIndex.coerceIn(0, (contentCategories.size - 1).coerceAtLeast(0)),
        items = contentUiItems,
        totalItemsText = selectedVodCatalog?.totalItems?.let { "${categoryVod.size} of $it loaded" }
            ?: "${categoryVod.size} titles loaded",
        hasMore = selectedVodCatalog?.hasMore == true,
        loadingMore = selectedVodCatalog?.loading == true,
        emptyMessage = selectedVodCatalog?.error ?: "Try another category or load more titles."
    )
    val favouritesState = StbPlayLibraryState(items = favoriteStreams.map(::toUi))
    val settingsState = StbPlaySettingsState(
        profiles = profiles,
        activeProfileId = storedSettings.id,
        subscriptionPlan = subscription.plan,
        subscriptionStatus = subscription.status,
        expiryText = subscription.toExpiryText().orEmpty(),
        liveCount = liveStreams.size,
        movieCount = movieStreams.size,
        seriesCount = seriesStreams.size,
        playerPreference = playerPreference,
        themePreference = themePreference,
        parentalMode = parentalMode,
        subtitlePreference = subtitlePreference,
        catalogueLanguage = catalogueLanguage,
        analyticsEnabled = analyticsEnabled,
        lastRefreshText = lastRefreshAt.toDateTimeText(),
        updateText = updateText,
        updateAvailableVersion = updateInfo?.version
    )

    fun loadVodCategory(categoryId: String?) {
        val key = categoryId ?: "all"
        val previous = vodCatalogs[key] ?: VodCatalogState()
        if (previous.loading || !previous.hasMore) return
        val generation = catalogGeneration
        vodCatalogs = vodCatalogs + (key to previous.copy(loading = true, error = null))
        scope.launch {
            runCatching {
                portalRepository.getVodCatalogBatch(categoryId, previous.nextPage, if (previous.nextPage == 0) 1 else 3)
            }.onSuccess { batch ->
                if (generation != catalogGeneration) return@onSuccess
                val latest = vodCatalogs[key] ?: previous
                val merged = (latest.items + batch.items).distinctBy { "${it.streamType}:${it.id}" }
                vodCatalogs = vodCatalogs + (key to latest.copy(
                    items = merged, nextPage = batch.nextPage, totalItems = batch.totalItems ?: latest.totalItems,
                    hasMore = batch.hasMore, loading = false
                ))
                movieStreams = (movieStreams + batch.items.filter { it.streamType == "movie" }).distinctBy { it.id }
                seriesStreams = (seriesStreams + batch.items.filter { it.streamType == "series" }).distinctBy { it.id }
                scope.launch { catalogCache.write(portalKey(storedSettings), catalogSnapshot()) }
                scope.launch { delay(700); startCategoryPreload(storedSettings) }
            }.onFailure { error ->
                if (generation == catalogGeneration) {
                    vodCatalogs = vodCatalogs + (key to previous.copy(loading = false, error = error.message ?: "Could not load this category."))
                }
            }
        }
    }

    fun loadLiveCategory(category: UiCategory) {
        if (category.id == "all" || loadingLiveCategoryId == category.id ||
            liveStreams.any { it.categoryId == category.id }) return
        val generation = catalogGeneration
        loadingLiveCategoryId = category.id
        liveCategoryError = null
        scope.launch {
            runCatching { portalRepository.getLiveStreams(category.id) }
                .onSuccess { fetched ->
                    if (generation == catalogGeneration) {
                        liveStreams = (liveStreams + fetched).distinctBy { it.id }
                        scope.launch { catalogCache.write(portalKey(storedSettings), catalogSnapshot()) }
                    }
                }
                .onFailure { if (generation == catalogGeneration) liveCategoryError = it.message ?: "Could not load channels." }
            if (generation == catalogGeneration) loadingLiveCategoryId = null
        }
    }

    fun applyCategorySelection(tab: StbPlayTab, index: Int) {
        when (tab) {
            StbPlayTab.LIVE -> {
                if (liveCategoryIndex != index) {
                    focusedLiveChannelId = null
                    scope.launch { liveChannelListState.scrollToItem(0) }
                }
                liveCategoryIndex = index
                uiLiveCategories.getOrNull(index)?.let(::loadLiveCategory)
            }
            StbPlayTab.CONTENT -> {
                if (contentCategoryIndex != index) scope.launch { contentGridState.scrollToItem(0) }
                contentCategoryIndex = index
                contentCategories.getOrNull(index)?.id
                    ?.takeIf { it != "all" && it !in vodCatalogs }
                    ?.let(::loadVodCategory)
            }
            else -> Unit
        }
    }

    fun requestCategory(tab: StbPlayTab, index: Int) {
        val category = when (tab) {
            StbPlayTab.LIVE -> uiLiveCategories.getOrNull(index)
            StbPlayTab.CONTENT -> contentCategories.getOrNull(index)
            else -> null
        }
        val key = "${tab.name}:$index"
        if (category?.isAdult == true && parentalMode == ParentalMode.ALL_CONTENT && unlockedAdultCategoryKey != key) {
            pendingCategory = PendingCategory(tab, index, category.title)
        } else {
            if (category?.isLocked != true) unlockedAdultCategoryKey = null
            applyCategorySelection(tab, index)
        }
    }

    fun chooseQuality(option: PortalQualityOption) {
        val context = qualityContext ?: return
        launchPlayback(
            stream = context.stream,
            command = option.command,
            seriesValue = option.seriesValue,
            episode = context.episode != null,
            resumeFraction = context.resumeFraction,
            contentId = context.episode?.id ?: context.stream.id
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when {
                BuildConfig.BUILD_TYPE == "release" && updateInfo?.isOverdue(currentTime) == true && playRequest == null -> {
                    UpdateNotice(
                        info = updateInfo!!, required = true, status = updateText,
                        onUpdate = {
                            queueUpdateDownload(updateInfo!!)
                                .onSuccess { updateText = "Downloading update. Android will open the installer when ready." }
                                .onFailure { updateText = "Download could not start. Check your connection and try again." }
                        },
                        onLater = {}, onRetry = { checkUpdates(manual = true) }
                    )
                }
                !disclaimerAcknowledged -> FirstStartDisclaimer {
                    scope.launch { settingsManager.acknowledgeDisclaimer() }
                }
                playRequest != null -> {
                    val currentRequest = playRequest!!
                    fun playEpisodeAt(index: Int) {
                        val series = playingSeries ?: return
                        val episode = playingEpisodes.getOrNull(index) ?: return
                        playingEpisodeIndex = index
                        playRequest = null
                        startQualityChoice(QualityContext(episode.name, series, episode))
                    }
                    PlaybackRoute(
                    request = currentRequest,
                    portalUiUrl = storedSettings.url,
                    macAddress = storedSettings.mac,
                    token = portalRepository.getHandshakeToken(),
                    sessionCookie = portalRepository.getSessionCookie(),
                    playerPreference = playerPreference,
                    subtitlePreference = subtitlePreference,
                    onProgress = { position, duration ->
                        if (currentRequest.kind != StalkerContentKind.LIVE) scope.launch { settingsManager.saveProgress(currentRequest.contentId, position, duration) }
                    },
                    episodeTitles = if (currentRequest.kind == StalkerContentKind.SERIES_EPISODE) playingEpisodes.map { it.name } else emptyList(),
                    currentEpisodeIndex = playingEpisodeIndex,
                    onEpisodeSelected = ::playEpisodeAt,
                    onPlaybackEnded = {
                        if (currentRequest.kind == StalkerContentKind.SERIES_EPISODE && playingEpisodeIndex + 1 < playingEpisodes.size)
                            playEpisodeAt(playingEpisodeIndex + 1)
                    },
                    onChannelStep = ::stepLiveChannel,
                    onBack = {
                        playRequest = null
                        selectedSeries = playingSeries
                        playingSeries = null
                        playingEpisodes = emptyList()
                        playingEpisodeIndex = -1
                        startCategoryPreload(storedSettings)
                    }
                    )
                }
                qualityContext != null -> QualitySelectionScreen(
                    title = qualityContext!!.title,
                    options = qualityOptions,
                    isLoading = qualityLoading,
                    error = qualityError,
                    onOptionClick = ::chooseQuality,
                    onBack = { qualityContext = null; qualityOptions = emptyList() }
                )
                selectedMovie != null -> {
                    val movie = selectedMovie!!
                    val media = toUi(movie)
                    MovieDetailsScreen(
                        item = media,
                        onPlay = { startQualityChoice(QualityContext(movie.name, movie)) },
                        onResume = { startQualityChoice(QualityContext(movie.name, movie, resumeFraction = media.progress)) },
                        onToggleFavorite = { setFavorite(media) },
                        onBack = { selectedMovie = null; startCategoryPreload(storedSettings) }
                    )
                }
                selectedSeries != null -> {
                    val series = selectedSeries!!
                    SeriesDetailsScreen(
                        series = series,
                        repository = portalRepository,
                        isFavorite = series.id in favoriteIds,
                        onToggleFavorite = { setFavorite(toUi(series)) },
                        onEpisodeClick = { episode, episodes ->
                            playingSeries = series
                            playingEpisodes = episodes
                            playingEpisodeIndex = episodes.indexOfFirst { it.id == episode.id }
                            startQualityChoice(QualityContext(episode.name, series, episode))
                        },
                        onBack = { selectedSeries = null; startCategoryPreload(storedSettings) }
                    )
                }
                screen == AppScreen.SETUP -> SetupScreen(
                    initialSettings = editingPortal ?: storedSettings,
                    onSave = { saved ->
                        val prepared = saved.withStableId()
                        scope.launch { settingsManager.upsertPortal(prepared) }
                        editingPortal = null
                        startConnection(prepared)
                    },
                    onCancel = if (profiles.isNotEmpty()) ({ editingPortal = null; screen = AppScreen.APP }) else null
                )
                screen == AppScreen.LOADING -> LoadingScreen(
                    portalName = storedSettings.name,
                    stage = loadingStage,
                    progress = loadingProgress,
                    error = connectionError,
                    onRetry = { startConnection(storedSettings) },
                    onEdit = { editingPortal = storedSettings; screen = AppScreen.SETUP }
                )
                else -> StbPlayApp(
                    homeState = homeState,
                    liveState = liveState,
                    contentState = contentState,
                    favouritesState = favouritesState,
                    settingsState = settingsState,
                    selectedTab = selectedTab,
                    liveChannelListState = liveChannelListState,
                    focusedLiveChannelId = focusedLiveChannelId,
                    contentGridState = contentGridState,
                    onTabSelected = { tab ->
                        if (tab != selectedTab) {
                            if (selectedTab == StbPlayTab.LIVE && uiLiveCategories.getOrNull(liveCategoryIndex)?.isAdult == true) {
                                liveCategoryIndex = 0
                                focusedLiveChannelId = null
                                scope.launch { liveChannelListState.scrollToItem(0) }
                            }
                            if (selectedTab == StbPlayTab.CONTENT && contentCategories.getOrNull(contentCategoryIndex)?.isAdult == true) {
                                contentCategoryIndex = 0
                                scope.launch { contentGridState.scrollToItem(0) }
                            }
                            unlockedAdultCategoryKey = null
                        }
                        selectedTab = tab
                    },
                    onLoadMoreContent = { loadVodCategory(selectedContentCategory?.id?.takeUnless { it == "all" }) },
                    onCategorySelected = ::requestCategory,
                    onMediaClick = ::requestMedia,
                    onToggleFavorite = ::setFavorite,
                    onRemoveHistory = { media -> scope.launch { settingsManager.removeFromHistory(media.id) } },
                    onRefresh = { startConnection(storedSettings) },
                    onClearCache = {
                        categoryPreloadJob?.cancel()
                        liveStreams = emptyList(); movieStreams = emptyList(); seriesStreams = emptyList()
                        vodCatalogs = emptyMap()
                        scope.launch {
                            catalogCache.clear(portalKey(storedSettings))
                            startConnection(storedSettings)
                        }
                    },
                    onClearHistory = { scope.launch { settingsManager.clearWatchHistory() } },
                    onAddPortal = { editingPortal = PortalSettings(pin = storedSettings.pin); screen = AppScreen.SETUP },
                    onEditPortal = { profile -> editingPortal = profile.copy(pin = storedSettings.pin); screen = AppScreen.SETUP },
                    onUsePortal = { profile ->
                        scope.launch { settingsManager.activatePortal(profile.id) }
                        autoConnectKey = null
                        screen = AppScreen.LOADING
                        loadingStage = "Switching portal…"
                        loadingProgress = 0.04f
                    },
                    onDeletePortal = { profile -> scope.launch { settingsManager.deletePortal(profile.id) } },
                    onPlayerPreferenceChanged = { preference -> scope.launch { settingsManager.setPlayerPreference(preference) } },
                    onThemePreferenceChanged = { preference -> scope.launch { settingsManager.setThemePreference(preference) } },
                    onSubtitlePreferenceChanged = { preference -> scope.launch { settingsManager.setSubtitlePreference(preference) } },
                    onCatalogueLanguageChanged = { language -> scope.launch { settingsManager.setCatalogueLanguage(language) } },
                    onAnalyticsChanged = { enabled -> scope.launch { settingsManager.setAnalyticsEnabled(enabled) } },
                    onChangePin = { changingPin = true },
                    onParentalModeChanged = { mode ->
                        if (mode != parentalMode) {
                            if (storedSettings.pin.isBlank()) scope.launch { settingsManager.setParentalMode(mode) }
                            else pendingParentalMode = mode
                        }
                    },
                    onCheckUpdates = { checkUpdates(manual = true) },
                    onDownloadUpdate = {
                        val info = updateInfo
                        if (info == null) {
                            updateText = "No Android TV update is available."
                        } else {
                            queueUpdateDownload(info)
                                .onSuccess { updateText = "Downloading ${info.version}. Android will open the installer when it finishes." }
                                .onFailure { updateText = "Could not start the update download." }
                        }
                    },
                    onShare = onShare,
                    onSearchVisibilityChanged = { visible ->
                        searchVisible = visible
                        if (visible) categoryPreloadJob?.cancel()
                        else scope.launch {
                            delay(1_000)
                            if (playRequest == null && selectedMovie == null && selectedSeries == null) startCategoryPreload(storedSettings)
                        }
                    },
                    searchCatalog = when (selectedTab) {
                        StbPlayTab.LIVE -> liveStreams.filter { isAllowedInMode(it, explicitAdultLiveCategoryIds) }
                        StbPlayTab.CONTENT -> {
                            // Search everything already loaded from every VOD page/category,
                            // not just the currently selected category's visible page.
                            allVod.filter { stream -> isAllowedInMode(stream, explicitAdultVodCategoryIds) }.filter { stream ->
                                catalogueLanguage == "All" ||
                                    stream.language?.contains(catalogueLanguage, ignoreCase = true) == true ||
                                    stream.searchText?.contains(catalogueLanguage, ignoreCase = true) == true
                            }
                        }
                        StbPlayTab.FAVOURITES -> favoriteStreams
                        else -> safeLive + safeVod
                    },
                    searchRemote = { query, page ->
                            portalRepository.searchVod(query, page).let { batch ->
                            batch.copy(items = batch.items.filter { stream -> isAllowedInMode(stream, explicitAdultVodCategoryIds) }.filter { stream ->
                                catalogueLanguage == "All" ||
                                    stream.language?.contains(catalogueLanguage, ignoreCase = true) == true ||
                                    stream.searchText?.contains(catalogueLanguage, ignoreCase = true) == true
                            })
                        }
                    },
                    onSearchResults = { remoteSearchStreams = it },
                    searchMedia = ::toUi
                )
            }

            pendingLockedMedia?.let { locked ->
                PinPrompt(
                    title = locked.name,
                    expectedPin = storedSettings.pin,
                    onVerified = { pendingLockedMedia = null; openMedia(locked) },
                    onCancel = { pendingLockedMedia = null }
                )
            }
            pendingCategory?.let { pending ->
                PinPrompt(
                    title = pending.title,
                    expectedPin = storedSettings.pin,
                    onVerified = {
                        unlockedAdultCategoryKey = "${pending.tab.name}:${pending.index}"
                        applyCategorySelection(pending.tab, pending.index)
                        pendingCategory = null
                    },
                    onCancel = { pendingCategory = null }
                )
            }
            pendingParentalMode?.let { requestedMode ->
                PinPrompt(
                    title = "Change parental mode",
                    expectedPin = storedSettings.pin,
                    onVerified = {
                        scope.launch { settingsManager.setParentalMode(requestedMode) }
                        pendingParentalMode = null
                    },
                    onCancel = { pendingParentalMode = null }
                )
            }
            if (changingPin) {
                ChangePinPrompt(
                    expectedPin = storedSettings.pin,
                    onSave = { newPin ->
                        scope.launch { settingsManager.updateParentalPin(storedSettings.pin, newPin) }
                        changingPin = false
                    },
                    onCancel = { changingPin = false }
                )
            }
            promptUpdate?.takeIf { !it.isOverdue(currentTime) || BuildConfig.BUILD_TYPE != "release" }?.let { available ->
                if (playRequest == null && screen == AppScreen.APP) UpdateNotice(
                    info = available, required = false, status = updateText,
                    onUpdate = {
                        updateManager.markPrompted(available)
                        promptUpdate = null
                        queueUpdateDownload(available)
                            .onSuccess { updateText = "Downloading ${available.version}. Android will open the installer when ready." }
                            .onFailure { updateText = "Could not start the update download." }
                    },
                    onLater = { updateManager.markPrompted(available); promptUpdate = null },
                    onRetry = { checkUpdates(manual = true) }
                )
            }
        }
    }
}

private fun PortalStream.toUiMedia(
    portrait: Boolean,
    progress: Float,
    favorite: Boolean,
    imageUrl: String?,
    imageHeaders: Map<String, String>
): UiMedia {
    val badge = when (streamType) {
        "live" -> number?.let { "CH $it" } ?: "LIVE"
        "series" -> "SERIES"
        else -> year?.toString() ?: "MOVIE"
    }
    return UiMedia(
        id = id,
        title = name,
        imageUrl = imageUrl,
        imageHeaders = imageHeaders,
        subtitle = originalTitle,
        description = description,
        badge = badge,
        progress = progress,
        portrait = portrait,
        streamType = streamType,
        year = year,
        searchText = searchText,
        language = language,
        genre = genre,
        rating = rating,
        cast = cast,
        isLocked = isLocked,
        isFavorite = favorite
    )
}

private fun PortalSubscription.toExpiryText(): String? {
    if (unlimited) return "Unlimited subscription"
    val expiry = expiryEpochMillis ?: return null
    val diff = expiry - System.currentTimeMillis()
    if (diff <= 0L) return "Subscription expired"
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        days == 0L -> "Subscription expires today"
        days == 1L -> "Subscription expires tomorrow"
        days < 30L -> "Subscription expires in $days days"
        else -> "Subscription expires ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(expiry))}"
    }
}

private fun Long.toDateTimeText(): String = if (this <= 0L) "Not refreshed yet"
else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))
