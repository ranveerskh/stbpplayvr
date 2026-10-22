@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.stbplay.data.ViewerPinMode

@Composable
internal fun OwnerPinModeOptions(mode: ViewerPinMode, onChanged: (ViewerPinMode) -> Unit) {
    Text("When should we ask for your PIN?")
    ViewerPinMode.entries.forEach { option ->
        QuestButton(onClick = { onChanged(option) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text((if (mode == option) "✓ " else "") + if (option == ViewerPinMode.PROFILE_ENTRY)
                "Every time I enter my profile" else "Only for restricted content")
        }
    }
}
