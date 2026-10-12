package com.example.stbplay.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.stbplay.data.model.PortalStream

/** Kept by the app route owner while a result/details/player temporarily replaces the browser. */
class SearchSession {
    var open by mutableStateOf(false)
    var scope by mutableStateOf(StbPlayTab.HOME)
    var query by mutableStateOf("")
        private set
    internal var indexed by mutableStateOf<List<IndexedMedia>>(emptyList())
    internal var indexedCatalog: List<PortalStream>? = null
    var results by mutableStateOf<List<PortalStream>>(emptyList())
    var remotePage by mutableIntStateOf(1)
    var remoteHasMore by mutableStateOf(false)
    var searching by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)
    var completedRequest: String? = null
    var queryGeneration by mutableIntStateOf(0)
        private set
    var positionedGeneration = 0
    var returningToResult = false
    var selectedResultKey: String? = null
    val gridState = LazyGridState()

    fun show(section: StbPlayTab) {
        if (scope != section) {
            scope = section
            updateQuery("")
            results = emptyList()
            completedRequest = null
            selectedResultKey = null
            returningToResult = false
        }
        open = true
    }

    fun updateQuery(value: String) {
        val next = value.take(80)
        if (query == next) return
        query = next
        queryGeneration++
        remotePage = 1
        remoteHasMore = false
        results = emptyList()
        searching = false
        searchError = null
        completedRequest = null
        selectedResultKey = null
        returningToResult = false
    }
}
