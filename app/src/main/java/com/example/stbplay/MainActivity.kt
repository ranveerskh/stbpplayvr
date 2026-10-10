package com.example.stbplay

import android.app.DownloadManager
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.media3.cast.Cast
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn as AndroidXOptIn
import com.example.stbplay.ui.SearchSession
import com.example.stbplay.data.CategoryDropdownPosition
import com.example.stbplay.data.PlayerPreference
import com.example.stbplay.data.ParentalMode
import com.example.stbplay.data.PortalRepository
import com.example.stbplay.data.CatalogCacheStore
import com.example.stbplay.data.CatalogSnapshot
import com.example.stbplay.data.CachedVodCatalog
import com.example.stbplay.data.SettingsManager
import com.example.stbplay.data.SettingsBootstrap
import com.example.stbplay.data.LauncherIconManager
import com.example.stbplay.data.StalkerContentKind
import com.example.stbplay.data.StalkerPlayRequest
import com.example.stbplay.data.ThemePreference
import com.example.stbplay.data.UpdateInfo
import com.example.stbplay.data.UpdateCheckWorker
import com.example.stbplay.data.UpdateManager
import com.example.stbplay.data.PlatformLicenseClient
import com.example.stbplay.data.appAccessInfo
import com.example.stbplay.data.ProviderPairingSession
import com.example.stbplay.data.PortalExpiryReminderWorker
import com.example.stbplay.data.model.PortalCategory
import com.example.stbplay.data.model.PortalEpisode
import com.example.stbplay.ui.screens.EpisodeResumePrompt
import com.example.stbplay.data.model.PortalQualityOption
import com.example.stbplay.data.model.PortalStream
import com.example.stbplay.data.model.PortalSubscription
import com.example.stbplay.domain.model.PortalSettings
import com.example.stbplay.ui.ChangePinPrompt
import com.example.stbplay.ui.FirstStartDisclaimer
import com.example.stbplay.ui.PinPrompt
import com.example.stbplay.ui.ProviderPinSetupPrompt
import com.example.stbplay.data.resolveProviderPortalId
import com.example.stbplay.data.resolvePairingPortal
import com.example.stbplay.data.findCatalogStream
import com.example.stbplay.ui.screens.TvMediaDetailsScreen
import com.example.stbplay.ui.screens.TvLivePreviewPanel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.example.stbplay.ui.ProviderPairingDialog
import com.example.stbplay.ui.StbPlayApp
import com.example.stbplay.ui.StbPlayHomeState
import com.example.stbplay.ui.StbPlayLibraryState
import com.example.stbplay.ui.StbPlaySettingsState
import com.example.stbplay.ui.StbPlayTab
import com.example.stbplay.ui.UiCategory
import com.example.stbplay.ui.UiMedia
import com.example.stbplay.ui.UiMediaRow
import com.example.stbplay.ui.rememberFavouriteUiItems
import com.example.stbplay.ui.rememberSearchCatalog
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
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
    private var observedPackageUpdateTime = 0L
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
        observedPackageUpdateTime = installedPackageUpdateTime()
        openUpdates = intent?.getBooleanExtra("stb_open_updates", false) == true
        if (!isAndroidTvDevice()) {
            runCatching { Cast.getSingletonInstance(this).initialize() }
        }
        settingsManager = SettingsManager(this)
        updateManager = UpdateManager(applicationContext)
        // Scheduling background work is optional; never let a scheduler failure
        // prevent the player UI from starting on a device.
        runCatching { UpdateCheckWorker.schedule(applicationContext) }
        runCatching { PortalExpiryReminderWorker.schedule(applicationContext) }
        registerUpdateReceiver()

        setContent {
            val themePreference by settingsManager.themePreference.collectAsState(initial = ThemePreference.BLUE)
            val savedParentalMode by settingsManager.parentalMode.collectAsState(initial = ParentalMode.ALL_CONTENT)
            val activeViewer by settingsManager.activeViewer.collectAsState(initial = com.example.stbplay.data.ViewerProfile("owner", "Owner", 18))
            val parentalMode = if (activeViewer.isKids) ParentalMode.HIDE_ADULT else savedParentalMode
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
        // Some Android installers return to the old task without recreating it.
        // Recreate only when this package was actually replaced, so Compose and
        // the portal session are rebuilt automatically after an in-app update.
        val currentPackageUpdateTime = installedPackageUpdateTime()
        if (observedPackageUpdateTime > 0L && currentPackageUpdateTime > observedPackageUpdateTime) {
            observedPackageUpdateTime = currentPackageUpdateTime
            recreate()
            return
        }
        if (currentPackageUpdateTime > 0L) observedPackageUpdateTime = currentPackageUpdateTime
        if (::updateManager.isInitialized) {
            val pending = updateManager.pendingDownloadId()
            if (pending > 0) runCatching { updateManager.openInstaller(pending) }
        }
    }

    private fun installedPackageUpdateTime(): Long = runCatching {
        packageManager.getPackageInfo(packageName, 0).lastUpdateTime
    }.getOrDefault(0L)

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
    val isAndroidTv = remember(appContext) {
        appContext.isAndroidTvDevice()
    }
    val platformLicenseClient = remember(appContext) { PlatformLicenseClient(appContext) }
    val demoTrialStartedAt = remember(platformLicenseClient) { platformLicenseClient.demoTrialStartedAtMillis() }
    val subscriptionCache = remember(appContext) { appContext.getSharedPreferences("portal_subscription_cache", android.content.Context.MODE_PRIVATE) }
    var platformLicense by remember { mutableStateOf(platformLicenseClient.currentLicense()) }
    var providerPairing by remember { mutableStateOf<ProviderPairingSession?>(null) }
    var providerPinSetup by remember { mutableStateOf<PortalSettings?>(null) }
    var providerPairingStatus by remember { mutableStateOf("Waiting for your provider to assign a portal.") }
    var providerPairingBusy by remember { mutableStateOf(false) }
    val catalogCache = remember(appContext) { CatalogCacheStore(appContext) }
    val contentGridState = rememberLazyGridState()
    val appUiStateHolder = rememberSaveableStateHolder()
    val liveChannelListState = rememberLazyListState()
    val startupSettings by settingsManager.startupSettings
        .map { it as SettingsBootstrap? }
        .collectAsState(initial = null)
    val settingsLoaded = startupSettings != null
    val storedSettings = startupSettings?.activePortal ?: PortalSettings()
    val profiles = startupSettings?.profiles.orEmpty()
    val viewerProfiles by settingsManager.viewerProfiles.collectAsState(initial = emptyList())
    val phoneSetupComplete by settingsManager.phoneSetupComplete.collectAsState(initial = true)
    var unlockedViewerId by remember { mutableStateOf<String?>(null) }
    val phoneDevice = !isAndroidTv && com.example.stbplay.ui.useStaticUiScale(false, android.os.Build.MANUFACTURER, android.os.Build.BRAND, android.os.Build.MODEL)

    val favoriteIds by settingsManager.favoriteIds.collectAsState(initial = emptySet())
    val progressById by settingsManager.vodProgress.collectAsState(initial = emptyMap())
    val playerPreference by settingsManager.playerPreference.collectAsState(initial = PlayerPreference.AUTO)
    val androidBoxVideoCompatibility by settingsManager.androidBoxVideoCompatibility.collectAsState(initial = false)
    val subtitlePreference by settingsManager.subtitlePreference.collectAsState(initial = com.example.stbplay.data.SubtitlePreference.AUTO)
    val themePreference by settingsManager.themePreference.collectAsState(initial = ThemePreference.BLUE)
    val phoneCategoryPosition by settingsManager.phoneCategoryPosition.collectAsState(initial = CategoryDropdownPosition.TOP)
    val phoneMovieColumns by settingsManager.phoneMovieColumns.collectAsState(initial = 2)
    val searchHistory by settingsManager.searchHistory.collectAsState(initial = emptyList())
    val savedParentalMode by settingsManager.parentalMode.collectAsState(initial = ParentalMode.ALL_CONTENT)
    val activeViewer by settingsManager.activeViewer.collectAsState(initial = com.example.stbplay.data.ViewerProfile("owner", "Owner", 18))
    val parentalMode = if (activeViewer.isKids) ParentalMode.HIDE_ADULT else savedParentalMode
    val catalogueLanguage by settingsManager.catalogueLanguage.collectAsState(initial = "All")
    val searchSession = remember(storedSettings.id, storedSettings.url, storedSettings.mac, activeViewer, parentalMode, catalogueLanguage) { SearchSession() }
    val analyticsEnabled by settingsManager.analyticsEnabled.collectAsState(initial = false)
    val privacyPrefs = remember(appContext) { appContext.getSharedPreferences("privacy_notice", Context.MODE_PRIVATE) }
    var acceptedPolicyVersion by remember(privacyPrefs) {
        mutableStateOf(privacyPrefs.getInt("policy_accepted_version", 0))
    }
    val disclaimerAcknowledged = startupSettings?.disclaimerAcknowledged ?: false
    val policyAccepted = disclaimerAcknowledged && acceptedPolicyVersion >= REQUIRED_POLICY_VERSION
    val lastRefreshAt by settingsManager.lastRefreshAt.collectAsState(initial = 0L)

    val portalRepository = remember { PortalRepository() }
    // Hold a neutral surface until DataStore has returned the saved setup state.
    // Once it is known, returning users start on Home while the portal reconnects.
    var screen by remember { mutableStateOf(AppScreen.APP) }
    var loadingStage by remember { mutableStateOf("Preparing portal…") }
    var loadingProgress by remember { mutableStateOf(0f) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    var autoConnectKey by remember { mutableStateOf<String?>(null) }
    var portalReady by remember { mutableStateOf(false) }
    var pendingPlaybackMedia by remember { mutableStateOf<UiMedia?>(null) }
    var playbackSessionGeneration by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var recoveryAttemptedContentIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var liveStreams by remember { mutableStateOf<List<PortalStream>>(emptyList()) }
    var movieStreams by remember { mutableStateOf<List<PortalStream>>(emptyList()) }
    var seriesStreams by remember { mutableStateOf<List<PortalStream>>(emptyList()) }
    val viewerCatalogProfileId = if (activeViewer.id == "owner") storedSettings.id else "${storedSettings.id}:view:${activeViewer.id}"
    var localFavoriteStreams by remember(viewerCatalogProfileId) { mutableStateOf<List<PortalStream>>(emptyList()) }

    LaunchedEffect(viewerCatalogProfileId) {
        localFavoriteStreams = catalogCache.readFavorites(viewerCatalogProfileId)
    }
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
    var subscriptionMessage by remember { mutableStateOf("") }
    var licenseStatus by remember { mutableStateOf("No key activated") }
    var licenseBusy by remember { mutableStateOf(false) }

    LaunchedEffect(policyAccepted, analyticsEnabled) {
        if (policyAccepted && analyticsEnabled) platformLicenseClient.usageHeartbeat()
    }

    var selectedTab by remember { mutableStateOf(StbPlayTab.HOME) }
    // Event-only state: recording remote input must not recompose the home.
    val lastHomeInteractionMs = remember { longArrayOf(0L) }
    var liveCategoryIndex by remember { mutableStateOf(0) }
    var contentCategoryIndex by remember { mutableStateOf(0) }
    var focusedLiveChannelId by remember { mutableStateOf<String?>(null) }
    var focusedContentId by remember { mutableStateOf<String?>(null) }
    var selectedMovie by remember { mutableStateOf<PortalStream?>(null) }
    var selectedLiveChannel by remember { mutableStateOf<PortalStream?>(null) }
    var selectedSeries by remember { mutableStateOf<PortalStream?>(null) }
    var qualityContext by remember { mutableStateOf<QualityContext?>(null) }
    var qualityOptions by remember { mutableStateOf<List<PortalQualityOption>>(emptyList()) }
    var qualityLoading by remember { mutableStateOf(false) }
    var qualityError by remember { mutableStateOf<String?>(null) }
    var playRequest by remember { mutableStateOf<StalkerPlayRequest?>(null) }
    var livePreviewStream by remember(storedSettings.id, storedSettings.url) { mutableStateOf<PortalStream?>(null) }
    var livePreviewFullscreen by remember { mutableStateOf(false) }
    var tvLivePreviewPlayback by remember { mutableStateOf(false) }
    var livePreviewBounds by remember { mutableStateOf(Rect.Zero) }
    var livePreviewVlcAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val livePreviewReturnFocus = remember { arrayOf<() -> Unit>({}) }
    val density = LocalDensity.current
    val livePreviewActionsRequester = remember { FocusRequester() }
    var playingSeries by remember { mutableStateOf<PortalStream?>(null) }
    var playingEpisodes by remember { mutableStateOf<List<PortalEpisode>>(emptyList()) }
    var playingEpisodeIndex by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
    var pendingEpisodePlayback by remember { mutableStateOf<QualityContext?>(null) }
    var pendingEpisodeList by remember { mutableStateOf<List<PortalEpisode>>(emptyList()) }
    var progressResetGeneration by remember { androidx.compose.runtime.mutableIntStateOf(0) }
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
    val kidsCategoryIds = remember(activeViewer, storedSettings, liveCategories, movieCategories, seriesCategories) {
        val allowedLive = liveCategories.filter { activeViewer.approvalPortalKey == portalKey(storedSettings) && !it.isAdultCategory() && it.id in activeViewer.allowedLiveCategories }.mapTo(HashSet()) { it.id }
        val allowedVod = (movieCategories + seriesCategories).filter { activeViewer.approvalPortalKey == portalKey(storedSettings) && !it.isAdultCategory() && it.id in activeViewer.allowedVodCategories }.mapTo(HashSet()) { it.id }
        allowedLive to allowedVod
    }
    fun viewerAllows(stream: PortalStream) = !activeViewer.isKids || com.example.stbplay.data.isKidsContentAllowed(stream, if (stream.streamType == "live") kidsCategoryIds.first else kidsCategoryIds.second)


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
        if (searchVisible || searchSession.open) return
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
                // Catalogue merges scan provider lists. Publish them after TV
                // navigation has been quiet instead of competing with each key.
                while (isAndroidTv && selectedTab == StbPlayTab.HOME && isActive) {
                    val remaining = 750L - (SystemClock.elapsedRealtime() - lastHomeInteractionMs[0])
                    if (remaining <= 0L) break
                    delay(remaining)
                }
                if (!isActive || generation != catalogGeneration) return
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

    fun startConnection(input: PortalSettings, keepHomeVisible: Boolean = false) {
        if (connecting || input.url.isBlank() || input.mac.isBlank()) return
        categoryPreloadJob?.cancel()
        val refreshingVisibleCatalogue = screen == AppScreen.APP && autoConnectKey == portalKey(input) &&
            (liveStreams.isNotEmpty() || movieStreams.isNotEmpty() || seriesStreams.isNotEmpty())
        if (!refreshingVisibleCatalogue) scope.launch { contentGridState.scrollToItem(0) }
        connecting = true
        portalReady = false
        catalogGeneration++
        if (!refreshingVisibleCatalogue && !keepHomeVisible) {
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
        } else if (keepHomeVisible) {
            screen = AppScreen.APP
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
                val login = com.example.stbplay.data.retryPortalLogin {
                    portalRepository.initialize(input)
                }
                if (!login.success) throw IllegalStateException(login.errorMessage ?: "Portal authentication failed.")
                portalReady = true
                subscription = portalRepository.getSubscription()
                PortalExpiryReminderWorker.setExpiry(appContext, subscription.expiryEpochMillis.takeUnless { subscription.unlimited })

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
                if (error is kotlinx.coroutines.CancellationException) throw error
                val reason = error.message?.takeIf { it.isNotBlank() } ?: "Connection failed."
                connectionError = "Could not connect to this portal ($reason). The portal may be unavailable, its details may have changed, or the provider service may have expired."
                if (refreshingVisibleCatalogue || keepHomeVisible) screen = AppScreen.APP
            } finally {
                connecting = false
            }
        }
    }

    fun startProviderPairing(target: PortalSettings) {
        if (providerPairingBusy) return
        val prepared = target.normalized().withStableId()
        providerPairingBusy = true
        scope.launch {
            try {
                // A new pairing draft has no URL until the provider assigns it.
                // Do not activate or save an incomplete profile as a normal portal.
                if (prepared.url.isNotBlank()) settingsManager.upsertPortal(prepared)
                providerPairingStatus = "Creating a secure, one-time pairing code…"
                val session = platformLicenseClient.startProviderPairing(prepared.mac, prepared.id)
                providerPairing = session
                providerPairingStatus = "Waiting for your provider to assign a portal."
            } catch (error: Exception) {
                android.widget.Toast.makeText(
                    appContext,
                    error.message ?: "Could not start provider pairing. Try again.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } finally {
                providerPairingBusy = false
            }
        }
    }

    suspend fun syncAssignedPortal(input: PortalSettings): PortalSettings {
        val boundId = platformLicenseClient.providerPortalId()
        if (boundId != null && boundId != input.id) return input
        val assignment = runCatching { platformLicenseClient.syncProviderAssignment() }.getOrNull() ?: return input
        val latestSettings = settingsManager.startupSettings.first()
        val draft = boundId?.let { settingsManager.providerPairingDraft(it) }
        val targetId = resolveProviderPortalId(boundId, assignment.portalUrl, latestSettings.profiles)
            ?: draft?.id ?: return input
        if (boundId == null) platformLicenseClient.bindProviderPortal(targetId)
        if (targetId != input.id) return input
        val latestProfile = latestSettings.profiles.firstOrNull { it.id == targetId } ?: draft ?: return input
        platformLicense = platformLicenseClient.currentLicense()
        licenseStatus = "Provider portal synced."
        val updated = latestProfile.copy(name = assignment.portalName, url = assignment.portalUrl,
            pin = latestSettings.activePortal.pin).normalized()
        val recoveringFirstPortal = draft != null && latestSettings.profiles.isEmpty()
        if (updated.name != latestProfile.name || updated.url != latestProfile.url)
            settingsManager.upsertPortal(updated, makeActive = recoveringFirstPortal)
        if (updated.pin.isBlank()) {
            if (latestSettings.activePortal.id == updated.id || recoveringFirstPortal) providerPinSetup = updated
            return input
        }
        return updated
    }

    suspend fun refreshProviderPortal(input: PortalSettings) {
        val latest = syncAssignedPortal(input)
        if (latest.url == input.url || providerPinSetup != null) return
        while (connecting) delay(200)
        val active = settingsManager.portalSettings.first()
        if (active.id == latest.id && active.url == latest.url && playRequest == null && autoConnectKey != portalKey(latest))
            startConnection(latest, keepHomeVisible = true)
    }

    fun refreshPortal(input: PortalSettings, keepHomeVisible: Boolean = false) {
        startConnection(input, keepHomeVisible)
        scope.launch { refreshProviderPortal(input) }
    }

    LaunchedEffect(settingsLoaded, disclaimerAcknowledged) {
        if (!settingsLoaded || !disclaimerAcknowledged || platformLicenseClient.pendingProviderPairing() != null) return@LaunchedEffect
        val bootstrap = settingsManager.startupSettings.first()
        val boundId = platformLicenseClient.providerPortalId()
        val target = bootstrap.profiles.firstOrNull { it.id == boundId }
            ?: boundId?.let { settingsManager.providerPairingDraft(it) } ?: bootstrap.activePortal
        refreshProviderPortal(target)
    }

    LaunchedEffect(settingsLoaded) {
        if (settingsLoaded) {
            platformLicense = platformLicenseClient.currentLicense()
            platformLicenseClient.pendingProviderPairing()?.let {
                providerPairing = it
                providerPairingStatus = if (it.expiresAtMillis <= System.currentTimeMillis())
                    "This pairing code expired. Close it and request a new code."
                else "Waiting for your provider to assign a portal."
            }
        }
    }

    LaunchedEffect(providerPairing?.pairingCode) {
        val session = providerPairing ?: return@LaunchedEffect
        while (isActive) {
            if (session.expiresAtMillis <= System.currentTimeMillis()) {
                providerPairingStatus = "This pairing code expired. Close it and request a new code."
                break
            }
            delay(3_000)
            val status = runCatching { platformLicenseClient.checkProviderPairing(session) }
            val assignment = status.getOrNull()
            if (assignment == null) {
                val message = status.exceptionOrNull()?.message.orEmpty()
                if (message.contains("expir", ignoreCase = true)) {
                    providerPairingStatus = "This pairing code expired. Close it and request a new code."
                    break
                }
                providerPairingStatus = "Still waiting for your provider. Connection will retry automatically."
                continue
            }
            if (assignment.pending) {
                providerPairingStatus = "Waiting for your provider to select a portal profile…"
                continue
            }
            val bootstrap = settingsManager.startupSettings.first()
            val current = resolvePairingPortal(
                session.portalId, session.portalMac, bootstrap.profiles, bootstrap.activePortal
            )
            val assignedPortal = current.copy(
                name = assignment.portalName,
                url = assignment.portalUrl,
                mac = session.portalMac,
                pin = bootstrap.activePortal.pin
            ).normalized().withStableId()
            // Persist the assigned URL before clearing the recoverable pairing session
            // or opening PIN setup. Restarting during PIN setup must keep the portal.
            settingsManager.upsertPortal(assignedPortal)
            platformLicenseClient.completeProviderPairing()
            platformLicense = platformLicenseClient.currentLicense()
            licenseStatus = "Provider license active."
            providerPairing = null
            if (assignedPortal.pin.isBlank()) {
                providerPairingStatus = "Portal assigned. Set a parental PIN to continue."
                providerPinSetup = assignedPortal
            } else {
                providerPairingStatus = "Portal assigned. Connecting…"
                startConnection(assignedPortal)
            }
            break
        }
    }

    androidx.compose.runtime.LaunchedEffect(
        settingsLoaded, disclaimerAcknowledged, storedSettings.id, storedSettings.url, storedSettings.mac, providerPinSetup
    ) {
        if (!settingsLoaded) return@LaunchedEffect
        if (providerPinSetup != null) return@LaunchedEffect
        if (!disclaimerAcknowledged) return@LaunchedEffect
        if (storedSettings.url.isBlank() || storedSettings.mac.isBlank()) {
            if (!connecting) screen = AppScreen.SETUP
            return@LaunchedEffect
        }
        val key = portalKey(storedSettings)
        if (connecting || autoConnectKey == key) return@LaunchedEffect
        if (!connecting && autoConnectKey != key) {
            startConnection(storedSettings, keepHomeVisible = true)
        }
    }

    fun allStreamFor(media: UiMedia): PortalStream? = findCatalogStream(
        media.id, media.streamType, liveStreams, movieStreams, seriesStreams,
        remoteSearchStreams, localFavoriteStreams,
        vodCatalogs.values.asSequence().flatMap { it.items.asSequence() }
    )

    fun setFavorite(media: UiMedia) {
        val save = media.id !in favoriteIds
        val stream = allStreamFor(media)
        scope.launch {
            settingsManager.setFavorite(media.id, save)
            if (stream != null) {
                catalogCache.updateFavorite(viewerCatalogProfileId, stream, save)
                localFavoriteStreams = if (save) {
                    (localFavoriteStreams.filterNot { it.streamType == stream.streamType && it.id == stream.id } + stream)
                } else {
                    localFavoriteStreams.filterNot { it.id == media.id }
                }
            }
        }
    }

    fun launchPlayback(
        stream: PortalStream,
        command: String? = stream.cmd,
        seriesValue: String = stream.series.orEmpty(),
        episode: Boolean = false,
        resumeFraction: Float = 0f,
        contentId: String = stream.id,
        livePreview: Boolean = false
    ) {
        if (!viewerAllows(stream)) return
        if (stream.streamType == "live") {
            if (!livePreview) focusedLiveChannelId = stream.id
            scope.launch { settingsManager.markRecentlyPlayed(stream.id) }
        }
        tvLivePreviewPlayback = livePreview
        livePreviewVlcAction = null
        livePreviewStream = if (livePreview) stream else null
        if (!livePreview) livePreviewFullscreen = false
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
        selectedLiveChannel = null
        selectedSeries = null
        qualityContext = null
    }

    fun openMedia(stream: PortalStream, resume: Float = 0f) {
        if (!viewerAllows(stream)) return
        when (stream.streamType) {
            "live" -> if (isAndroidTv && selectedTab == StbPlayTab.LIVE) {
                focusedLiveChannelId = stream.id
                if (tvLivePreviewPlayback && livePreviewStream?.id == stream.id) livePreviewFullscreen = true
                else {
                    livePreviewFullscreen = false
                    launchPlayback(stream, livePreview = true)
                }
            } else if (isAndroidTv) selectedLiveChannel = stream else launchPlayback(stream)
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

    fun launchSeriesEpisode(choice: QualityContext, episodes: List<PortalEpisode>, resumeFraction: Float) {
        val episode = choice.episode ?: return
        playingSeries = choice.stream
        playingEpisodes = episodes
        playingEpisodeIndex = episodes.indexOfFirst { it.id == episode.id }
        playRequest = null
        startQualityChoice(choice.copy(resumeFraction = resumeFraction))
    }

    fun chooseSeriesEpisode(series: PortalStream, episode: PortalEpisode, episodes: List<PortalEpisode>) {
        val progress = (progressById[episode.id] ?: 0f).coerceIn(0f, 1f)
        val choice = QualityContext(episode.name, series, episode, resumeFraction = progress)
        if (isAndroidTv && progress > 0f && progress < 0.95f) {
            pendingEpisodeList = episodes
            pendingEpisodePlayback = choice
        } else launchSeriesEpisode(choice, episodes, 0f)
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
    val contentCategories = remember(movieCategories, seriesCategories, parentalMode, allVod, activeViewer, kidsCategoryIds) {
        val categories = (movieCategories + seriesCategories).distinctBy { it.id }
        val flaggedIds = allVod.filter { it.isAdultContent() }.mapNotNullTo(HashSet()) { it.categoryId }
        val visible = when (parentalMode) {
            ParentalMode.ALL_CONTENT -> categories
            ParentalMode.HIDE_ADULT -> categories.filterNot { it.isAdultCategory() }
            ParentalMode.ADULT_ONLY -> categories.filter { it.isAdultCategory() || it.id in flaggedIds }
        }.filter { !activeViewer.isKids || it.id in kidsCategoryIds.second }.map { category ->
            val properAdultCategory = category.isAdultCategory()
            UiCategory(
                category.id,
                category.name,
                parentalMode == ParentalMode.ALL_CONTENT && properAdultCategory,
                properAdultCategory || (parentalMode == ParentalMode.ADULT_ONLY && category.id in flaggedIds)
            )
        }
        if (parentalMode == ParentalMode.ADULT_ONLY) listOf(UiCategory("adult-only", "Adult only", isAdult = true)) + visible
        else listOf(UiCategory("all", "All")) + visible
    }
    val uiLiveCategories = remember(liveCategories, liveStreams, parentalMode, activeViewer, kidsCategoryIds) {
        val flaggedIds = liveStreams.filter { it.isAdultContent() }.mapNotNullTo(HashSet()) { it.categoryId }
        val visible = when (parentalMode) {
            ParentalMode.ALL_CONTENT -> liveCategories
            ParentalMode.HIDE_ADULT -> liveCategories.filterNot { it.isAdultCategory() }
            ParentalMode.ADULT_ONLY -> liveCategories.filter { it.isAdultCategory() || it.id in flaggedIds }
        }.filter { !activeViewer.isKids || it.id in kidsCategoryIds.first }.map { category ->
            val properAdultCategory = category.isAdultCategory()
            UiCategory(
                category.id,
                category.name,
                parentalMode == ParentalMode.ALL_CONTENT && properAdultCategory,
                properAdultCategory || (parentalMode == ParentalMode.ADULT_ONLY && category.id in flaggedIds)
            )
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
        if (!portalReady) {
            if (connecting) pendingPlaybackMedia = media
            return
        }
        val stream = allStreamFor(media) ?: return
        if (!viewerAllows(stream)) return
        if (selectedTab == StbPlayTab.CONTENT && stream.streamType != "live") {
            focusedContentId = stream.id
        }
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
        val properAdultCategory = when (stream.streamType) {
            "live" -> stream.categoryId in explicitAdultLiveCategoryIds
            else -> stream.categoryId in explicitAdultVodCategoryIds
        }
        val needsPin = stream.isLocked || (parentalMode == ParentalMode.ALL_CONTENT && properAdultCategory)
        if (needsPin && parentalMode != ParentalMode.ADULT_ONLY && !categoryUnlocked) pendingLockedMedia = stream else openMedia(stream, media.progress)
    }

    LaunchedEffect(portalReady, connecting, pendingPlaybackMedia) {
        val queued = pendingPlaybackMedia ?: return@LaunchedEffect
        if (portalReady) {
            pendingPlaybackMedia = null
            requestMedia(queued)
        } else if (!connecting && connectionError != null) {
            pendingPlaybackMedia = null
        }
    }

    fun filterByCategory(items: List<PortalStream>, categories: List<UiCategory>, selectedIndex: Int): List<PortalStream> {
        val category = categories.getOrNull(selectedIndex) ?: categories.firstOrNull()
        val isLive = items.firstOrNull()?.streamType == "live"
        val explicitCategoryIds = if (isLive) explicitAdultLiveCategoryIds else explicitAdultVodCategoryIds
        val eligible = when (parentalMode) {
            ParentalMode.ALL_CONTENT -> items
            // In Adult Free, remove adult categories and individually flagged titles.
            // A single R-rated title must not hide every title in its normal category.
            ParentalMode.HIDE_ADULT -> items.filterNot { it.isAdultContent() || it.categoryId in explicitCategoryIds }
            ParentalMode.ADULT_ONLY -> items.filter { stream ->
                stream.isAdultContent() || stream.categoryId in explicitCategoryIds
            }
        }
        val viewerEligible = if (activeViewer.isKids) eligible.filter(::viewerAllows) else eligible
        return when {
            category == null || category.id == "all" -> viewerEligible
            category.id == "adult-only" -> viewerEligible
            category.isAdult && parentalMode == ParentalMode.ADULT_ONLY -> viewerEligible.filter {
                it.categoryId == category.id && (it.isAdultContent() || category.id in explicitCategoryIds)
            }
            else -> viewerEligible.filter { it.categoryId == category.id }
        }
    }

    val filteredLive = remember(activeViewer.id, kidsCategoryIds, liveStreams, uiLiveCategories, liveCategoryIndex, parentalMode, explicitAdultVodCategoryIds, explicitAdultLiveCategoryIds, flaggedAdultVodCategoryIds, flaggedAdultLiveCategoryIds) {
        filterByCategory(liveStreams, uiLiveCategories, liveCategoryIndex)
    }
    fun stepLiveChannel(direction: Int) {
        val currentId = playRequest?.contentId ?: livePreviewStream?.id
        val currentCategory = uiLiveCategories.getOrNull(liveCategoryIndex)
        val categoryUnlocked = parentalMode == ParentalMode.ADULT_ONLY ||
            (currentCategory?.isAdult == true && unlockedAdultCategoryKey == "LIVE:$liveCategoryIndex")
        val channels = (if (filteredLive.any { it.id == currentId }) filteredLive else filterByCategory(liveStreams, uiLiveCategories, liveCategoryIndex))
            .filter { !it.isLocked || categoryUnlocked }
        if (channels.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.id == currentId }
            val nextIndex = if (currentIndex < 0) 0 else (currentIndex + direction + channels.size) % channels.size
            launchPlayback(channels[nextIndex], livePreview = isAndroidTv && tvLivePreviewPlayback)
        }
    }
    val activity = LocalContext.current as? MainActivity
    DisposableEffect(activity, playRequest, livePreviewStream, filteredLive, liveStreams, unlockedAdultCategoryKey) {
        val handler: ((Int) -> Unit)? = if (playRequest?.kind == StalkerContentKind.LIVE || livePreviewStream != null) {
            { direction: Int -> stepLiveChannel(direction) }
        } else null
        activity?.channelStepHandler = handler
        onDispose { activity?.let { if (it.channelStepHandler === handler) it.channelStepHandler = null } }
    }
    val selectedContentCategory = contentCategories.getOrNull(contentCategoryIndex)
    val selectedVodKey = selectedContentCategory?.id ?: "all"
    val selectedVodCatalog = vodCatalogs[selectedVodKey]
    val categoryVod = if (selectedVodKey == "adult-only") allVod else selectedVodCatalog?.items.orEmpty()
    val filteredVod = remember(activeViewer.id, kidsCategoryIds, categoryVod, catalogueLanguage, parentalMode, explicitAdultVodCategoryIds, explicitAdultLiveCategoryIds, flaggedAdultVodCategoryIds, flaggedAdultLiveCategoryIds, contentCategories, contentCategoryIndex) {
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

    fun isAllowedInMode(stream: PortalStream, explicitAdultCategoryIds: Set<String>): Boolean = viewerAllows(stream) && when (parentalMode) {
        ParentalMode.ALL_CONTENT -> !stream.isAdultContent() && stream.categoryId !in explicitAdultCategoryIds
        ParentalMode.HIDE_ADULT -> !stream.isAdultContent() && stream.categoryId !in explicitAdultCategoryIds
        ParentalMode.ADULT_ONLY -> stream.isAdultContent() || stream.categoryId in explicitAdultCategoryIds
    }
    val safeLive = remember(activeViewer.id, kidsCategoryIds, liveStreams, parentalMode, explicitAdultLiveCategoryIds) {
        liveStreams.filter { isAllowedInMode(it, explicitAdultLiveCategoryIds) }
    }
    val safeVod = remember(activeViewer.id, kidsCategoryIds, allVod, parentalMode, explicitAdultVodCategoryIds) {
        allVod.filter { isAllowedInMode(it, explicitAdultVodCategoryIds) }
    }
    val latestMovies = remember(safeVod) { safeVod.asSequence().filter { it.streamType == "movie" }.take(18).toList() }
    val latestSeries = remember(safeVod) { safeVod.asSequence().filter { it.streamType == "series" }.take(18).toList() }
    LaunchedEffect(viewerCatalogProfileId, favoriteIds, liveStreams, movieStreams, seriesStreams, vodCatalogs) {
        val profileId = viewerCatalogProfileId
        if (profileId.isNotBlank() && favoriteIds.isNotEmpty()) {
            delay(250)
            val known = liveStreams + movieStreams + seriesStreams + vodCatalogs.values.flatMap { it.items }
            catalogCache.mergeKnownFavorites(profileId, favoriteIds, known)
            localFavoriteStreams = catalogCache.readFavorites(profileId)
        }
    }
    val favoriteStreams = remember(activeViewer.id, kidsCategoryIds, liveStreams, allVod, localFavoriteStreams, favoriteIds, parentalMode, explicitAdultLiveCategoryIds, explicitAdultVodCategoryIds) {
        (liveStreams + allVod + localFavoriteStreams).filter { stream ->
            stream.id in favoriteIds && isAllowedInMode(
                stream,
                if (stream.streamType == "live") explicitAdultLiveCategoryIds else explicitAdultVodCategoryIds
            )
        }.distinctBy { "${it.streamType}:${it.id}" }
    }
    val continueStreams = remember(safeVod, safeLive, progressById) {
        (safeVod + safeLive).filter { (progressById[it.id] ?: 0f) > 0f }
            .distinctBy { "${it.streamType}:${it.id}" }
            .sortedByDescending { progressById[it.id] ?: 0f }
    }
    val recommendationCandidates = remember(safeVod) { safeVod.take(1_200) }
    val recommendationStreams = remember(recommendationCandidates, continueStreams, favoriteStreams) {
        val signalGenres = (continueStreams + favoriteStreams).mapNotNull { it.genre?.lowercase() }.toSet()
        val signalLanguages = (continueStreams + favoriteStreams).mapNotNull { it.language?.lowercase() }.toSet()
        val watchedIds = continueStreams.mapTo(HashSet()) { it.id }
        // Provider catalogues can contain 100k+ titles. Score a bounded, recent
        // candidate window instead of sorting the full catalogue on the UI thread.
        recommendationCandidates.asSequence()
            .filter { it.id !in watchedIds }
            .sortedByDescending { stream ->
                (if (stream.genre?.lowercase() in signalGenres) 2 else 0) +
                    (if (stream.language?.lowercase() in signalLanguages) 1 else 0)
            }
            .take(18)
            .toList()
    }

    val homeHeroStreams = remember(safeVod, safeLive) {
        (safeVod.asSequence() + safeLive.asSequence()).take(7).toList()
    }
    val artworkToken = portalRepository.getHandshakeToken()
    val artworkCookie = portalRepository.getSessionCookie()
    val searchCatalog = rememberSearchCatalog(selectedTab, safeLive, safeVod, favoriteStreams, catalogueLanguage)
    val favouriteUiItems = rememberFavouriteUiItems(
        favoriteStreams, progressById, favoriteIds, storedSettings, catalogGeneration,
        artworkToken, artworkCookie, portalReady, ::toUi
    )
    val homeHeroes = remember(homeHeroStreams, progressById, favoriteIds, catalogGeneration, artworkToken, artworkCookie) {
        homeHeroStreams.map(::toUi)
    }
    val favouriteLive = remember(favoriteStreams) { favoriteStreams.filter { it.streamType == "live" } }
    val homeContinue = remember(continueStreams) { continueStreams.take(16) }
    val homeFavouriteLive = remember(favouriteLive) { favouriteLive.take(16) }
    val homeLive = remember(safeLive) { safeLive.take(16) }
    val homeRows = remember(homeContinue, recommendationStreams, homeFavouriteLive, homeLive, latestMovies, latestSeries,
        progressById, favoriteIds, catalogGeneration, artworkToken, artworkCookie) {
        buildList {
            if (homeContinue.isNotEmpty()) add(UiMediaRow("continue", "Continue Watching", "Saved on this device", homeContinue.map(::toUi)))
            if (recommendationStreams.isNotEmpty()) add(UiMediaRow("recommended", "Recommended for you", "Based on what you watch", recommendationStreams.map(::toUi)))
            if (homeFavouriteLive.isNotEmpty()) add(UiMediaRow("favorites-live", "Favourite channels", items = homeFavouriteLive.map(::toUi)))
            if (homeLive.isNotEmpty()) add(UiMediaRow("live", "Live TV", items = homeLive.map(::toUi)))
            if (latestMovies.isNotEmpty()) add(UiMediaRow("latest-movies", "Latest releases", "Movies", latestMovies.map(::toUi)))
            if (latestSeries.isNotEmpty()) add(UiMediaRow("latest-series", "Latest releases", "Series", latestSeries.map(::toUi)))
        }
    }
    val homeLoading = screen == AppScreen.LOADING
    LaunchedEffect(storedSettings.id, storedSettings.url, storedSettings.mac, portalReady) {
        val identity = "${storedSettings.id}|${storedSettings.url}|${storedSettings.mac}"
        val raw = subscriptionCache.getString(identity, null)
        val profileDetails = if (portalReady) portalRepository.getSubscription() else PortalSubscription()
        subscription = profileDetails
        subscriptionMessage = if (!portalReady && raw != null) "Saved portal details" else ""
        if (raw != null && profileDetails == PortalSubscription()) runCatching {
            val saved = org.json.JSONObject(raw)
            subscription = PortalSubscription(saved.optString("plan", "Subscription"), saved.optString("status"),
                saved.optLong("expiry", -1L).takeIf { it > 0L }, saved.optBoolean("unlimited"))
        }
        if (portalReady) {
            subscriptionMessage = "Refreshing portal subscription…"
            val fresh = try {
                portalRepository.refreshSubscription()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                subscriptionMessage = if (subscription != PortalSubscription()) "Account details unavailable. Showing saved/profile details." else "Could not fetch portal subscription. Refresh to try again."
                if (subscription != PortalSubscription()) subscriptionCache.edit().putString(identity, org.json.JSONObject()
                    .put("plan", subscription.plan).put("status", subscription.status)
                    .put("expiry", subscription.expiryEpochMillis ?: -1L).put("unlimited", subscription.unlimited).toString()).apply()
                return@LaunchedEffect
            }
            subscriptionMessage = if (fresh == PortalSubscription()) "The portal did not supply subscription details." else ""
            subscription = fresh
            subscriptionCache.edit().putString(identity, org.json.JSONObject()
                .put("plan", fresh.plan).put("status", fresh.status)
                .put("expiry", fresh.expiryEpochMillis ?: -1L).put("unlimited", fresh.unlimited).toString()).apply()
            PortalExpiryReminderWorker.setExpiry(appContext, fresh.expiryEpochMillis.takeUnless { fresh.unlimited })
        }
    }

    val homeExpiry = subscription.toExpiryText()
    val homeState = remember(homeRows, homeHeroes, homeLoading, connectionError, homeExpiry) {
        StbPlayHomeState(loading = homeLoading, heroes = homeHeroes, portalWarning = connectionError,
            rows = homeRows, expiryText = homeExpiry)
    }
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
        emptyMessage = selectedVodCatalog?.error ?: "Try another category or refresh the portal."
    )
    val favouritesState = StbPlayLibraryState(items = favouriteUiItems)
    val accessInfo = appAccessInfo(platformLicense, platformLicenseClient.providerPortalId() != null, demoTrialStartedAt, System.currentTimeMillis())
    val licenseExpiryMillis = accessInfo.expiresAtMillis
    val settingsState = StbPlaySettingsState(
        profiles = profiles,
        activeProfileId = storedSettings.id,
        subscriptionPlan = subscription.plan,
        subscriptionStatus = subscription.status,
        expiryText = subscription.toExpiryText().orEmpty(),
        licenseName = accessInfo.name,
        appAccessStatus = accessInfo.status,
        deviceReference = platformLicenseClient.displayDeviceReference(),
        portalMac = storedSettings.mac,
        portalSubscriptionMessage = subscriptionMessage,
        licenseExpiryText = when {
            licenseExpiryMillis == null -> "No expiry set"
            licenseExpiryMillis <= System.currentTimeMillis() -> "Expired ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(java.util.Date(licenseExpiryMillis))}"
            else -> "Expires ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(java.util.Date(licenseExpiryMillis))} · ${kotlin.math.ceil((licenseExpiryMillis - System.currentTimeMillis()).toDouble() / TimeUnit.DAYS.toMillis(1)).toInt()} days left"
        },
        liveCount = liveStreams.size,
        movieCount = movieStreams.size,
        seriesCount = seriesStreams.size,
        playerPreference = playerPreference,
        androidBoxVideoCompatibility = androidBoxVideoCompatibility,
        themePreference = themePreference,
        phoneCategoryPosition = phoneCategoryPosition,
        phoneMovieColumns = phoneMovieColumns,
        parentalMode = parentalMode,
        kidsProfile = activeViewer.isKids,
        viewerName = activeViewer.name,
        subtitlePreference = subtitlePreference,
        catalogueLanguage = catalogueLanguage,
        analyticsEnabled = analyticsEnabled,
        lastRefreshText = lastRefreshAt.toDateTimeText(),
        updateText = updateText,
        updateAvailableVersion = updateInfo?.version,
        licenseStatus = licenseStatus,
        licenseBusy = licenseBusy
    )

    LaunchedEffect(portalReady, storedSettings.url) {
        if (portalReady) {
            platformLicenseClient.heartbeat(storedSettings.url)?.let {
                licenseStatus = it
                platformLicense = platformLicenseClient.currentLicense()
            }
        }
    }

    fun activatePlatformKey(key: String) {
        if (licenseBusy) return
        licenseBusy = true
        licenseStatus = "Checking key…"
        scope.launch {
            try {
                licenseStatus = platformLicenseClient.activate(key, storedSettings.url)
                platformLicense = platformLicenseClient.currentLicense()
            } catch (error: Exception) {
                licenseStatus = error.message ?: "Could not verify this key. Try again."
            } finally {
                licenseBusy = false
            }
        }
    }

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
                if (contentCategoryIndex != index) {
                    focusedContentId = null
                    scope.launch { contentGridState.scrollToItem(0) }
                }
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
                !settingsLoaded || viewerProfiles.isEmpty() -> Box(Modifier.fillMaxSize().background(Color(0xFF071425)))
                !policyAccepted -> FirstStartDisclaimer {
                    privacyPrefs.edit().putInt("policy_accepted_version", REQUIRED_POLICY_VERSION).apply()
                    acceptedPolicyVersion = REQUIRED_POLICY_VERSION
                    scope.launch { settingsManager.acknowledgeDisclaimer() }
                }
                viewerProfiles.isNotEmpty() && unlockedViewerId != activeViewer.id -> com.example.stbplay.ui.ViewerProfilesScreen(
                    profiles = viewerProfiles, ownerPin = storedSettings.pin,
                    liveCategories = liveCategories, vodCategories = (movieCategories + seriesCategories).distinctBy { it.id },
                    approvalPortalKey = portalKey(storedSettings),
                    onSelected = { viewer -> scope.launch { settingsManager.activateViewer(viewer.id); unlockedViewerId = viewer.id } },
                    onSave = { viewer, pin -> scope.launch { settingsManager.saveViewer(viewer, pin) } },
                    onSetOwnerPin = { pin -> scope.launch { settingsManager.updateParentalPin(storedSettings.pin, pin) } }
                )
                phoneDevice && !phoneSetupComplete && viewerProfiles.isNotEmpty() -> com.example.stbplay.ui.PhoneFirstSetup(
                    phoneCategoryPosition, phoneMovieColumns, themePreference,
                    onPosition = { scope.launch { settingsManager.setPhoneCategoryPosition(it) } },
                    onColumns = { scope.launch { settingsManager.setPhoneMovieColumns(it) } },
                    onTheme = { scope.launch { settingsManager.setThemePreference(it) } },
                    onDone = { scope.launch { settingsManager.completePhoneSetup() } }
                )
                screen == AppScreen.APP && (storedSettings.url.isBlank() || storedSettings.mac.isBlank()) ->
                    Box(Modifier.fillMaxSize().background(Color(0xFF071425)))
                playRequest != null && !tvLivePreviewPlayback -> {
                    val currentRequest = playRequest!!
                    val currentProgressGeneration = remember(currentRequest) { progressResetGeneration }
                    fun playEpisodeAt(index: Int, promptForResume: Boolean = true) {
                        val series = playingSeries ?: return
                        val episode = playingEpisodes.getOrNull(index) ?: return
                        if (promptForResume) chooseSeriesEpisode(series, episode, playingEpisodes)
                        else launchSeriesEpisode(QualityContext(episode.name, series, episode), playingEpisodes, 0f)
                    }
                    androidx.compose.runtime.key(playbackSessionGeneration) { PlaybackRoute(
                    request = currentRequest,
                    portalUiUrl = storedSettings.url,
                    macAddress = storedSettings.mac,
                    token = portalRepository.getHandshakeToken(),
                    sessionCookie = portalRepository.getSessionCookie(),
                    playerPreference = playerPreference,
                    androidBoxVideoCompatibility = androidBoxVideoCompatibility,
                    subtitlePreference = subtitlePreference,
                    onProgress = { position, duration ->
                        if (currentRequest.kind != StalkerContentKind.LIVE && currentProgressGeneration == progressResetGeneration)
                            scope.launch {
                                if (currentProgressGeneration == progressResetGeneration)
                                    settingsManager.saveProgress(currentRequest.contentId, position, duration)
                            }
                    },
                    onPlaybackFailure = {
                        if (currentRequest.contentId !in recoveryAttemptedContentIds) {
                            recoveryAttemptedContentIds = recoveryAttemptedContentIds + currentRequest.contentId
                            scope.launch {
                                val login = com.example.stbplay.data.retryPortalLogin { portalRepository.initialize(storedSettings) }
                                if (login.success) {
                                    portalReady = true
                                    subscription = portalRepository.getSubscription()
                                    PortalExpiryReminderWorker.setExpiry(appContext, subscription.expiryEpochMillis.takeUnless { subscription.unlimited })
                                    connectionError = null
                                    playbackSessionGeneration++
                                } else {
                                    portalReady = false
                                    connectionError = "Portal connection failed after playback recovery. Check the portal details or provider service status."
                                }
                            }
                        }
                    },
                    episodeTitles = if (currentRequest.kind == StalkerContentKind.SERIES_EPISODE) playingEpisodes.map { it.name } else emptyList(),
                    currentEpisodeIndex = playingEpisodeIndex,
                    onEpisodeSelected = { playEpisodeAt(it) },
                    onPlaybackEnded = {
                        if (currentRequest.kind == StalkerContentKind.SERIES_EPISODE && playingEpisodeIndex + 1 < playingEpisodes.size)
                            playEpisodeAt(playingEpisodeIndex + 1, promptForResume = false)
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
                    ) }
                }
                qualityContext != null -> QualitySelectionScreen(
                    title = qualityContext!!.title,
                    options = qualityOptions,
                    isLoading = qualityLoading,
                    error = qualityError,
                    onOptionClick = ::chooseQuality,
                    onBack = { qualityContext = null; qualityOptions = emptyList() }
                )
                selectedLiveChannel != null -> {
                    val channel = selectedLiveChannel!!
                    val media = toUi(channel)
                    TvMediaDetailsScreen(media,
                        onPlay = { launchPlayback(channel) }, onResume = { launchPlayback(channel) },
                        onToggleFavorite = { setFavorite(media) },
                        onRemoveHistory = if (channel.id in progressById) ({
                            scope.launch { settingsManager.removeFromHistory(channel.id) }
                            selectedLiveChannel = null
                        }) else null,
                        onBack = { selectedLiveChannel = null; startCategoryPreload(storedSettings) })
                }
                selectedMovie != null -> {
                    val movie = selectedMovie!!
                    val media = toUi(movie)
                    MovieDetailsScreen(
                        item = media,
                        onPlay = { startQualityChoice(QualityContext(movie.name, movie)) },
                        onResume = { startQualityChoice(QualityContext(movie.name, movie, resumeFraction = media.progress)) },
                        onToggleFavorite = { setFavorite(media) },
                        onBack = { selectedMovie = null; startCategoryPreload(storedSettings) },
                        onRemoveHistory = if (isAndroidTv && movie.id in progressById) ({
                            scope.launch { settingsManager.removeFromHistory(movie.id) }
                            selectedMovie = null
                        }) else null
                    )
                }
                selectedSeries != null -> {
                    val series = selectedSeries!!
                    SeriesDetailsScreen(
                        series = series,
                        repository = portalRepository,
                        isFavorite = series.id in favoriteIds,
                        onToggleFavorite = { setFavorite(toUi(series)) },
                        episodeProgress = if (isAndroidTv) progressById else emptyMap(),
                        onEpisodeClick = { episode, episodes ->
                            chooseSeriesEpisode(series, episode, episodes)
                        },
                        onBack = { selectedSeries = null; startCategoryPreload(storedSettings) },
                        onRemoveHistory = if (isAndroidTv && series.id in progressById) ({
                            scope.launch { settingsManager.removeFromHistory(series.id) }
                            selectedSeries = null
                        }) else null
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
                    onProviderPair = ::startProviderPairing,
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
                else -> {
                  val renderApp: @Composable () -> Unit = { StbPlayApp(
                    homeState = homeState,
                    liveState = liveState,
                    contentState = contentState,
                    favouritesState = favouritesState,
                    settingsState = settingsState,
                    onActivateLicense = ::activatePlatformKey,
                    selectedTab = selectedTab,
                    liveChannelListState = liveChannelListState,
                    focusedLiveChannelId = focusedLiveChannelId,
                    focusedContentId = focusedContentId,
                    contentGridState = contentGridState,
                    onTabSelected = { tab ->
                        if (tab != selectedTab) {
                            if (tvLivePreviewPlayback) playRequest = null
                            tvLivePreviewPlayback = false
                            livePreviewFullscreen = false
                            livePreviewStream = null
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
                    livePreviewActionsRequester = livePreviewActionsRequester,
                    playingLiveChannelId = if (tvLivePreviewPlayback) playRequest?.contentId else null,
                    livePreview = { modifier, returnToChannels ->
                        androidx.compose.runtime.SideEffect { livePreviewReturnFocus[0] = returnToChannels }
                        TvLivePreviewPanel(
                            channel = livePreviewStream?.let(::toUi),
                            actionsRequester = livePreviewActionsRequester,
                            modifier = modifier,
                            onVideoBounds = { if (livePreviewBounds != it) livePreviewBounds = it },
                            onFullscreen = { if (playRequest != null) livePreviewFullscreen = true },
                            onFavorite = { livePreviewStream?.let { setFavorite(toUi(it)) } },
                            onVlc = { livePreviewVlcAction?.invoke() },
                            onBack = returnToChannels
                        )
                    },
                    onToggleFavorite = ::setFavorite,
                    onRemoveHistory = { media -> scope.launch { settingsManager.removeFromHistory(media.id) } },
                    onRefresh = { refreshPortal(storedSettings) },
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
                    onAddPortal = {
                        editingPortal = PortalSettings(
                            pin = storedSettings.pin,
                            mac = if (isAndroidTv) storedSettings.mac else ""
                        )
                        screen = AppScreen.SETUP
                    },
                    onEditPortal = { profile -> if (!activeViewer.isKids) { editingPortal = profile.copy(pin = storedSettings.pin); screen = AppScreen.SETUP } },
                    onUsePortal = { profile ->
                        scope.launch { settingsManager.activatePortal(profile.id) }
                        autoConnectKey = null
                        screen = AppScreen.LOADING
                        loadingStage = "Switching portal…"
                        loadingProgress = 0.04f
                    },
                    onDeletePortal = { profile -> scope.launch { settingsManager.deletePortal(profile.id) } },
                    onProviderPair = ::startProviderPairing,
                    onPlayerPreferenceChanged = { preference -> scope.launch { settingsManager.setPlayerPreference(preference) } },
                    onAndroidBoxVideoCompatibilityChanged = { enabled -> scope.launch { settingsManager.setAndroidBoxVideoCompatibility(enabled) } },
                    onPhoneCategoryPositionChanged = { position -> scope.launch { settingsManager.setPhoneCategoryPosition(position) } },
                    onPhoneMovieColumnsChanged = { columns -> scope.launch { settingsManager.setPhoneMovieColumns(columns) } },
                    onThemePreferenceChanged = { preference -> scope.launch { settingsManager.setThemePreference(preference) } },
                    onSubtitlePreferenceChanged = { preference -> scope.launch { settingsManager.setSubtitlePreference(preference) } },
                    onCatalogueLanguageChanged = { language -> scope.launch { settingsManager.setCatalogueLanguage(language) } },
                    onAnalyticsChanged = { enabled -> scope.launch { settingsManager.setAnalyticsEnabled(enabled) } },
                    onChangePin = { if (!activeViewer.isKids) changingPin = true },
                    onParentalModeChanged = { mode ->
                        if (!activeViewer.isKids && mode != parentalMode) {
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
                    onSwitchViewer = {
                        playRequest = null; livePreviewStream = null; tvLivePreviewPlayback = false
                        selectedMovie = null; selectedSeries = null; selectedLiveChannel = null
                        unlockedAdultCategoryKey = null; pendingLockedMedia = null; pendingParentalMode = null
                        unlockedViewerId = null; selectedTab = StbPlayTab.HOME
                    },
                    searchSession = searchSession,
                    searchHistory = searchHistory,
                    onRememberSearch = { query -> scope.launch { settingsManager.addSearchHistory(query) } },
                    onClearSearchHistory = { scope.launch { settingsManager.clearSearchHistory() } },
                    onSearchVisibilityChanged = { visible ->
                        searchVisible = visible
                        if (visible) categoryPreloadJob?.cancel()
                        else scope.launch {
                            delay(1_000)
                            if (playRequest == null && selectedMovie == null && selectedSeries == null) startCategoryPreload(storedSettings)
                        }
                    },
                    searchCatalog = searchCatalog,
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
                    searchMedia = ::toUi,
                    onHomeInteraction = { if (isAndroidTv) lastHomeInteractionMs[0] = SystemClock.elapsedRealtime() },
                    onMediaFocused = { tab, media ->
                        if (isAndroidTv) when (tab) {
                            StbPlayTab.LIVE -> focusedLiveChannelId = media.id
                            StbPlayTab.CONTENT -> focusedContentId = media.id
                            else -> Unit
                        }
                    }
                  ) }
                  if (isAndroidTv) appUiStateHolder.SaveableStateProvider(storedSettings.id.ifBlank { "setup" }) {
                      Box(Modifier.fillMaxSize().then(if (livePreviewFullscreen) Modifier.focusProperties { canFocus = false } else Modifier)) { renderApp() }
                  }
                  else renderApp()
                }
            }

            // One instance of the existing homepage playback route stays mounted
            // while its bounds change between the preview slot and fullscreen.
            if (isAndroidTv && tvLivePreviewPlayback && playRequest != null &&
                (livePreviewFullscreen || livePreviewBounds.width > 0f)) {
                val previewRequest = playRequest!!
                val playerModifier = if (livePreviewFullscreen) Modifier.fillMaxSize()
                else with(density) {
                    Modifier.offset { IntOffset(livePreviewBounds.left.toInt(), livePreviewBounds.top.toInt()) }
                        .size(livePreviewBounds.width.toDp(), livePreviewBounds.height.toDp())
                }
                Box(playerModifier) {
                    androidx.compose.runtime.key(playbackSessionGeneration) { PlaybackRoute(
                        request = previewRequest,
                        portalUiUrl = storedSettings.url,
                        macAddress = storedSettings.mac,
                        token = portalRepository.getHandshakeToken(),
                        sessionCookie = portalRepository.getSessionCookie(),
                        playerPreference = playerPreference,
                        subtitlePreference = subtitlePreference,
                        androidBoxVideoCompatibility = androidBoxVideoCompatibility,
                        embedded = !livePreviewFullscreen,
                        onVlcActionAvailable = { livePreviewVlcAction = it },
                        onChannelStep = ::stepLiveChannel,
                    onPlaybackFailure = {
                        if (previewRequest.contentId !in recoveryAttemptedContentIds) {
                            recoveryAttemptedContentIds = recoveryAttemptedContentIds + previewRequest.contentId
                            scope.launch {
                                val login = com.example.stbplay.data.retryPortalLogin { portalRepository.initialize(storedSettings) }
                                if (login.success) {
                                    portalReady = true
                                    subscription = portalRepository.getSubscription()
                                    PortalExpiryReminderWorker.setExpiry(appContext, subscription.expiryEpochMillis.takeUnless { subscription.unlimited })
                                    connectionError = null
                                    playbackSessionGeneration++
                                } else {
                                    portalReady = false
                                    connectionError = "Portal connection failed after playback recovery. Check the portal details or provider service status."
                                }
                            }
                        }
                    },
                        onBack = {
                            livePreviewFullscreen = false
                            scope.launch {
                                androidx.compose.runtime.withFrameNanos { }
                                livePreviewReturnFocus[0]()
                            }
                        }
                    ) }
                }
            }
            pendingLockedMedia?.let { locked ->
                PinPrompt(
                    title = locked.name,
                    expectedPin = storedSettings.pin,
                    onVerified = { pendingLockedMedia = null; openMedia(locked) },
                    onCancel = { pendingLockedMedia = null }
                )
            }
            pendingEpisodePlayback?.let { choice ->
                EpisodeResumePrompt(
                    title = choice.title,
                    onResume = {
                        pendingEpisodePlayback = null
                        launchSeriesEpisode(choice, pendingEpisodeList, choice.resumeFraction)
                    },
                    onStartOver = {
                        // Ignore the old player's final callback when restarting
                        // the same episode so it cannot restore the cleared point.
                        progressResetGeneration++
                        scope.launch {
                            settingsManager.removeFromHistory(choice.episode!!.id)
                            pendingEpisodePlayback = null
                            launchSeriesEpisode(choice, pendingEpisodeList, 0f)
                        }
                    },
                    onCancel = { pendingEpisodePlayback = null }
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
            providerPinSetup?.let { portal ->
                ProviderPinSetupPrompt(
                    onSave = { pin ->
                        scope.launch {
                            val pinnedPortal = portal.copy(pin = pin)
                            settingsManager.upsertPortal(pinnedPortal)
                            providerPinSetup = null
                            startConnection(pinnedPortal)
                        }
                    },
                    onCancel = { providerPinSetup = null }
                )
            }
            ProviderPairingDialog(
                session = providerPairing,
                status = providerPairingStatus,
                onDismiss = {
                    val session = providerPairing
                    scope.launch { platformLicenseClient.cancelProviderPairing(session) }
                    providerPairing = null
                    providerPairingStatus = "Waiting for your provider to assign a portal."
                }
            )
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

private const val REQUIRED_POLICY_VERSION = 3

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
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(expiry))
    val diff = expiry - System.currentTimeMillis()
    if (diff <= 0L) return "Subscription expired on $date"
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        days == 0L -> "Expires today · $date"
        days == 1L -> "Expires tomorrow · $date"
        else -> "Expires $date · $days days left"
    }
}

private fun Long.toDateTimeText(): String = if (this <= 0L) "Not refreshed yet"
else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))
