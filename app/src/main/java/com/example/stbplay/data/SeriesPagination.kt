package com.example.stbplay.data

/** Series paging is independent of catalogue/live limits. IDs tolerate changed row metadata. */
internal data class SeriesPage<T>(val items: List<T>, val total: Int? = null)

internal suspend fun <T> loadAllSeriesPages(
    key: (T) -> String,
    fetch: suspend (Int) -> SeriesPage<T>
): List<T> {
    // Current Stalker portals start at 1; page 0 is a legacy fallback only.
    for (start in listOf(1, 0)) {
        val result = LinkedHashMap<String, T>()
        var page = start
        var repeats = 0
        var total: Int? = null
        while (true) {
            val response = try { fetch(page) } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException || result.isNotEmpty()) throw error
                break // Unsupported endpoint/page origin; let the repository try its legacy route.
            }
            if (response.total != null) total = response.total
            if (response.items.isEmpty()) {
                break
            }
            var added = 0
            response.items.forEach { if (result.putIfAbsent(key(it), it) == null) added++ }
            // Totals can describe only the current page on some providers. The next
            // response, not a reported count, decides whether there are more episodes.
            repeats = if (added == 0) repeats + 1 else 0
            // A portal that ignores p repeats its response. Allow 0/1 aliases first.
            if (repeats >= 3) {
                break
            }
            page++
        }
        if (result.isNotEmpty()) {
            // A genuinely zero-based portal may return a non-empty second page for
            // p=1. Probe p=0 once and prepend only missing identities, not duplicates.
            if (start == 1) {
                val zero = try { fetch(0) } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    null
                }
                val first = LinkedHashMap<String, T>()
                zero?.items?.filter { key(it) !in result }?.forEach { first[key(it)] = it }
                first.putAll(result); result.clear(); result.putAll(first)
            }
            if (total != null && result.size < total!!)
                error("Provider returned an incomplete series list (${result.size}/$total). Please retry.")
            return result.values.toList()
        }
    }
    return emptyList()
}
