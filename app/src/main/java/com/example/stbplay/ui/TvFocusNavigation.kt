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
    val returnToRail: () -> Unit,
    val onFocused: (String) -> Unit
) {
    val targets = mutableStateMapOf<String, FocusRequester>()
    var entryId by mutableStateOf<String?>(null)
    var pending by mutableStateOf(true)
    var requestGeneration by mutableIntStateOf(0)
    var focusedId: String? = null

    fun enterPage() { pending = true; requestGeneration++ }
    fun focused(id: String) { focusedId = id; pending = false; onFocused(id) }
    fun detach(id: String, requester: FocusRequester) {
        if (targets[id] !== requester) return
        targets.remove(id)
        if (focusedId == id) { focusedId = null; pending = true }
    }
    fun setEntry(id: String?) {
        if (entryId == null && id != null) pending = true
        entryId = id
    }
    fun attach(id: String, requester: FocusRequester) {
        if (targets[id] !== requester) targets[id] = requester
    }
}

internal val LocalTvFocusNavigation = staticCompositionLocalOf<TvFocusNavigation?> { null }

@Composable
internal fun Modifier.tvFocusTarget(id: String): Modifier {
    val navigation = LocalTvFocusNavigation.current ?: return this
    val requester = remember(id, navigation) { FocusRequester() }
    DisposableEffect(id, requester, navigation) {
        onDispose { navigation.detach(id, requester) }
    }
    return this.focusRequester(requester)
        .onFocusChanged { if (it.isFocused) navigation.focused(id) }
        .onGloballyPositioned { navigation.attach(id, requester) }
        .testTag("tv-focus:$id")
}

@Composable
internal fun TvPageEntry(id: String?) {
    val navigation = LocalTvFocusNavigation.current ?: return
    SideEffect { navigation.setEntry(id) }
}
