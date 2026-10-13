package com.example.stbplay.data

import com.example.stbplay.data.model.PortalStream
import java.util.Locale
import java.util.PriorityQueue

/** Small personal viewing summary, independent of unfinished resume positions. */
data class ViewingTaste(
    val id: String, val type: String, val categoryId: String?, val categoryTitle: String?,
    val genre: String?, val language: String?, val sessions: Int,
    val updatedAt: Long, val completed: Boolean = false
)

fun PortalStream.viewingTaste(now: Long, completed: Boolean = false) = ViewingTaste(
    id, streamType, categoryId, categoryTitle, genre, language, 1, now, completed
)

fun updateViewingTaste(old: List<ViewingTaste>, signal: ViewingTaste, newSession: Boolean): List<ViewingTaste> {
    val previous = old.firstOrNull { it.id == signal.id && it.type == signal.type }
    val updated = signal.copy(sessions = ((previous?.sessions ?: 0) + if (newSession) 1 else 0).coerceAtLeast(1),
        completed = signal.completed || previous?.completed == true)
    // This bounds stored preference summaries, never the provider catalogue or browsing.
    return (listOf(updated) + old.filterNot { it.id == signal.id && it.type == signal.type }).take(200)
}

/** Counts progress actually traversed; resume offsets, seeks and previews do not qualify. */
class ViewingSessionTracker {
    private var lastPosition: Long? = null
    private var lastTime: Long? = null
    private var watchedMs = 0L
    private var qualified = false
    private var completionRecorded = false
    fun observe(position: Long, duration: Long, elapsedMs: Long): Pair<Boolean, Boolean>? {
        val delta = lastPosition?.let { position - it } ?: 0L
        val wallDelta = lastTime?.let { elapsedMs - it } ?: 0L
        if (delta > 0 && wallDelta in 1..15_000 && delta <= wallDelta * 2 + 1_000)
            watchedMs += minOf(delta, wallDelta)
        lastPosition = position; lastTime = elapsedMs
        if (duration <= 0) return null
        val threshold = minOf(120_000L, maxOf(10_000L, duration / 5))
        val first = !qualified && watchedMs >= threshold
        if (first) qualified = true
        val finished = qualified && !completionRecorded && position.toDouble() / duration >= .95
        if (finished) completionRecorded = true
        return if (first || finished) first to finished else null
    }
}

private fun terms(value: String?): Set<String> = value.orEmpty().lowercase(Locale.ROOT)
    .split(',', ';', '|', '/').map { it.trim() }.filter { it.isNotBlank() }.toSet()

/** Linear scan off the UI thread; retains only a small ranking heap even for 100k titles. */
fun recommendHome(catalogue: List<PortalStream>, taste: List<ViewingTaste>, unfinished: Set<String>): List<PortalStream> {
    val categories = HashMap<String, Double>()
    val genres = HashMap<String, Double>()
    val languages = HashMap<String, Double>()
    fun category(type: String, id: String?) = id?.takeIf { it.isNotBlank() }?.let { "$type:$it" }
    taste.forEach { signal ->
        val weight = kotlin.math.sqrt(signal.sessions.coerceAtLeast(1).toDouble())
        category(signal.type, signal.categoryId)?.let { categories[it] = (categories[it] ?: 0.0) + weight }
        terms(signal.genre).forEach { genres[it] = (genres[it] ?: 0.0) + weight }
        terms(signal.language).forEach { languages[it] = (languages[it] ?: 0.0) + weight }
    }
    val recent = taste.sortedByDescending { it.updatedAt }.take(2)
    val completed = taste.filter { it.completed && it.type == "movie" }.mapTo(HashSet()) { "${it.type}:${it.id}" }
    data class Ranked(val stream: PortalStream, val score: Double, val order: Int)
    val bestFirst = compareByDescending<Ranked> { it.score }.thenBy { it.order }
    val heap = PriorityQueue(40, bestFirst.reversed())
    catalogue.forEachIndexed { index, stream ->
        if (stream.streamType == "live" || stream.id in unfinished || "${stream.streamType}:${stream.id}" in completed) return@forEachIndexed
        val streamGenres = terms(stream.genre)
        val streamLanguages = terms(stream.language)
        var score = 4 * (categories[category(stream.streamType, stream.categoryId)] ?: 0.0) +
            2 * streamGenres.sumOf { genres[it] ?: 0.0 } + streamLanguages.sumOf { languages[it] ?: 0.0 }
        recent.forEachIndexed { recentIndex, signal ->
            val bonus = if (recentIndex == 0) 3.0 else 2.0
            if (stream.categoryId != null && stream.categoryId == signal.categoryId && stream.streamType == signal.type) score += bonus * 2
            if (streamGenres.any { it in terms(signal.genre) }) score += bonus
            if (streamLanguages.any { it in terms(signal.language) }) score += bonus
        }
        val ranked = Ranked(stream, score, index)
        if (heap.size < 40) heap.add(ranked)
        else if (bestFirst.compare(ranked, heap.peek()) < 0) { heap.poll(); heap.add(ranked) }
    }
    val ranked = heap.sortedWith(bestFirst)
    val selected = ArrayList<PortalStream>(10)
    val counts = HashMap<String, Int>()
    ranked.forEach { candidate ->
        val key = category(candidate.stream.streamType, candidate.stream.categoryId)
        if (selected.size < 8 && (key == null || (counts[key] ?: 0) < 6)) {
            selected.add(candidate.stream); if (key != null) counts[key] = (counts[key] ?: 0) + 1
        }
    }
    // Reserve some room for other available categories without inventing provider metadata.
    val selectedKeys = selected.mapTo(HashSet()) { "${it.streamType}:${it.id}" }
    catalogue.asSequence().filter { it.streamType != "live" && it.id !in unfinished &&
        "${it.streamType}:${it.id}" !in completed && "${it.streamType}:${it.id}" !in selectedKeys &&
        category(it.streamType, it.categoryId)?.let { key -> key !in counts } == true }
        .take(2).forEach { selected.add(it); selectedKeys.add("${it.streamType}:${it.id}") }
    ranked.forEach { candidate ->
        if (selected.size < 10 && selectedKeys.add("${candidate.stream.streamType}:${candidate.stream.id}")) selected.add(candidate.stream)
    }
    return selected.take(10)
}
