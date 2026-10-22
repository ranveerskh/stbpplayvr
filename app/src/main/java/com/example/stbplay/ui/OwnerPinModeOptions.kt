@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.stbplay.data.ViewerPinMode
import com.example.stbplay.isAndroidTvDevice

@Composable
internal fun OwnerPinModeOptions(mode: ViewerPinMode, onChanged: (ViewerPinMode) -> Unit, firstRequester: FocusRequester? = null) {
    val input = androidx.compose.ui.platform.LocalInputModeManager.current
    val television = androidx.compose.ui.platform.LocalContext.current.isAndroidTvDevice()
    LaunchedEffect(television) {
        if (television) input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
    }
    Text("When should we ask for your PIN?")
    ViewerPinMode.entries.forEach { option ->
        QuestButton(onClick = { onChanged(option) }, modifier = Modifier.then(if (option == ViewerPinMode.PROFILE_ENTRY && firstRequester != null) Modifier.focusRequester(firstRequester) else Modifier).fillMaxWidth().heightIn(min = 48.dp)) {
            Text((if (mode == option) "✓ " else "") + if (option == ViewerPinMode.PROFILE_ENTRY)
                "Every time I enter my profile" else "Only for restricted content")
        }
    }
}
