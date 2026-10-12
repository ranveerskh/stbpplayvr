package com.example.stbplay.data

import org.junit.Assert.*
import org.junit.Test

class SearchHistoryTest {
    @Test fun selectingAnExistingQueryMovesItToTheFrontWithoutDuplicates() {
        assertEquals(listOf("News", "Film"), updatedSearchHistory(listOf("Film", "news"), " News "))
    }
    @Test fun incompleteQueriesDoNotReplaceHistory() {
        assertEquals(listOf("Film"), updatedSearchHistory(listOf("Film"), " x "))
    }
    @Test fun historyRetentionDoesNotGrowWithEverySelection() {
        val history = (1..100).fold(emptyList<String>()) { entries, n -> updatedSearchHistory(entries, "Query $n") }
        assertEquals(25, history.size)
        assertEquals("Query 100", history.first())
        assertEquals("Query 76", history.last())
    }
}
