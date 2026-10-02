@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.example.stbplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.example.stbplay.ui.QuestButton
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.example.stbplay.domain.model.PortalSettings
import com.example.stbplay.domain.model.generateStbPlayMac
import com.example.stbplay.ui.questInitialFocus
import com.example.stbplay.ui.theme.LocalStbPalette

private val SetupNavy: Color @Composable get() = LocalStbPalette.current.background
private val SetupPanel: Color @Composable get() = LocalStbPalette.current.panel
private val SetupGold: Color @Composable get() = LocalStbPalette.current.accent
private val SetupGoldLight: Color @Composable get() = LocalStbPalette.current.accentLight
private val SetupText: Color @Composable get() = LocalStbPalette.current.text
private val SetupMuted: Color @Composable get() = LocalStbPalette.current.muted
private val SetupError: Color @Composable get() = LocalStbPalette.current.danger
private val SetupOnAccent: Color @Composable get() = LocalStbPalette.current.onAccent

/** First-time portal setup and the reusable Add/Edit Portal form. */
@Composable
fun SetupScreen(
    initialSettings: PortalSettings = PortalSettings(),
    onSave: (PortalSettings) -> Unit,
    onProviderPair: ((PortalSettings) -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    var name by remember(initialSettings.id) { mutableStateOf(initialSettings.name) }
    var url by remember(initialSettings.id) { mutableStateOf(initialSettings.url) }
    var mac by remember(initialSettings.id) { mutableStateOf(initialSettings.mac.ifBlank { generateStbPlayMac() }) }
    var pin by remember(initialSettings.id) { mutableStateOf(initialSettings.pin) }
    var error by remember(initialSettings.id) { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val phoneLayout = LocalConfiguration.current.screenWidthDp < 900 &&
        !context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)

    Box(modifier = Modifier.fillMaxSize().background(SetupNavy)) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 800.dp)
                .then(if (phoneLayout) Modifier.imePadding().verticalScroll(rememberScrollState()).padding(18.dp) else Modifier.padding(42.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("STB PLAY", color = SetupGold, fontSize = if (phoneLayout) 32.sp else 46.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                if (initialSettings.id.isBlank()) "Portal Setup" else "Edit Portal",
                color = SetupGoldLight,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(if (phoneLayout) 18.dp else 30.dp))

            SetupField("Nickname", name, "My IPTV Portal", compact = phoneLayout) { name = it }
            SetupField("Portal URL", url, "http://your-portal.example", initialFocus = true, compact = phoneLayout) { url = it }
            SetupField("MAC Address", mac, "02:00:00:00:00:00", compact = phoneLayout) { mac = it.uppercase() }
            SetupField("Parental PIN", pin, "4 to 8 digits", KeyboardType.NumberPassword, compact = phoneLayout) {
                pin = it.filter(Char::isDigit).take(8)
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = SetupError, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(24.dp))
            val actionModifier = if (phoneLayout) Modifier.fillMaxWidth() else Modifier
            val actionContent: @Composable () -> Unit = {
                QuestButton(
                    onClick = {
                        val normalUrl = url.trim().trimEnd('/')
                        val normalMac = mac.trim().uppercase()
                        error = when {
                            name.trim().isBlank() -> "Enter a name for this portal."
                            normalUrl.isBlank() -> "Enter the portal URL."
                            !normalUrl.startsWith("http://", true) && !normalUrl.startsWith("https://", true) ->
                                "Portal URL must start with http:// or https://."
                            !Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$").matches(normalMac) ->
                                "Enter a valid MAC address, for example 02:00:00:00:00:00."
                            pin.length !in 4..8 -> "Enter a parental PIN with 4 to 8 digits."
                            else -> null
                        }
                        if (error == null) {
                            onSave(
                                PortalSettings(
                                    id = initialSettings.id,
                                    name = name.trim(),
                                    url = normalUrl,
                                    mac = normalMac,
                                    pin = pin
                                )
                            )
                        }
                    },
                    modifier = if (phoneLayout) Modifier.fillMaxWidth() else Modifier.width(250.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = SetupGold,
                        contentColor = SetupOnAccent,
                        focusedContainerColor = SetupGoldLight,
                        focusedContentColor = SetupOnAccent
                    )
                ) { Text("Save & Connect", fontWeight = FontWeight.Bold) }

                onCancel?.let { cancel ->
                    QuestButton(
                        onClick = cancel,
                        modifier = actionModifier,
                        colors = ButtonDefaults.colors(
                            containerColor = LocalStbPalette.current.panelSoft,
                            contentColor = SetupText
                        )
                    ) { Text("Cancel") }
                }
            }
            if (phoneLayout) Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { actionContent() }
            else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { actionContent() }

            onProviderPair?.let { pair ->
                Spacer(modifier = Modifier.height(10.dp))
                QuestButton(
                    onClick = {
                        val normalMac = mac.trim().uppercase()
                        error = when {
                            !Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$").matches(normalMac) ->
                                "Enter a valid MAC address before provider pairing."
                            pin.isNotBlank() && pin.length !in 4..8 -> "Enter a parental PIN with 4 to 8 digits."
                            else -> null
                        }
                        if (error == null) pair(
                            PortalSettings(
                                id = initialSettings.id,
                                name = name.trim().ifBlank { "My device" },
                                url = url.trim().trimEnd('/'),
                                mac = normalMac,
                                pin = pin
                            )
                        )
                    },
                    modifier = actionModifier,
                    colors = ButtonDefaults.colors(
                        containerColor = LocalStbPalette.current.panelSoft,
                        contentColor = SetupText,
                        focusedContainerColor = SetupGoldLight,
                        focusedContentColor = SetupOnAccent
                    )
                ) { Text("Link with provider", fontWeight = FontWeight.SemiBold) }
                Text(
                    "This sends the device ID and portal MAC to STB Play so your provider can assign an authorized portal. Manual setup remains available above.",
                    color = SetupMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 560.dp)
                )
            }

            Spacer(modifier = Modifier.height(19.dp))
            Text(
                "Use only a portal and subscription you are authorized to use. Portal details stay on this device.",
                color = SetupMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 560.dp)
            )
        }
    }
}

@Composable
private fun SetupField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    initialFocus: Boolean = false,
    compact: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val field: @Composable (Modifier) -> Unit = { widthModifier ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .then(if (initialFocus) Modifier.questInitialFocus() else Modifier)
                .then(widthModifier)
                .height(52.dp)
                .onFocusChanged { focused = it.isFocused }
                .background(SetupPanel, androidx.compose.foundation.shape.RoundedCornerShape(11.dp))
                .border(
                    2.dp,
                    if (focused) SetupGold else Color.Transparent,
                    androidx.compose.foundation.shape.RoundedCornerShape(11.dp)
                )
                .padding(horizontal = 17.dp, vertical = 15.dp),
            singleLine = true,
            textStyle = TextStyle(color = SetupText, fontSize = 17.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { inner ->
                if (value.isBlank()) Text(placeholder, color = SetupMuted, fontSize = 16.sp)
                inner()
            }
        )
    }
    if (compact) Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = SetupText, fontSize = 14.sp)
        field(Modifier.fillMaxWidth())
    } else Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SetupText, fontSize = 15.sp, modifier = Modifier.width(190.dp))
        field(Modifier.weight(1f))
    }
}
