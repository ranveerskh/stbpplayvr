package com.example.stbplay.data

import com.example.stbplay.data.model.PortalStream

/** Keep source precedence and typed IDs without copying the whole catalogue on each click. */
internal fun findCatalogStream(
    id: String,
    streamType: String,
    live: List<PortalStream>,
    movies: List<PortalStream>,
    series: List<PortalStream>,
    remoteSearch: List<PortalStream>,
    localFavorites: List<PortalStream>,
    categoryItems: Sequence<PortalStream>
): PortalStream? {
    fun List<PortalStream>.matching() = firstOrNull { it.id == id && it.streamType == streamType }
    return live.matching()
        ?: movies.matching()
        ?: series.matching()
        ?: remoteSearch.matching()
        ?: localFavorites.matching()
        ?: categoryItems.firstOrNull { it.id == id && it.streamType == streamType }
}
