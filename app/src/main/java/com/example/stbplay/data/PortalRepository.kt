package com.example.stbplay.data

import com.example.stbplay.data.model.*
import com.example.stbplay.domain.model.PortalSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Single source of truth for portal calls. UI code only receives parsed domain
 * models, so a provider response/error can never crash a Compose screen.
 */
data class VodCatalogBatch(
    val items: List<PortalStream>,
    val nextPage: Int,
    val totalItems: Int?,
    val hasMore: Boolean
)

class PortalRepository {

    private val stalkerClient = StalkerPortalClient()
    private var currentSettings: PortalSettings? = null
    private var currentSession: StalkerSession? = null
    private var currentSubscription = PortalSubscription()
    private var liveCategoriesCache: List<PortalCategory> = emptyList()
    private var vodCategoriesCache: List<PortalCategory> = emptyList()
    private var seriesCategoriesCache: List<PortalCategory> = emptyList()

    suspend fun initialize(settings: PortalSettings): LoginResponse = withContext(Dispatchers.IO) {
        val cleanUrl = settings.url.trim()
        val cleanMac = settings.mac.trim()
        if (cleanUrl.isBlank()) return@withContext LoginResponse(false, "Enter a portal URL first.")
        if (cleanMac.isBlank()) return@withContext LoginResponse(false, "Enter the device MAC address first.")

        currentSettings = settings.copy(url = cleanUrl, mac = cleanMac.uppercase())
        currentSession = null

        try {
            val session = stalkerClient.handshake(cleanUrl, cleanMac)
            currentSession = session

            // MAG/Stalker portals commonly require these two calls before lists
            // become available. Their bodies are provider-specific, so no data
            // is assumed from them.
            val profile = stalkerClient.call(
                cleanUrl,
                cleanMac,
                session,
                "stb",
                "get_profile",
                mapOf(
                    "hd" to "1",
                    "stb_type" to "MAG250",
                    "image_version" to "218",
                    "auth_second_step" to "1",
                    "not_valid_token" to "0"
                )
            )
            currentSubscription = parseSubscription(profile)
            stalkerClient.call(cleanUrl, cleanMac, session, "stb", "get_localization")

            LoginResponse(success = true)
        } catch (error: Throwable) {
            currentSession = null
            val message = error.message.orEmpty()
            val errorMsg = when {
                message.contains("403", ignoreCase = true) ->
                    "Portal access denied. Check the activated MAC or portal access."
                message.contains("404", ignoreCase = true) ->
                    "Portal API endpoint not found. Check the portal address."
                message.contains("authentication", ignoreCase = true) ->
                    "Portal authentication failed for this MAC address."
                else -> message.ifBlank { "Connection failed. Check the portal and network." }
            }
            LoginResponse(success = false, errorMessage = errorMsg)
        }
    }

    suspend fun getLiveCategories(): List<PortalCategory> = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext emptyList()
        val settings = currentSettings ?: return@withContext emptyList()
        runCatching {
            val response = stalkerClient.call(settings.url, settings.mac, session, "itv", "get_genres")
            StalkerParser.parseGenres(response, "itv")
        }.getOrDefault(emptyList()).also { liveCategoriesCache = it }
    }

    suspend fun getLiveStreams(categoryId: String? = null): List<PortalStream> = withContext(Dispatchers.IO) {
        val settings = currentSettings ?: return@withContext emptyList()
        val session = currentSession ?: return@withContext emptyList()
        val params = buildMap {
            put("fav", "0")
            if (!categoryId.isNullOrBlank() && categoryId != "0") {
                // Different Stalker builds use either spelling. Sending both is
                // harmless and keeps category filtering compatible.
                put("genre", categoryId)
                put("category", categoryId)
            }
        }
        fetchWithPageFallback(settings, session, "itv", "get_all_channels", params, pageValues = listOf(null, "1", "0")) {
            StalkerParser.parseLiveChannels(it)
        }.withCategoryLocks(liveCategoriesCache)
    }

    suspend fun getVodCategories(): List<PortalCategory> = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext emptyList()
        val settings = currentSettings ?: return@withContext emptyList()
        runCatching {
            val response = stalkerClient.call(settings.url, settings.mac, session, "vod", "get_categories")
            StalkerParser.parseGenres(response, "vod")
        }.getOrDefault(emptyList()).also { vodCategoriesCache = it }
    }

    /** Read mixed VOD pages once, then classify each row using is_series. */
    suspend fun getVodCatalogBatch(
        categoryId: String? = null,
        startPage: Int = 0,
        maxPages: Int = 20
    ): VodCatalogBatch = withContext(Dispatchers.IO) {
        val settings = currentSettings ?: return@withContext VodCatalogBatch(emptyList(), startPage, null, false)
        val session = currentSession ?: return@withContext VodCatalogBatch(emptyList(), startPage, null, false)
        val items = mutableListOf<PortalStream>()
        val seen = mutableSetOf<String>()
        var page = startPage
        var total: Int? = null
        var hasMore = true
        val params = vodListParams(categoryId)

        for (index in 0 until maxPages) {
            val response = try {
                stalkerClient.call(settings.url, settings.mac, session, "vod", "get_ordered_list", params + ("p" to page.toString()))
            } catch (error: Exception) {
                if (items.isEmpty()) throw error
                break
            }
            val js = response.optJSONObject("js")
            val reportedTotal = js?.optInt("total_items", -1) ?: -1
            if (reportedTotal >= 0) total = reportedTotal
            val parsed = StalkerParser.parseVod(response, seriesOnly = null)
            if (parsed.isEmpty()) {
                hasMore = false
                return@withContext VodCatalogBatch(items, page, total, hasMore)
            }
            val categoryLocked = (vodCategoriesCache + seriesCategoriesCache).any { it.id == categoryId && it.isLocked }
            val newItems = parsed.filter { seen.add("${it.streamType}:${it.id}") }
                .withCategoryLocks(vodCategoriesCache + seriesCategoriesCache)
                .map { if (categoryLocked) it.copy(isLocked = true) else it }
            if (newItems.isEmpty()) {
                hasMore = false
                return@withContext VodCatalogBatch(items, page, total, hasMore)
            }
            items += newItems
            page++
            val pageSize = js?.optInt("max_page_items", parsed.size)?.takeIf { it > 0 } ?: parsed.size
            if (total != null && page * pageSize >= total!!) {
                hasMore = false
                return@withContext VodCatalogBatch(items, page, total, hasMore)
            }
            if (parsed.size < pageSize) {
                hasMore = false
                return@withContext VodCatalogBatch(items, page, total, hasMore)
            }
        }
        VodCatalogBatch(items, page, total, hasMore)
    }

    suspend fun getVodStreams(categoryId: String? = null): List<PortalStream> =
        getVodCatalogBatch(categoryId).items.filter { it.streamType == "movie" }

    suspend fun getSeriesCategories(): List<PortalCategory> = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext emptyList()
        val settings = currentSettings ?: return@withContext emptyList()

        // Newer middleware exposes series categories through `series`; older
        // installations expose the same categories through `vod`.
        val series = runCatching {
            StalkerParser.parseGenres(
                stalkerClient.call(settings.url, settings.mac, session, "series", "get_categories"),
                "series"
            )
        }.getOrDefault(emptyList())
        if (series.isNotEmpty()) {
            seriesCategoriesCache = series
            return@withContext series
        }

        runCatching {
            StalkerParser.parseGenres(
                stalkerClient.call(settings.url, settings.mac, session, "vod", "get_categories"),
                "series"
            )
        }.getOrDefault(emptyList()).also { seriesCategoriesCache = it }
    }

    suspend fun getSeriesStreams(categoryId: String? = null): List<PortalStream> =
        getVodCatalogBatch(categoryId).items.filter { it.streamType == "series" }

    suspend fun getSeasons(seriesId: String): List<PortalSeason> = withContext(Dispatchers.IO) {
        val settings = currentSettings ?: return@withContext emptyList()
        val session = currentSession ?: return@withContext emptyList()
        val params = mapOf(
            "movie_id" to seriesId,
            "season_id" to "0",
            "episode_id" to "0",
            "row" to "0",
            "category" to "*",
            "sortby" to "added"
        )
        val vodResult = fetchWithPageFallback(settings, session, "vod", "get_ordered_list", params) {
            StalkerParser.parseSeasons(it)
        }
        if (vodResult.isNotEmpty()) return@withContext vodResult

        val candidates = listOf(
            "get_seasons" to mapOf("series_id" to seriesId, "movie_id" to seriesId),
            "get_ordered_list" to mapOf("series_id" to seriesId, "movie_id" to seriesId, "category" to "*")
        )
        candidates.firstNotNullOfOrNull { (action, extra) ->
            runCatching {
                StalkerParser.parseSeasons(
                    stalkerClient.call(settings.url, settings.mac, session, "series", action, extra)
                ).takeIf { it.isNotEmpty() }
            }.getOrNull()
        }.orEmpty()
    }

    suspend fun getEpisodes(seriesId: String, seasonId: String, parentCommand: String? = null): List<PortalEpisode> = withContext(Dispatchers.IO) {
        val settings = currentSettings ?: return@withContext emptyList()
        val session = currentSession ?: return@withContext emptyList()
        val params = mapOf(
            "movie_id" to seriesId,
            "season_id" to seasonId,
            "episode_id" to "0",
            "row" to "0",
            "category" to "*",
            "sortby" to "added"
        )
        val vodResult = fetchWithPageFallback(settings, session, "vod", "get_ordered_list", params) {
            StalkerParser.parseEpisodes(it, parentCommand)
        }
        if (vodResult.isNotEmpty()) return@withContext vodResult

        val candidates = listOf(
            "get_episodes" to mapOf("series_id" to seriesId, "season_id" to seasonId),
            "get_ordered_list" to mapOf("series_id" to seriesId, "movie_id" to seriesId, "season_id" to seasonId, "category" to "*")
        )
        candidates.firstNotNullOfOrNull { (action, extra) ->
            runCatching {
                StalkerParser.parseEpisodes(
                    stalkerClient.call(settings.url, settings.mac, session, "series", action, extra),
                    parentCommand
                ).takeIf { it.isNotEmpty() }
            }.getOrNull()
        }.orEmpty()
    }

    suspend fun getStreamUrl(stream: PortalStream): String = withContext(Dispatchers.IO) {
        val settings = currentSettings ?: return@withContext ""
        val session = currentSession ?: return@withContext ""
        val command = stream.cmd?.trim().orEmpty()
        if (command.isBlank()) return@withContext ""

        runCatching {
            val resolver = StalkerPlaybackResolver(
                portalUiUrl = settings.url,
                macAddress = settings.mac,
                token = session.token,
                sessionCookie = session.cookie
            )
            when (stream.streamType) {
                "live" -> resolver.resolveLive(command)
                "series" -> resolver.resolveSeriesEpisode(command, stream.series.orEmpty())
                else -> resolver.resolveMovie(command, stream.series.orEmpty())
            }
        }.getOrDefault("")
    }

    suspend fun getEpisodeStreamUrl(episode: PortalEpisode): String = withContext(Dispatchers.IO) {
        val settings = currentSettings ?: return@withContext ""
        val session = currentSession ?: return@withContext ""
        val command = episode.cmd?.trim().orEmpty()
        if (command.isBlank()) return@withContext ""

        runCatching {
            StalkerPlaybackResolver(
                portalUiUrl = settings.url,
                macAddress = settings.mac,
                token = session.token,
                sessionCookie = session.cookie
            ).resolveSeriesEpisode(command, episode.series.orEmpty())
        }.getOrDefault("")
    }

    fun getHandshakeToken(): String? = currentSession?.token
    fun getSessionCookie(): String = currentSession?.cookie.orEmpty()
    fun getPortalReferer(): String = currentSettings?.url?.let(PortalUrl::refererUrl).orEmpty()
    fun getMacAddress(): String = currentSettings?.mac.orEmpty()
    fun getPortalName(): String = currentSettings?.name.orEmpty()
    fun getSubscription(): PortalSubscription = currentSubscription

    /**
     * Returns only qualities actually exposed by the portal. A single playable
     * command is labelled Auto rather than inventing a 4K/1080p label.
     */
    suspend fun getMovieQualityOptions(stream: PortalStream): List<PortalQualityOption> =
        withContext(Dispatchers.IO) {
            // MAG requests the movie's file rows before create_link. The catalogue
            // command identifies the title, while /media/file_<id>.mpg identifies
            // the selected file on this portal.
            val files = getVodFileOptions(stream, "0", "0", stream.series.orEmpty())
            if (files.isNotEmpty()) files
            else getQualityOptions(stream, stream.cmd, stream.series.orEmpty())
        }

    suspend fun getEpisodeQualityOptions(
        series: PortalStream,
        episode: PortalEpisode
    ): List<PortalQualityOption> = withContext(Dispatchers.IO) {
        val files = getVodFileOptions(
            series,
            episode.seasonId.orEmpty().ifBlank { "0" },
            episode.id,
            episode.series.orEmpty()
        )
        if (files.isNotEmpty()) return@withContext files

        val command = episode.cmd?.takeIf { it.isNotBlank() }
            ?: series.cmd?.takeIf { it.isNotBlank() }
            ?: return@withContext emptyList()
        listOf(PortalQualityOption(
            id = episode.id,
            label = "Auto",
            command = command,
            seriesValue = episode.series.orEmpty()
        ))
    }

    private suspend fun getVodFileOptions(
        stream: PortalStream,
        seasonId: String,
        episodeId: String,
        seriesValue: String
    ): List<PortalQualityOption> {
        val settings = currentSettings ?: return emptyList()
        val session = currentSession ?: return emptyList()
        val params = buildMap {
            put("movie_id", stream.id)
            put("season_id", seasonId)
            put("episode_id", episodeId)
            if (episodeId != "0") put("row", "0")
            put("fav", "0")
            put("sortby", "added")
            put("hd", "0")
            put("not_ended", "0")
            put("category", stream.categoryId?.takeIf { it.isNotBlank() && it != "0" } ?: "*")
            put("p", "0")
        }
        val response = runCatching {
            stalkerClient.call(settings.url, settings.mac, session, "vod", "get_ordered_list", params)
        }.getOrNull() ?: return emptyList()
        val js = response.opt("js")
        val rows = when (js) {
            is JSONArray -> js
            is JSONObject -> js.optJSONArray("data")
                ?: js.optJSONArray("items")
                ?: js.optJSONArray("rows")
            else -> null
        } ?: return emptyList()

        return buildList {
            for (index in 0 until rows.length()) {
                val file = rows.optJSONObject(index) ?: continue
                val isFile = file.opt("is_file")?.toString()?.lowercase()
                if (isFile !in setOf("1", "true")) continue
                val fileId = file.optString("id").trim()
                if (fileId.isEmpty() || !fileId.all(Char::isDigit)) continue
                val command = "/media/file_${fileId}.mpg"
                add(PortalQualityOption(
                    id = fileId,
                    label = normaliseQualityLabel(file.optString("quality")),
                    command = command,
                    seriesValue = seriesValue
                ))
            }
        }.distinctBy { it.command }
    }

    private suspend fun getQualityOptions(
        stream: PortalStream,
        fallbackCommand: String?,
        seriesValue: String
    ): List<PortalQualityOption> = withContext(Dispatchers.IO) {
        val settings = currentSettings ?: return@withContext emptyList()
        val session = currentSession ?: return@withContext emptyList()
        val rawOptions = mutableListOf<Pair<String, String>>()
        runCatching {
            val info = stalkerClient.call(
                settings.url,
                settings.mac,
                session,
                "vod",
                "get_vod_info",
                mapOf("movie_id" to stream.id, "vod_id" to stream.id, "series_id" to stream.id)
            )
            collectQualityOptions(info, rawOptions)
        }
        fallbackCommand?.takeIf { it.isNotBlank() }?.let { rawOptions += "Auto" to it }
        rawOptions
            .map { (label, command) ->
                PortalQualityOption(
                    id = "$label|$command".hashCode().toString(),
                    label = normaliseQualityLabel(label),
                    command = command,
                    seriesValue = seriesValue
                )
            }
            .distinctBy { it.command }
    }

    private fun List<PortalStream>.withCategoryLocks(categories: List<PortalCategory>): List<PortalStream> {
        if (isEmpty() || categories.isEmpty()) return this
        val locks = categories.associate { it.id to it.isLocked }
        return map { stream ->
            stream.copy(
                isLocked = stream.isLocked || locks[stream.categoryId] == true ||
                    ContentSafety.isRestricted(stream.name, stream.rating, stream.categoryTitle)
            )
        }
    }

    private fun collectQualityOptions(
        node: Any?,
        output: MutableList<Pair<String, String>>,
        inheritedLabel: String = "",
        depth: Int = 0
    ) {
        if (node == null || node == JSONObject.NULL || depth > 10) return
        when (node) {
            is JSONArray -> for (index in 0 until node.length()) {
                collectQualityOptions(node.opt(index), output, inheritedLabel, depth + 1)
            }
            is JSONObject -> {
                val label = listOf("label", "quality", "resolution", "title", "name", "height")
                    .asSequence()
                    .map { node.optString(it).trim() }
                    .firstOrNull { it.isNotBlank() }
                    ?: inheritedLabel
                val command = listOf("cmd", "command", "url", "stream", "stream_url", "play_url")
                    .asSequence()
                    .map { node.optString(it).trim() }
                    .firstOrNull { it.isPotentialPortalCommand() }
                if (command != null) output += (label.ifBlank { "Auto" } to command)
                val iterator = node.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    collectQualityOptions(node.opt(key), output, label, depth + 1)
                }
            }
            is String -> if (node.isPotentialPortalCommand()) output += (inheritedLabel.ifBlank { "Auto" } to node)
        }
    }

    private fun String.isPotentialPortalCommand(): Boolean {
        val value = trim()
        return value.startsWith("http://", true) || value.startsWith("https://", true) ||
            value.startsWith("ffmpeg ", true) || value.startsWith("ffrt ", true) ||
            value.startsWith("/media/", true) || value.contains("localhost/ch/", true)
    }

    private fun normaliseQualityLabel(raw: String): String {
        val value = raw.trim().lowercase()
        return when {
            value.contains("2160") || value.contains("4k") -> "4K"
            value.contains("1080") -> "1080p"
            value.contains("720") -> "720p"
            value.contains("480") -> "480p"
            value.contains("360") -> "360p"
            else -> "Auto"
        }
    }

    private fun parseSubscription(profile: JSONObject): PortalSubscription {
        val root = profile.opt("js")
        val plan = profileValue(root, setOf("tariff_plan", "tariff", "plan", "package", "subscription_name"))
            .orEmpty().ifBlank { "Subscription" }.take(80)
        val status = profileValue(root, setOf("status", "state", "account_status")).orEmpty().take(40)
        val expiryRaw = profileValue(
            root,
            setOf("end_date", "expire_date", "expire_billing_date", "expiration", "expires_at", "expiry_date", "valid_until", "endDate", "expireDate")
        ).orEmpty()
        val unlimited = expiryRaw.trim().lowercase() in setOf("", "0", "none", "never", "unlimited", "infinite")
        return PortalSubscription(plan, status, if (unlimited) null else parseDateEpoch(expiryRaw), unlimited)
    }

    private fun profileValue(node: Any?, keys: Set<String>, depth: Int = 0): String? {
        if (node == null || node == JSONObject.NULL || depth > 7) return null
        return when (node) {
            is JSONObject -> {
                val iterator = node.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val value = node.opt(key)
                    if (key in keys && value != null && value != JSONObject.NULL) {
                        val text = value.toString().trim()
                        if (text.isNotBlank()) return text
                    }
                    profileValue(value, keys, depth + 1)?.let { return it }
                }
                null
            }
            is JSONArray -> {
                for (index in 0 until node.length()) {
                    profileValue(node.opt(index), keys, depth + 1)?.let { return it }
                }
                null
            }
            else -> null
        }
    }

    private fun parseDateEpoch(raw: String): Long? {
        val value = raw.trim()
        value.toLongOrNull()?.let { numeric ->
            return if (numeric < 10_000_000_000L) numeric * 1_000L else numeric
        }
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
    }

    private fun vodListParams(categoryId: String?): Map<String, String> = buildMap {
        put("movie_id", "0")
        put("season_id", "0")
        put("episode_id", "0")
        put("row", "0")
        put("fav", "0")
        put("sortby", "added")
        put("hd", "0")
        put("not_ended", "0")
        put("category", categoryId?.takeIf { it.isNotBlank() && it != "0" } ?: "*")
    }

    private suspend fun <T> fetchWithPageFallback(
        settings: PortalSettings,
        session: StalkerSession,
        type: String,
        action: String,
        parameters: Map<String, String>,
        pageValues: List<String?> = listOf("0", "1"),
        parser: (JSONObject) -> List<T>
    ): List<T> {
        // Captured Stalker clients start at page 1. A few older builds start at
        // page 0, and some channel endpoints ignore paging entirely. Walk pages
        // until the response is empty or starts repeating, with a safety cap.
        for (pageStart in pageValues) {
            if (pageStart == null) {
                val response = runCatching {
                    stalkerClient.call(settings.url, settings.mac, session, type, action, parameters)
                }.getOrNull() ?: continue
                val parsed = runCatching { parser(response) }.getOrDefault(emptyList())
                if (parsed.isNotEmpty()) return parsed
                continue
            }

            val firstPage = pageStart.toIntOrNull() ?: 1
            val result = mutableListOf<T>()
            val seen = mutableSetOf<T>()
            for (page in firstPage..(firstPage + 19)) {
                val response = runCatching {
                    stalkerClient.call(
                        settings.url,
                        settings.mac,
                        session,
                        type,
                        action,
                        parameters + ("p" to page.toString())
                    )
                }.getOrNull() ?: break
                val parsed = runCatching { parser(response) }.getOrDefault(emptyList())
                if (parsed.isEmpty()) break
                val newItems = parsed.filter { seen.add(it) }
                if (newItems.isEmpty()) break
                result += newItems
            }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }
}
