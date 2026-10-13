package com.example.stbplay.data.model

/** Provider episode numbers are not display positions or database IDs. */
fun episodeDisplayLabel(episode: PortalEpisode): String {
    val number = episode.number ?: return episode.name
    return if (Regex("(?i)^(episode|ep\\.?)[\\s#:_-]*\\d+$").matches(episode.name.trim()))
        "Episode $number" else "$number. ${episode.name}"
}
