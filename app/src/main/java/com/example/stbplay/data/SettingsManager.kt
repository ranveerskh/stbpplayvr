package com.example.stbplay.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.stbplay.domain.model.PortalSettings
import com.example.stbplay.domain.model.generateStbPlayMac
import com.example.stbplay.isAndroidTvDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "stb_play_settings")

enum class PlayerPreference { AUTO, INTERNAL, VLC }
enum class ThemePreference { BLUE, LIGHT, BLACK }
enum class CategoryDropdownPosition { TOP, BOTTOM }
enum class ParentalMode { ALL_CONTENT, HIDE_ADULT, ADULT_ONLY }
enum class SubtitlePreference { AUTO, OFF, ENGLISH, HINDI, PUNJABI }

data class WatchProgress(
    val contentId: String,
    val fraction: Float,
    val updatedAt: Long
)

data class SettingsBootstrap(
    val profiles: List<PortalSettings>,
    val activePortal: PortalSettings,
    val disclaimerAcknowledged: Boolean
)

/**
 * All personal settings remain local to this Android TV. Portal URLs and MAC
 * addresses are not placed in analytics, diagnostics, or a public catalogue.
 */
class SettingsManager(private val context: Context) {

    private val keyPortals = stringPreferencesKey("portal_profiles_v2")
    private val keyActivePortal = stringPreferencesKey("active_portal_id")
    private val keySharedDeviceMac = stringPreferencesKey("shared_device_mac")
    private val isAndroidTvDevice = context.isAndroidTvDevice()
    private val keyParentalPin = stringPreferencesKey("parental_pin")
    private val keyParentalMode = stringPreferencesKey("parental_mode")
    private val keyFavoritesPrefix = "favorite_ids_"
    private val keyProgressPrefix = "vod_progress_"
    private val keyPlayer = stringPreferencesKey("player_preference")
    private val keyAndroidBoxVideoCompatibility = booleanPreferencesKey("android_box_video_compatibility")
    private val keyPhoneCategoryPosition = stringPreferencesKey("phone_category_position")
    private val keyPhoneMovieColumns = intPreferencesKey("phone_movie_columns")
    private val keyTheme = stringPreferencesKey("theme_preference")
    private val keySubtitles = stringPreferencesKey("subtitle_preference")
    private val keyLanguage = stringPreferencesKey("catalogue_language")
    private val keyAnalytics = booleanPreferencesKey("analytics_enabled")
    private val keyDisclaimer = booleanPreferencesKey("disclaimer_acknowledged")
    private val keyLastRefresh = longPreferencesKey("last_catalogue_refresh")

    // Migration keys for the first native STB Play test builds.
    private val legacyName = stringPreferencesKey("portal_name")
    private val legacyUrl = stringPreferencesKey("portal_url")
    private val legacyMac = stringPreferencesKey("portal_mac")
    private val legacyPin = stringPreferencesKey("portal_pin")

    val portalProfiles: Flow<List<PortalSettings>> = context.dataStore.data.map { prefs ->
        profilesFrom(prefs[keyPortals], prefs)
    }

    /** A single persisted snapshot prevents startup from routing before portal/setup state is known. */
    val startupSettings: Flow<SettingsBootstrap> = context.dataStore.data.map { prefs ->
        val profiles = profilesFrom(prefs[keyPortals], prefs)
        val activeId = prefs[keyActivePortal].orEmpty()
        val active = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        SettingsBootstrap(
            profiles = profiles,
            activePortal = active?.copy(pin = prefs[keyParentalPin] ?: prefs[legacyPin].orEmpty())
                ?: defaultPortal(prefs),
            disclaimerAcknowledged = prefs[keyDisclaimer] ?: false
        )
    }

    val portalSettings: Flow<PortalSettings> = context.dataStore.data.map { prefs ->
        val profiles = profilesFrom(prefs[keyPortals], prefs)
        val activeId = prefs[keyActivePortal].orEmpty()
        val active = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        active?.copy(pin = prefs[keyParentalPin] ?: prefs[legacyPin].orEmpty()) ?: defaultPortal(prefs)
    }

    val favoriteIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val profileId = activeProfileId(prefs)
        if (profileId.isBlank()) emptySet()
        else prefs[stringSetPreferencesKey(keyFavoritesPrefix + profileId)].orEmpty()
    }

    val vodProgress: Flow<Map<String, Float>> = context.dataStore.data.map { prefs ->
        val profileId = activeProfileId(prefs)
        if (profileId.isBlank()) emptyMap()
        else parseProgress(prefs[stringPreferencesKey(keyProgressPrefix + profileId)].orEmpty())
            .mapValues { it.value.first }
    }

    val watchHistory: Flow<List<WatchProgress>> = context.dataStore.data.map { prefs ->
        val profileId = activeProfileId(prefs)
        if (profileId.isBlank()) emptyList()
        else parseProgress(prefs[stringPreferencesKey(keyProgressPrefix + profileId)].orEmpty())
            .map { (id, value) -> WatchProgress(id, value.first, value.second) }
            .sortedByDescending { it.updatedAt }
    }

    val playerPreference: Flow<PlayerPreference> = context.dataStore.data.map {
        runCatching { PlayerPreference.valueOf(it[keyPlayer] ?: PlayerPreference.AUTO.name) }
            .getOrDefault(PlayerPreference.AUTO)
    }

    /** Use SurfaceView first on Android TV/boxes that render HEVC as green or blue. */
    val androidBoxVideoCompatibility: Flow<Boolean> = context.dataStore.data.map {
        it[keyAndroidBoxVideoCompatibility] ?: false
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map {
        when (it[keyTheme]) {
            "DARK", "SYSTEM", ThemePreference.BLACK.name -> ThemePreference.BLACK
            ThemePreference.LIGHT.name -> ThemePreference.LIGHT
            else -> ThemePreference.BLUE
        }
    }

    val phoneCategoryPosition: Flow<CategoryDropdownPosition> = context.dataStore.data.map {
        runCatching { CategoryDropdownPosition.valueOf(it[keyPhoneCategoryPosition] ?: "TOP") }
            .getOrDefault(CategoryDropdownPosition.TOP)
    }
    val phoneMovieColumns: Flow<Int> = context.dataStore.data.map { (it[keyPhoneMovieColumns] ?: 2).coerceIn(2, 4) }

    suspend fun setPhoneCategoryPosition(value: CategoryDropdownPosition) {
        context.dataStore.edit { it[keyPhoneCategoryPosition] = value.name }
    }
    suspend fun setPhoneMovieColumns(value: Int) {
        context.dataStore.edit { it[keyPhoneMovieColumns] = value.coerceIn(2, 4) }
    }

    val parentalMode: Flow<ParentalMode> = context.dataStore.data.map {
        runCatching { ParentalMode.valueOf(it[keyParentalMode] ?: ParentalMode.ALL_CONTENT.name) }
            .getOrDefault(ParentalMode.ALL_CONTENT)
    }

    val subtitlePreference: Flow<SubtitlePreference> = context.dataStore.data.map {
        runCatching { SubtitlePreference.valueOf(it[keySubtitles] ?: SubtitlePreference.AUTO.name) }
            .getOrDefault(SubtitlePreference.AUTO)
    }

    val catalogueLanguage: Flow<String> = context.dataStore.data.map { it[keyLanguage] ?: "All" }
    val analyticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[keyAnalytics] ?: false }
    val disclaimerAcknowledged: Flow<Boolean> = context.dataStore.data.map { it[keyDisclaimer] ?: false }
    val lastRefreshAt: Flow<Long> = context.dataStore.data.map { it[keyLastRefresh] ?: 0L }

    suspend fun upsertPortal(settings: PortalSettings, makeActive: Boolean = true) {
        val prepared = settings.normalized().withStableId()
        context.dataStore.edit { prefs ->
            val existing = profilesFrom(prefs[keyPortals], prefs)
            val sharedMac = if (isAndroidTvDevice) {
                prepared.mac.takeIf(::isValidMac)
                    ?: prefs[keySharedDeviceMac]?.takeIf(::isValidMac)
                    ?: existing.firstOrNull()?.mac?.takeIf(::isValidMac)
                    ?: generatedDeviceMac()
            } else prepared.mac
            val saved = prepared.copy(mac = sharedMac)
            if (isAndroidTvDevice) prefs[keySharedDeviceMac] = sharedMac
            val profiles = existing.map { profile ->
                if (isAndroidTvDevice) profile.copy(mac = sharedMac) else profile
            }.toMutableList()
            val index = profiles.indexOfFirst { it.id == saved.id }
            val profileOnly = saved.copy(pin = "")
            if (index >= 0) profiles[index] = profileOnly else profiles.add(profileOnly)
            prefs[keyPortals] = encodeProfiles(profiles)
            if (makeActive) prefs[keyActivePortal] = saved.id
            if (saved.pin.isNotBlank()) prefs[keyParentalPin] = saved.pin
        }
    }

    suspend fun savePortalSettings(settings: PortalSettings) = upsertPortal(settings, makeActive = true)

    suspend fun activatePortal(id: String) {
        context.dataStore.edit { prefs ->
            if (profilesFrom(prefs[keyPortals], prefs).any { it.id == id }) {
                prefs[keyActivePortal] = id
            }
        }
    }

    suspend fun deletePortal(id: String) {
        context.dataStore.edit { prefs ->
            val profiles = profilesFrom(prefs[keyPortals], prefs).filterNot { it.id == id }
            if (profiles.isEmpty()) {
                prefs.remove(keyPortals)
                prefs.remove(keyActivePortal)
            } else {
                prefs[keyPortals] = encodeProfiles(profiles)
                if (prefs[keyActivePortal] == id) prefs[keyActivePortal] = profiles.first().id
            }
        }
    }

    suspend fun updateParentalPin(currentPin: String, newPin: String): Boolean {
        if (newPin.length !in 4..8 || !newPin.all(Char::isDigit)) return false
        var accepted = false
        context.dataStore.edit { prefs ->
            val stored = prefs[keyParentalPin] ?: prefs[legacyPin].orEmpty()
            if (stored.isBlank() || stored == currentPin) {
                prefs[keyParentalPin] = newPin
                accepted = true
            }
        }
        return accepted
    }

    suspend fun setFavorite(contentId: String, favorite: Boolean) {
        if (contentId.isBlank()) return
        context.dataStore.edit { prefs ->
            val profileId = activeProfileId(prefs)
            if (profileId.isBlank()) return@edit
            val key = stringSetPreferencesKey(keyFavoritesPrefix + profileId)
            val ids = prefs[key].orEmpty().toMutableSet()
            if (favorite) ids.add(contentId) else ids.remove(contentId)
            prefs[key] = ids
        }
    }

    suspend fun saveProgress(contentId: String, positionMs: Long, durationMs: Long) {
        if (contentId.isBlank() || durationMs <= 0L || positionMs <= 0L) return
        val fraction = (positionMs.toDouble() / durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
        context.dataStore.edit { prefs ->
            val profileId = activeProfileId(prefs)
            if (profileId.isBlank()) return@edit
            val key = stringPreferencesKey(keyProgressPrefix + profileId)
            val progress = parseProgress(prefs[key].orEmpty()).toMutableMap()
            if (fraction >= 0.95f) progress.remove(contentId)
            else progress[contentId] = fraction to System.currentTimeMillis()
            prefs[key] = encodeProgress(progress)
        }
    }

    /** Records a live channel in Continue Watching without pretending it has seek progress. */
    suspend fun markRecentlyPlayed(contentId: String) {
        if (contentId.isBlank()) return
        context.dataStore.edit { prefs ->
            val profileId = activeProfileId(prefs)
            if (profileId.isBlank()) return@edit
            val key = stringPreferencesKey(keyProgressPrefix + profileId)
            val progress = parseProgress(prefs[key].orEmpty()).toMutableMap()
            progress[contentId] = (progress[contentId]?.first ?: 0.001f) to System.currentTimeMillis()
            prefs[key] = encodeProgress(progress)
        }
    }

    suspend fun removeFromHistory(contentId: String) {
        context.dataStore.edit { prefs ->
            val profileId = activeProfileId(prefs)
            if (profileId.isBlank()) return@edit
            val key = stringPreferencesKey(keyProgressPrefix + profileId)
            val progress = parseProgress(prefs[key].orEmpty()).toMutableMap()
            progress.remove(contentId)
            prefs[key] = encodeProgress(progress)
        }
    }

    suspend fun clearWatchHistory() {
        context.dataStore.edit { prefs ->
            val profileId = activeProfileId(prefs)
            if (profileId.isNotBlank()) prefs.remove(stringPreferencesKey(keyProgressPrefix + profileId))
        }
    }

    suspend fun setPlayerPreference(value: PlayerPreference) {
        context.dataStore.edit { it[keyPlayer] = value.name }
    }

    suspend fun setAndroidBoxVideoCompatibility(enabled: Boolean) {
        context.dataStore.edit { it[keyAndroidBoxVideoCompatibility] = enabled }
    }

    suspend fun setThemePreference(value: ThemePreference) {
        context.dataStore.edit { it[keyTheme] = value.name }
    }

    suspend fun setParentalMode(value: ParentalMode) {
        context.dataStore.edit { it[keyParentalMode] = value.name }
    }

    suspend fun setSubtitlePreference(value: SubtitlePreference) {
        context.dataStore.edit { it[keySubtitles] = value.name }
    }

    suspend fun setCatalogueLanguage(value: String) {
        context.dataStore.edit { it[keyLanguage] = value.take(40).ifBlank { "All" } }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[keyAnalytics] = enabled }
    }

    suspend fun acknowledgeDisclaimer() {
        context.dataStore.edit { it[keyDisclaimer] = true }
    }

    suspend fun markRefreshNow() {
        context.dataStore.edit { it[keyLastRefresh] = System.currentTimeMillis() }
    }

    private fun profilesFrom(
        raw: String?,
        prefs: androidx.datastore.preferences.core.Preferences
    ): List<PortalSettings> {
        val parsed = parseProfiles(raw)
        val profiles = if (parsed.isNotEmpty()) parsed else {
            val oldUrl = prefs[legacyUrl].orEmpty().trim()
            val oldMac = prefs[legacyMac].orEmpty().trim()
            if (oldUrl.isNotBlank() && oldMac.isNotBlank()) {
                listOf(
                    PortalSettings(
                        id = "legacy-${oldMac.replace(":", "").lowercase()}",
                        name = prefs[legacyName].orEmpty().ifBlank { "Portal" },
                        url = oldUrl,
                        mac = oldMac
                    )
                )
            } else emptyList()
        }
        if (!isAndroidTvDevice || profiles.isEmpty()) return profiles
        val sharedMac = prefs[keySharedDeviceMac]?.takeIf(::isValidMac)
            ?: profiles.firstOrNull()?.mac?.takeIf(::isValidMac)
            ?: generatedDeviceMac()
        return profiles.map { it.copy(mac = sharedMac) }
    }

    private fun defaultPortal(prefs: androidx.datastore.preferences.core.Preferences): PortalSettings {
        val mac = if (isAndroidTvDevice) {
            prefs[keySharedDeviceMac]?.takeIf(::isValidMac) ?: generatedDeviceMac()
        } else ""
        return PortalSettings(mac = mac)
    }

    private fun generatedDeviceMac(): String {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ).orEmpty()
        return generateStbPlayMac(androidId)
    }

    private fun isValidMac(value: String): Boolean =
        Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$").matches(value.trim().uppercase())

    private fun activeProfileId(prefs: androidx.datastore.preferences.core.Preferences): String {
        val profiles = profilesFrom(prefs[keyPortals], prefs)
        val requested = prefs[keyActivePortal].orEmpty()
        return profiles.firstOrNull { it.id == requested }?.id ?: profiles.firstOrNull()?.id.orEmpty()
    }

    /** Recover only a stored incomplete pairing draft; deleted profiles stay deleted. */
    suspend fun providerPairingDraft(id: String): PortalSettings? {
        if (id.isBlank()) return null
        val prefs = context.dataStore.data.first()
        return parseProfiles(prefs[keyPortals], includeDrafts = true)
            .firstOrNull { it.id == id && it.url.isBlank() }
            ?.copy(pin = prefs[keyParentalPin] ?: prefs[legacyPin].orEmpty())
    }

    private fun parseProfiles(raw: String?, includeDrafts: Boolean = false): List<PortalSettings> = runCatching {
        val array = JSONArray(raw.orEmpty())
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val url = item.optString("url").trim()
                val mac = item.optString("mac").trim().uppercase()
                if (id.isBlank() || (!includeDrafts && url.isBlank()) || mac.isBlank()) continue
                add(
                    PortalSettings(
                        id = id,
                        name = item.optString("name").trim().ifBlank { "Portal" },
                        url = url,
                        mac = mac
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun encodeProfiles(profiles: List<PortalSettings>): String = JSONArray().apply {
        profiles.forEach { profile ->
            put(
                JSONObject().apply {
                    put("id", profile.id)
                    put("name", profile.name)
                    put("url", profile.url)
                    put("mac", profile.mac)
                }
            )
        }
    }.toString()

    private fun parseProgress(raw: String): Map<String, Pair<Float, Long>> = raw
        .split(';')
        .asSequence()
        .mapNotNull { entry ->
            val parts = entry.split('|')
            if (parts.size < 3) return@mapNotNull null
            val fraction = parts[1].toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
            val timestamp = parts[2].toLongOrNull() ?: 0L
            parts[0].takeIf { it.isNotBlank() }?.let { it to (fraction to timestamp) }
        }
        .toMap()

    private fun encodeProgress(values: Map<String, Pair<Float, Long>>): String = values.entries
        .sortedByDescending { it.value.second }
        .take(100)
        .joinToString(";") { (id, value) -> "$id|${value.first}|${value.second}" }
}
