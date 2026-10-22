@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.example.stbplay.data.*
import com.example.stbplay.ui.theme.LocalStbPalette
import kotlinx.coroutines.launch

@Composable
internal fun GlassAppearanceSettings(adultOnly: Boolean, normalGlass: Boolean) {
    val context = LocalContext.current
    val manager = remember(context) { SettingsManager(context.applicationContext) }
    val appearance by manager.glassAppearance.collectAsState(initial = GlassAppearance())
    val scope = rememberCoroutineScope()
    val p = LocalStbPalette.current
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true; message = null
            try {
                val copy = importCustomBackground(context, uri)
                manager.setCustomGlassBackground(copy.absolutePath, adultOnly)
                message = "Background saved on this device."
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message = error.message ?: "Could not import this image. Your background is unchanged."
            } finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (adultOnly) {
            Text("Adult themes", color = p.text)
            AdultThemePreference.entries.forEach { theme ->
                val name = if (theme == AdultThemePreference.GLASS) "Adult Glass" else "After Dark"
                QuestButton(onClick = { scope.launch { manager.setAdultTheme(theme) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("${if (appearance.adultTheme == theme) "✓ " else ""}$name")
                }
            }
        }
        if ((!adultOnly && normalGlass) || (adultOnly && appearance.adultTheme == AdultThemePreference.GLASS)) {
            Text("Background blur", color = p.text)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 50, 100).forEach { value ->
                    QuestButton(onClick = { scope.launch { manager.setGlassBlur(value) } },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text("${if (appearance.blurPercent == value) "✓ " else ""}$value%")
                    }
                }
            }
            Text("Glass background", color = p.text)
            if (adultOnly) {
                QuestButton(onClick = { scope.launch { manager.useBuiltInAdultBackground() } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("${if (appearance.adultCustomPath.isBlank()) "✓ " else ""}Midnight Velvet · Artistic")
                }
            } else {
                listOf(GlassBackground.PLANET to "Planet", GlassBackground.MIDNIGHT_LAKE to "Midnight Lake",
                    GlassBackground.GRAPHITE_SILK to "Graphite Silk").forEach { (background, name) ->
                    QuestButton(onClick = { scope.launch { manager.setGlassBackground(background) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("${if (appearance.background == background) "✓ " else ""}$name")
                    }
                }
                if (appearance.customPath.isNotBlank()) QuestButton(onClick = { scope.launch { manager.setGlassBackground(GlassBackground.CUSTOM) } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("${if (appearance.background == GlassBackground.CUSTOM) "✓ " else ""}Your background")
                }
            }
            QuestButton(onClick = {
                if (!busy) runCatching { picker.launch(arrayOf("image/*")) }.onFailure {
                    message = "This device has no image picker. Choose a built-in background."
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (busy) "Saving…" else "Upload your background") }
            Text("Saved locally. Static images only.", color = p.muted)
            message?.let { Text(it, color = p.muted) }
        }
    }
}
