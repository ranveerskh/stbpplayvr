package com.example.stbplay.data

/** History is separate from favourites/watch progress; a query is committed on selection or submit. */
internal fun updatedSearchHistory(previous: List<String>, query: String): List<String> {
    val clean = query.trim().take(80)
    if (clean.length < 2) return previous
    return (listOf(clean) + previous.filterNot { it.equals(clean, ignoreCase = true) }).take(25)
}
