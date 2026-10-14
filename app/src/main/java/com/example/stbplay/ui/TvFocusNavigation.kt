package com.example.stbplay.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag

/** Only attached targets can receive a pending page-entry request. */
internal class TvFocusNavigation(
    private val railAction: () -> Unit,
    val onFocused: (String) -> Unit
) {
    val targets = mutableStateMapOf<String, FocusRequester>()
    var entryId by mutableStateOf<String?>(null)
    var pending by mutableStateOf(true)
    var requestGeneration by mutableIntStateOf(0)
    var focusedId: String? = null
    private var focusedRequester: FocusRequester? = null
    var railOwnsFocus by mutableStateOf(false)
    var lossGeneration by mutableIntStateOf(0)
    var hasPageFocus by mutableStateOf(false)
    var groupHasFocus by mutableStateOf(false)
    var lastFocusedId: String? = null
    private var restoringLostFocus by mutableStateOf(false)

    fun enterPage() { restoringLostFocus = false; railOwnsFocus = false; pending = true; requestGeneration++ }
    fun leavePage() { railOwnsFocus = true; pending = false }
    fun returnToRail() { leavePage(); railAction() }
    fun restorePage() {
        if (railOwnsFocus || pending) return
        restoringLostFocus = true
        pending = true; requestGeneration++
    }
    val restoreTarget: FocusRequester? get() =
        (if (restoringLostFocus) lastFocusedId?.let { targets[it] } else null) ?: entryId?.let { targets[it] }
    fun focused(id: String, requester: FocusRequester) {
        restoringLostFocus = false
        focusedId = id; lastFocusedId = id; focusedRequester = requester
        hasPageFocus = true; railOwnsFocus = false; pending = false; onFocused(id)
    }
    fun blurred(requester: FocusRequester) {
        if (focusedRequester === requester) {
            hasPageFocus = false; focusedRequester = null; lossGeneration++
        }
    }
    fun detach(id: String, requester: FocusRequester) {
        if (targets[id] === requester) targets.remove(id)
        // Lazy/page replacement can attach the new target with the same ID before
        // disposing the focused old one. Track ownership, not just the target map.
        if (focusedRequester === requester) {
            focusedId = null; focusedRequester = null; hasPageFocus = false
            if (!railOwnsFocus) pending = true
        }
    }
    fun setEntry(id: String?) {
        if (entryId == null && id != null) pending = true
        entryId = id
    }
    fun attach(id: String, requester: FocusRequester) {
        if (targets[id] !== requester) targets[id] = requester
    }
}

/** Intentional focus in the experimental portal selector is not page focus loss. */
internal val LocalPortalSwitcherFocused = staticCompositionLocalOf { false }

internal val LocalTvFocusNavigation = staticCompositionLocalOf<TvFocusNavigation?> { null }

@Composable
internal fun Modifier.tvFocusTarget(id: String): Modifier {
    val navigation = LocalTvFocusNavigation.current ?: return this
    val requester = remember(id, navigation) { FocusRequester() }
    DisposableEffect(id, requester, navigation) {
        onDispose { navigation.detach(id, requester) }
    }
    return this.focusRequester(requester)
        .onFocusChanged { if (it.isFocused) navigation.focused(id, requester) else navigation.blurred(requester) }
        .onGloballyPositioned { navigation.attach(id, requester) }
        .testTag("tv-focus:$id")
}

@Composable
internal fun TvPageEntry(id: String?) {
    val navigation = LocalTvFocusNavigation.current ?: return
    SideEffect { navigation.setEntry(id) }
}
