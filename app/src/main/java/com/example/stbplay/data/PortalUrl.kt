package com.example.stbplay.data

import android.net.Uri

object PortalUrl {

    fun stalkerRoot(raw: String): String {
        var value = raw.trim()

        require(value.isNotEmpty()) {
            "Portal URL is empty"
        }

        if (!value.startsWith("http://", true) &&
            !value.startsWith("https://", true)
        ) {
            value = "http://$value"
        }

        value = value.trimEnd('/')
        val lower = value.lowercase()

        return when {
            lower.endsWith("/server/load.php") ->
                value.dropLast("/server/load.php".length)

            lower.endsWith("/portal.php") ->
                value.dropLast("/portal.php".length)

            lower.endsWith("/stalker_portal/c") ->
                value.dropLast("/c".length)

            lower.endsWith("/stalker_portal") ->
                value

            lower.endsWith("/c") ->
                value.dropLast("/c".length) + "/stalker_portal"

            else ->
                "$value/stalker_portal"
        }
    }

    fun apiUrl(raw: String): String {
        return "${stalkerRoot(raw)}/server/load.php"
    }

    fun refererUrl(raw: String): String {
        return "${stalkerRoot(raw)}/c/"
    }

    /**
     * Resolves absolute, protocol-relative, and portal-relative artwork paths.
     * Stalker providers frequently return paths such as ../misc/logos/... .
     */
    fun resolveArtworkUrl(raw: String?, portalUrl: String?): String? {
        val value = raw?.trim()?.replace("\\/", "/").orEmpty()
        if (value.isBlank() || value.equals("null", ignoreCase = true) || value == "0") return null

        val scheme = Uri.parse(value).scheme?.lowercase()
        if (scheme == "http" || scheme == "https") return value.replace(" ", "%20")
        if (scheme != null) return null

        val portal = portalUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val root = runCatching { stalkerRoot(portal) }.getOrNull() ?: return null
        val reference = when {
            value.startsWith("/stalker_portal/", ignoreCase = true) -> value
            value.startsWith("stalker_portal/", ignoreCase = true) -> "/$value"
            listOf("/misc/", "/images/", "/image/", "/logos/", "/media/").any { value.startsWith(it, ignoreCase = true) } ->
                "$root/${value.removePrefix("/")}"
            listOf("misc/", "images/", "image/", "logos/", "media/").any { value.startsWith(it, ignoreCase = true) } ->
                "$root/$value"
            else -> value
        }
        return runCatching {
            java.net.URL(java.net.URL(refererUrl(portal)), reference.replace(" ", "%20")).toExternalForm()
        }.getOrNull()
    }

    fun isPseudoStalkerCommand(raw: String): Boolean {
        val value = raw.trim()
        return value.startsWith("ffrt ", ignoreCase = true) ||
                value.startsWith("ffmpeg ", ignoreCase = true) ||
                value.contains("localhost/ch/", ignoreCase = true)
    }

    fun extractPlayableUrl(raw: String?): String? {
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
            '"', '\'', ')', ']', '}', ',', ';'
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

    fun cleanPlayableUrl(raw: String?): String? = extractPlayableUrl(raw)
}
