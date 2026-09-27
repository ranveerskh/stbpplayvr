package com.example.stbplay.data

import com.example.stbplay.data.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Defensive parser for the different Stalker middleware response shapes.
 * Providers often return the same list as either `js: []` or `js: {data: []}`
 * and may use empty strings/JSON null for optional values.
 */
object StalkerParser {

    fun parseLiveChannels(response: JSONObject): List<PortalStream> {
        return dataArray(response).asSequence()
            .mapNotNull { item ->
                val id = item.text("id", "channel_id") ?: return@mapNotNull null
                val name = item.text("name", "title", "channel_name") ?: "Channel $id"
                PortalStream(
                    id = id,
                    name = name,
                    iconUrl = item.text("logo", "icon", "thumbnail", "screenshot_uri"),
                    categoryId = item.text("tv_genre_id", "genre_id", "category_id"),
                    streamType = "live",
                    number = item.text("number", "channel_number")?.toIntOrNull(),
                    isLocked = item.bool("lock", "censored", "adult", "is_adult") ||
                        ContentSafety.isRestricted(name, item.text("group_title", "genre", "category_name")),
                    cmd = item.text("cmd", "command"),
                    description = item.text("description", "about"),
                    year = item.text("year")?.toIntOrNull(),
                    searchText = item.searchText(),
                    originalTitle = item.text("original_name", "o_name", "alternative_name", "alt_name"),
                    language = item.text("language", "lang", "audio_language", "audio_lang"),
                    genre = item.text("genre", "genre_name", "category_name"),
                    rating = item.text("rating_imdb", "rating", "kinopoisk_rating"),
                    cast = item.text("actors", "cast"),
                    categoryTitle = item.text("category_title", "category_name", "group_title"),
                    releaseDate = item.text("date_add", "release_date", "date")
                )
            }
            .distinctBy { it.id }
            .toList()
    }

    fun parseGenres(response: JSONObject, type: String): List<PortalCategory> {
        return dataArray(response).asSequence()
            .mapNotNull { item ->
                val id = item.text("id", "category_id") ?: return@mapNotNull null
                val name = item.text("title", "name", "category_name") ?: return@mapNotNull null
                if (id == "0" || name.equals("all", ignoreCase = true)) return@mapNotNull null
                PortalCategory(
                    id = id,
                    name = name,
                    type = type,
                    isLocked = item.bool("lock", "locked", "adult", "is_adult", "censored") ||
                        ContentSafety.isRestricted(name)
                )
            }
            .distinctBy { it.id }
            .toList()
    }

    /**
     * Parses both movies and series. `seriesOnly` is deliberately optional:
     * older portals omit `is_series`, in which case the caller still receives
     * the catalogue instead of an empty screen.
     */
    fun parseVod(
        response: JSONObject,
        streamType: String = "movie",
        seriesOnly: Boolean? = null
    ): List<PortalStream> {
        val items = dataArray(response)
        val hasSeriesFlag = items.any {
            it.text("is_series", "content_type", "series") != null
        }

        return items.asSequence()
            .mapNotNull { item ->
                val id = item.text("id", "movie_id", "video_id") ?: return@mapNotNull null
                val name = item.text("name", "title", "movie_name") ?: "Untitled"
                val isSeries = item.isSeriesFlag()

                if (seriesOnly == true && hasSeriesFlag && !isSeries) return@mapNotNull null
                if (seriesOnly == false && hasSeriesFlag && isSeries) return@mapNotNull null

                PortalStream(
                    id = id,
                    name = name,
                    iconUrl = item.text(
                        "screenshot_uri",
                        "screenshot",
                        "cover",
                        "poster",
                        "logo",
                        "icon",
                        "thumbnail"
                    ),
                    categoryId = item.text("category_id", "category", "genre_id"),
                    streamType = if (seriesOnly == null) (if (isSeries) "series" else "movie") else streamType,
                    number = item.text("number", "position")?.toIntOrNull(),
                    isLocked = item.bool("lock", "censored", "adult", "is_adult", "age_restriction") ||
                        ContentSafety.isRestricted(name, item.text("rating", "rating_imdb", "kinopoisk_rating")),
                    cmd = item.text("cmd", "command", "url"),
                    // Stalker catalogue "series" is often a JSON array of episode numbers.
                    // It must never be sent back as the create_link series parameter.
                    series = item.text("series_cmd", "series_command")
                        ?: (item.opt("series") as? String)?.trim()?.takeIf { it.isNotBlank() && it != "0" },
                    description = item.text("description", "plot", "about", "short_description"),
                    year = item.text("year", "release_year")?.toIntOrNull(),
                    searchText = item.searchText(),
                    originalTitle = item.text("original_name", "o_name", "alternative_name", "alt_name", "old_name"),
                    language = item.text("language", "lang", "audio_language", "audio_lang"),
                    genre = item.text("genre", "genre_name", "category_name"),
                    rating = item.text("rating_imdb", "rating", "kinopoisk_rating"),
                    cast = item.text("actors", "cast"),
                    categoryTitle = item.text("category_title", "category_name"),
                    releaseDate = item.text("date_add", "release_date", "date")
                )
            }
            .distinctBy { it.id }
            .toList()
    }

    fun parseSeasons(response: JSONObject): List<PortalSeason> {
        return dataArray(response).asSequence()
            .mapNotNull { item ->
                if (item.has("is_season") && !item.bool("is_season")) return@mapNotNull null
                val id = item.text("id", "season_id") ?: return@mapNotNull null
                val number = item.text("number", "season_number")?.toIntOrNull()
                    ?: item.optInt("season", 0).takeIf { it > 0 }
                    ?: 1
                PortalSeason(
                    id = id,
                    name = item.text("name", "title") ?: "Season $number",
                    number = number
                )
            }
            .distinctBy { it.id }
            .sortedBy { it.number }
            .toList()
    }

    fun parseEpisodes(response: JSONObject, parentCommand: String? = null): List<PortalEpisode> {
        return dataArray(response).asSequence()
            .mapNotNull { item ->
                if (item.has("is_episode") && !item.bool("is_episode")) return@mapNotNull null
                val id = item.text("id", "episode_id") ?: return@mapNotNull null
                PortalEpisode(
                    id = id,
                    name = item.text("name", "title", "episode_name") ?: "Episode $id",
                    // Stalker episode rows omit cmd. Playback uses the parent series cmd
                    // and passes this episode's series_number to create_link.
                    cmd = item.text("cmd", "command", "url") ?: parentCommand,
                    seasonId = item.text("season_id", "season"),
                    series = item.text("series_number", "series_cmd", "series_command")
                        ?: item.optJSONArray("series")?.optString(0)?.takeIf { it.isNotBlank() },
                    description = item.text("description", "plot", "about")
                )
            }
            .distinctBy { it.id }
            .toList()
    }

    private fun dataArray(response: JSONObject): List<JSONObject> {
        val js = response.opt("js") ?: return emptyList()
        val array = when (js) {
            is JSONArray -> js
            is JSONObject -> js.optJSONArray("data")
                ?: js.optJSONArray("items")
                ?: js.optJSONArray("rows")
                ?: js.optJSONArray("results")
                ?: if (js.has("id")) JSONArray().put(js) else null
            else -> null
        } ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let(::add)
            }
        }
    }

    private fun JSONObject.text(vararg keys: String): String? {
        keys.forEach { key ->
            val raw = opt(key)
            val value = when (raw) {
                null, JSONObject.NULL -> null
                is String -> raw.trim()
                else -> raw.toString().trim()
            }
            if (!value.isNullOrBlank() && !value.equals("null", ignoreCase = true)) return value
        }
        return null
    }

    private fun JSONObject.bool(vararg keys: String): Boolean {
        keys.forEach { key ->
            val raw = opt(key) ?: return@forEach
            when (raw) {
                is Boolean -> if (raw) return true
                is Number -> if (raw.toInt() != 0) return true
                else -> if (raw.toString().trim().lowercase() in setOf("1", "true", "yes", "adult", "locked")) return true
            }
        }
        return false
    }

    private fun JSONObject.isSeriesFlag(): Boolean {
        val explicit = opt("is_series")
        if (explicit != null && explicit != JSONObject.NULL) {
            return when (explicit) {
                is Boolean -> explicit
                is Number -> explicit.toInt() != 0
                else -> explicit.toString().trim().lowercase() in setOf("1", "true", "yes", "series", "tvshow")
            }
        }

        val contentType = opt("content_type")
        if (contentType != null && contentType != JSONObject.NULL) {
            return contentType.toString().trim().lowercase() in setOf("series", "tvshow", "show")
        }

        val seriesValue = opt("series") ?: return false
        return when (seriesValue) {
            is Boolean -> seriesValue
            is Number -> seriesValue.toInt() != 0
            is JSONArray -> seriesValue.length() > 0
            else -> seriesValue.toString().trim().lowercase().let { value ->
                value in setOf("1", "true", "yes", "series", "tvshow") ||
                    (value.isNotBlank() && value !in setOf("0", "false", "no", "none"))
            }
        }
    }

    private fun JSONObject.searchText(): String? {
        val values = listOfNotNull(
            text("original_name", "o_name", "alternative_name", "alt_name"),
            text("genre", "genre_name", "category_name"),
            text("language", "lang"),
            text("year", "release_year")
        )
        return values.joinToString(" ").takeIf { it.isNotBlank() }
    }
}
