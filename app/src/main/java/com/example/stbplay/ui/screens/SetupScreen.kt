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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.example.stbplay.domain.model.PortalSettings
import com.example.stbplay.domain.model.generateStbPlayMac

private val SetupNavy = Color(0xFF061426)
private val SetupPanel = Color(0xFF0D223B)
private val SetupGold = Color(0xFFDDB32F)
private val SetupGoldLight = Color(0xFFFFD966)
private val SetupText = Color(0xFFF4F6FA)
private val SetupMuted = Color(0xFF9AA8B8)
private val SetupError = Color(0xFFFFA4A4)

/** First-time portal setup and the reusable Add/Edit Portal form. */
@Composable
fun SetupScreen(
    initialSettings: PortalSettings = PortalSettings(),
    onSave: (PortalSettings) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var name by remember(initialSettings.id) { mutableStateOf(initialSettings.name) }
    var url by remember(initialSettings.id) { mutableStateOf(initialSettings.url) }
    var mac by remember(initialSettings.id) { mutableStateOf(initialSettings.mac.ifBlank { generateStbPlayMac() }) }
    var pin by remember(initialSettings.id) { mutableStateOf(initialSettings.pin) }
    var error by remember(initialSettings.id) { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(SetupNavy)) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 800.dp)
                .padding(42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("STB PLAY", color = SetupGold, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                if (initialSettings.id.isBlank()) "Portal Setup" else "Edit Portal",
                color = SetupGoldLight,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(30.dp))

            SetupField("Nickname", name, "My IPTV Portal") { name = it }
            SetupField("Portal URL", url, "http://your-portal.example") { url = it }
            SetupField("MAC Address", mac, "02:00:00:00:00:00") { mac = it.uppercase() }
            SetupField("Parental PIN", pin, "4 to 8 digits", KeyboardType.NumberPassword) {
                pin = it.filter(Char::isDigit).take(8)
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = SetupError, fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
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
                    modifier = Modifier.width(250.dp),
                    colors = ButtonDefaults.colors(
                        containerColor = SetupGold,
                        contentColor = SetupNavy,
                        focusedContainerColor = SetupGoldLight,
                        focusedContentColor = SetupNavy
                    )
                ) { Text("Save & Connect", fontWeight = FontWeight.Bold) }

                onCancel?.let { cancel ->
                    Button(
                        onClick = cancel,
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFF153452),
                            contentColor = SetupText
                        )
                    ) { Text("Cancel") }
                }
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
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SetupText, fontSize = 15.sp, modifier = Modifier.width(190.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
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
}
