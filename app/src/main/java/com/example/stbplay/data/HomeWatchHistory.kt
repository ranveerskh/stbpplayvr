package com.example.stbplay.data

import com.example.stbplay.data.model.PortalStream

data class HomeWatchItem(val stream: PortalStream, val progress: WatchProgress, val episode: SeriesHistoryEntry? = null)

/** Caller supplies only content allowed for the active viewer/mode. */
fun projectHomeWatchHistory(
    history: List<WatchProgress>,
    episodes: Map<String, SeriesHistoryEntry>,
    vod: Map<String, PortalStream>,
    live: Map<String, PortalStream>
): List<HomeWatchItem> {
    val seen = HashSet<String>()
    return history.sortedByDescending { it.updatedAt }.mapNotNull { progress ->
        val episode = episodes[progress.contentId]
        val stream = if (episode != null) vod[episode.seriesId]
            else vod[progress.contentId] ?: live[progress.contentId]
        stream ?: return@mapNotNull null
        if (progress.fraction <= 0f || (stream.streamType != "live" && progress.fraction >= 0.95f)) return@mapNotNull null
        if (!seen.add("${stream.streamType}:${stream.id}")) return@mapNotNull null
        HomeWatchItem(stream, progress, episode)
    }
}
