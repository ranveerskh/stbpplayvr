@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.example.stbplay.data.UpdateInfo
import com.example.stbplay.ui.QuestButton
import com.example.stbplay.ui.questInitialFocus
import com.example.stbplay.ui.theme.LocalStbPalette

@Composable
fun UpdateNotice(
    info: UpdateInfo,
    required: Boolean,
    status: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = LocalStbPalette.current
    val content: @Composable () -> Unit = {
        Column(
            Modifier.widthIn(max = 580.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(colors.panel).padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(if (required) "Update required" else "STB Play update available", color = colors.text, fontSize = 25.sp)
            Text("Version ${info.version} · Update by ${info.deadlineText()}", color = colors.accentLight, fontSize = 16.sp)
            if (required) Text("The 14-day update period has ended. Install the new version to continue.", color = colors.text, fontSize = 14.sp)
            if (info.notes.isNotBlank()) Text(info.notes, color = colors.muted, fontSize = 13.sp)
            Text(status, color = colors.muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuestButton(onClick = onUpdate, modifier = Modifier.questInitialFocus(),
                    colors = ButtonDefaults.colors(containerColor = colors.accent, contentColor = colors.onAccent)) {
                    Text("Download & install")
                }
                if (!required) QuestButton(onClick = onLater, colors = ButtonDefaults.colors(containerColor = colors.panelSoft, contentColor = colors.text)) {
                    Text("Later")
                }
                if (required) QuestButton(onClick = onRetry, colors = ButtonDefaults.colors(containerColor = colors.panelSoft, contentColor = colors.text)) {
                    Text("Check again")
                }
            }
        }
    }
    if (required) {
        BackHandler { }
        Box(Modifier.fillMaxSize().background(colors.background).padding(20.dp), contentAlignment = Alignment.Center) { content() }
    } else {
        Dialog(onDismissRequest = onLater) { content() }
    }
}
