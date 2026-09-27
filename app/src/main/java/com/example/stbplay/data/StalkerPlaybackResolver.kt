package com.example.stbplay.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class StalkerContentKind {
    LIVE,
    MOVIE,
    SERIES_EPISODE
}

data class StalkerPlayRequest(
    val command: String?,
    val kind: StalkerContentKind,
    val seriesValue: String = "",
    val contentId: String = "",
    val title: String = "",
    val resumeFraction: Float = 0f
)

class StalkerPlaybackResolver(
    portalUiUrl: String,
    private val macAddress: String,
    private val token: String?,
    private val sessionCookie: String = ""
) {
    private val uiUrl = portalUiUrl.trim().let {
        if (it.endsWith("/")) it else "$it/"
    }

    private val apiUrl = buildApiUrl(uiUrl)

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun resolve(request: StalkerPlayRequest): String =
        withContext(Dispatchers.IO) {
            val command = request.command?.trim().orEmpty()

            if (command.isBlank()) {
                throw IllegalStateException(
                    "This item has no playable command"
                )
            }

            // Direct URL only when it is not a Stalker command.
            if (!isPseudoCommand(command)) {
                extractPlayableUrl(command)?.let {
                    return@withContext it
                }
            }

            val type = when (request.kind) {
                StalkerContentKind.LIVE -> "itv"
                StalkerContentKind.MOVIE,
                StalkerContentKind.SERIES_EPISODE -> "vod"
            }

            val forcedStorage =
                if (request.kind == StalkerContentKind.LIVE) {
                    "0"
                } else {
                    ""
                }

            val resolved = requestCreateLink(
                type = type,
                command = command,
                seriesValue = request.seriesValue,
                forcedStorage = forcedStorage
            )

            val returnedValue = extractReturnedValue(resolved.body)

            val playableUrl =
                extractPlayableUrl(returnedValue)
                    ?: extractPlayableUrl(resolved.finalUrl)

            playableUrl
                ?: throw IllegalStateException(
                    "Portal did not return a playable stream URL"
                )
        }

    // Existing live code can continue calling this.
    suspend fun resolveLive(command: String): String {
        return resolve(
            StalkerPlayRequest(
                command = command,
                kind = StalkerContentKind.LIVE
            )
        )
    }

    suspend fun resolveMovie(
        command: String,
        seriesValue: String = ""
    ): String {
        return resolve(
            StalkerPlayRequest(
                command = command,
                kind = StalkerContentKind.MOVIE,
                seriesValue = seriesValue
            )
        )
    }

    suspend fun resolveSeriesEpisode(
        command: String?,
        seriesValue: String = ""
    ): String {
        return resolve(
            StalkerPlayRequest(
                command = command,
                kind = StalkerContentKind.SERIES_EPISODE,
                seriesValue = seriesValue
            )
        )
    }

    private fun requestCreateLink(
        type: String,
        command: String,
        seriesValue: String,
        forcedStorage: String
    ): LinkResponse {
        val requestUrl = apiUrl.toHttpUrl()
            .newBuilder()
            .addQueryParameter("type", type)
            .addQueryParameter("action", "create_link")
            .addQueryParameter("cmd", command)
            .addQueryParameter(
                "series",
                if (type == "itv") "" else seriesValue
            )
            .addQueryParameter(
                "forced_storage",
                forcedStorage
            )
            .addQueryParameter("disable_ad", "0")
            .addQueryParameter("download", "0")
            .addQueryParameter("force_ch_link_check", "0")
            .addQueryParameter("JsHttpRequest", "1-xml")
            .build()
            .toString()

        val builder = Request.Builder()
            .url(requestUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 MAG254"
            )
            .header(
                "X-User-Agent",
                "Model: MAG254; Link: Ethernet"
            )
            .header("Referer", uiUrl)
            .header("Accept", "*/*")
            .header("Cookie", buildCookieHeader())

        if (!token.isNullOrBlank()) {
            builder.header(
                "Authorization",
                "Bearer $token"
            )
        }

        httpClient.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException(
                    "create_link HTTP ${response.code}"
                )
            }

            return LinkResponse(
                body = body,
                finalUrl = response.request.url.toString()
            )
        }
    }

    private fun buildCookieHeader(): String {
        val cookies = sessionCookie
            .split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()

        fun hasCookie(name: String): Boolean {
            return cookies.any {
                it.startsWith("$name=", ignoreCase = true)
            }
        }

        if (!hasCookie("mac")) {
            cookies.add("mac=$macAddress")
        }

        if (!hasCookie("stb_lang")) {
            cookies.add("stb_lang=en")
        }

        if (!hasCookie("timezone")) {
            cookies.add("timezone=America/Toronto")
        }

        if (!hasCookie("token") && !token.isNullOrBlank()) {
            cookies.add("token=$token")
        }

        return cookies.joinToString("; ")
    }

    private fun extractReturnedValue(
        body: String
    ): String? {
        if (body.isBlank()) return null

        val json = runCatching {
            JSONObject(body)
        }.getOrNull() ?: return body

        return extractFromJson(json)
    }

    private fun extractFromJson(
        json: JSONObject
    ): String? {
        val directKeys = arrayOf(
            "cmd",
            "url",
            "stream_url",
            "play_url"
        )

        for (key in directKeys) {
            val value = json.optString(key)
            if (value.isNotBlank()) return value
        }

        val js = json.opt("js")

        when (js) {
            is JSONObject -> {
                val nested = extractFromJson(js)
                if (!nested.isNullOrBlank()) return nested
            }

            is JSONArray -> {
                for (index in 0 until js.length()) {
                    val item = js.opt(index)

                    if (item is JSONObject) {
                        val nested = extractFromJson(item)
                        if (!nested.isNullOrBlank()) {
                            return nested
                        }
                    }

                    if (item is String && item.isNotBlank()) {
                        return item
                    }
                }
            }

            is String -> {
                val value = js.trim()

                if (value.startsWith("{")) {
                    return extractReturnedValue(value)
                }

                if (value.isNotBlank()) return value
            }
        }

        return null
    }

    private fun isPseudoCommand(
        command: String
    ): Boolean {
        val value = command.trim()

        return value.startsWith(
            "ffrt ",
            ignoreCase = true
        ) ||
                value.startsWith(
                    "ffmpeg ",
                    ignoreCase = true
                ) ||
                value.contains(
                    "localhost/ch/",
                    ignoreCase = true
                )
    }

    private fun extractPlayableUrl(
        raw: String?
    ): String? {
        val value = raw
            ?.trim()
            ?.replace("\\/", "/")
            .orEmpty()

        if (value.isBlank()) return null

        val match = Regex(
            """https?://[^\s|]+""",
            RegexOption.IGNORE_CASE
        ).find(value) ?: return null

        val url = match.value.trimEnd(
            '"',
            '\'',
            ')',
            ']',
            '}',
            ',',
            ';'
        )

        val host = Uri.parse(url)
            .host
            ?.lowercase()
            .orEmpty()

        if (
            host == "localhost" ||
            host == "127.0.0.1" ||
            host == "0.0.0.0"
        ) {
            return null
        }

        return url
    }

    private fun buildApiUrl(
        input: String
    ): String {
        val clean = input.trim().trimEnd('/')

        return when {
            clean.endsWith("/server/load.php") -> clean

            clean.endsWith("/c") ->
                clean.removeSuffix("/c") +
                        "/server/load.php"

            clean.endsWith("/portal.php") ->
                clean.removeSuffix("/portal.php") +
                        "/server/load.php"

            else ->
                "$clean/server/load.php"
        }
    }

    data class LinkResponse(
        val body: String,
        val finalUrl: String
    )
}
