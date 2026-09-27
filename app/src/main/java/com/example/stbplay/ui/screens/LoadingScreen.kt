@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stbplay.ui.QuestButton
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.example.stbplay.ui.questInitialFocus

private val LoadNavy = Color(0xFF070707)
private val LoadGold = Color(0xFFDDB32F)
private val LoadGoldLight = Color(0xFFFFD966)
private val LoadWhite = Color(0xFFF4F6FA)
private val LoadMuted = Color(0xFF9AA8B8)

@Composable
fun LoadingScreen(
    portalName: String,
    stage: String,
    progress: Float,
    error: String? = null,
    onRetry: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(LoadNavy), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(560.dp).padding(28.dp)
        ) {
            Text("STB PLAY", color = LoadGold, fontSize = 42.sp)
            Text("${portalName.ifBlank { "Portal" }} · Loading", color = LoadGoldLight, fontSize = 18.sp)
            if (error == null) {
                Text(stage, color = LoadWhite, fontSize = 18.sp)
                Box(Modifier.fillMaxWidth().height(7.dp).background(Color(0xFF252525))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(7.dp).background(LoadGold))
                }
                Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", color = LoadMuted, fontSize = 12.sp)
            } else {
                Text("Connection error", color = Color(0xFFFFA4A4), fontSize = 22.sp)
                Text(error, color = LoadMuted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuestButton(onClick = onRetry, modifier = Modifier.questInitialFocus(), colors = ButtonDefaults.colors(containerColor = LoadGold, contentColor = LoadNavy)) { Text("Retry") }
                    QuestButton(onClick = onEdit, colors = ButtonDefaults.colors(containerColor = Color(0xFF262626), contentColor = LoadWhite)) { Text("Edit portal") }
                }
            }
        }
    }
}
