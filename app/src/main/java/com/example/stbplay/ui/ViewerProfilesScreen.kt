@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.stbplay.data.ViewerProfile
import com.example.stbplay.data.verifyViewerPin
import com.example.stbplay.ui.theme.LocalStbPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun ViewerProfilesScreen(
    profiles: List<ViewerProfile>, ownerPin: String,
    liveCategories: List<com.example.stbplay.data.model.PortalCategory> = emptyList(),
    vodCategories: List<com.example.stbplay.data.model.PortalCategory> = emptyList(),
    approvalPortalKey: String = "",
    onSelected: (ViewerProfile) -> Unit,
    onSave: (ViewerProfile, String) -> Unit,
    onSetOwnerPin: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val palette = LocalStbPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val uploadScope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<ViewerProfile?>(null) }
    var managing by remember { mutableStateOf(false) }
    var authorizeManagement by remember { mutableStateOf(false) }
    var setOwner by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ViewerProfile?>(null) }
    var allowedLive by remember { mutableStateOf<Set<String>>(emptySet()) }
    var allowedVod by remember { mutableStateOf<Set<String>>(emptySet()) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("🙂") }
    var error by remember { mutableStateOf<String?>(null) }
    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadScope.launch {
            val saved = withContext(Dispatchers.IO) { runCatching {
                val folder = java.io.File(context.filesDir, "viewer-avatars").apply { mkdirs() }
                val file = java.io.File(folder, UUID.randomUUID().toString() + ".img")
                try {
                    context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { output ->
                        val buffer = ByteArray(8192); var total = 0L
                        while (true) { val read = input.read(buffer); if (read < 0) break
                            total += read; require(total <= 5 * 1024 * 1024) { "Choose an avatar smaller than 5 MB." }; output.write(buffer, 0, read)
                        }
                    } }
                    file.absolutePath
                } catch (e: Throwable) { file.delete(); throw e }
            } }
            saved.onSuccess { avatar = it }.onFailure { error = it.message ?: "Could not read avatar." }
        }
    }

    BackHandler(enabled = managing || onBack != null) { if (managing) managing = false else onBack?.invoke() }
    Column(Modifier.fillMaxSize().background(palette.background).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (managing) "Manage profiles" else "Who's watching?", color = palette.text, fontSize = 26.sp)
        if (!managing) {
            profiles.forEachIndexed { index, profile ->
                QuestButton(onClick = { pending = profile }, modifier = Modifier.then(if (index == 0) Modifier.questInitialFocus() else Modifier).fillMaxWidth().height(56.dp)) {
                    if (profile.avatar.startsWith("/")) coil.compose.AsyncImage(profile.avatar, "Avatar", Modifier.size(36.dp))
                    else Text(profile.avatar)
                    Text("  ${profile.name}${if (profile.isKids) " · Kids" else ""}")
                }
            }
            QuestButton(onClick = { if (ownerPin.isBlank()) setOwner = true else authorizeManagement = true }) { Text("Manage profiles · Owner PIN") }
            onBack?.let { QuestButton(onClick = it) { Text("Back") } }
        } else {
            profiles.filter { it.id != "owner" }.forEach { existing ->
                QuestButton(onClick = {
                    editing = existing; name = existing.name; age = existing.age.toString(); avatar = existing.avatar; pin = ""
                    allowedLive = if (existing.approvalPortalKey == approvalPortalKey) existing.allowedLiveCategories else emptySet()
                    allowedVod = if (existing.approvalPortalKey == approvalPortalKey) existing.allowedVodCategories else emptySet()
                }) { Text("Edit ${existing.name}") }
            }
            QuestButton(onClick = { editing = null; name = ""; age = ""; pin = ""; avatar = "🙂"; allowedLive = emptySet(); allowedVod = emptySet() }) { Text("New profile") }
            ProfileField("Name", name) { name = it.take(32) }
            ProfileField("Your age (years)", age, true) { age = it.filter(Char::isDigit).take(3) }
            Text("Age is self-declared. Under 18 uses Kids mode. Owner approval is required for each allowed category.", color = palette.muted, fontSize = 13.sp)
            ProfileField("Personal PIN (4–8 digits, optional for Kids)", pin, true) { pin = it.filter(Char::isDigit).take(8) }
            if ((age.toIntOrNull() ?: 18) < 18) {
                Text("Approve Kids categories", color = palette.text)
                Text("Only checked categories will appear. Provider flags always apply. Empty selection hides all content.", color = palette.muted, fontSize = 12.sp)
                (liveCategories.filter { com.example.stbplay.data.isKidsCategory(it) }).forEach { category ->
                    QuestButton(onClick = { allowedLive = if (category.id in allowedLive) allowedLive - category.id else allowedLive + category.id }) { Text("${if (category.id in allowedLive) "✓ " else ""}Live · ${category.name}") }
                }
                (vodCategories.filter { com.example.stbplay.data.isKidsCategory(it) }).forEach { category ->
                    QuestButton(onClick = { allowedVod = if (category.id in allowedVod) allowedVod - category.id else allowedVod + category.id }) { Text("${if (category.id in allowedVod) "✓ " else ""}Movies/Series · ${category.name}") }
                }
                if (liveCategories.isEmpty() && vodCategories.isEmpty()) Text("Connect the portal, then edit this profile to approve categories.", color = palette.muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("🙂", "🦊", "🐼", "🚀").forEach { face -> QuestButton(onClick = { avatar = face }) { Text(face) } }
            }
            QuestButton(onClick = { avatarPicker.launch("image/*") }) { Text("Upload avatar photo") }
            if (avatar.startsWith("/")) coil.compose.AsyncImage(avatar, "Selected avatar", Modifier.size(64.dp))
            else Text("Avatar: $avatar", color = palette.text)
            error?.let { Text(it, color = palette.danger) }
            QuestButton(onClick = {
                val years = age.toIntOrNull()
                error = when {
                    name.isBlank() || years == null || years !in 1..120 -> "Enter a name and age from 1 to 120."
                    (years >= 18 && editing?.pinHash.isNullOrBlank() || pin.isNotEmpty()) && pin.length !in 4..8 -> "Use a 4–8 digit personal PIN."
                    else -> null
                }
                if (error == null) {
                    onSave(ViewerProfile(editing?.id ?: UUID.randomUUID().toString(), name.trim(), years!!, avatar, editing?.pinHash.orEmpty(), allowedLive, allowedVod, approvalPortalKey), pin)
                    name = ""; age = ""; pin = ""; editing = null; allowedLive = emptySet(); allowedVod = emptySet(); managing = false
                }
            }) { Text("Save profile") }
            QuestButton(onClick = { managing = false }) { Text("Cancel") }
        }
    }
    if (setOwner) ProviderPinSetupPrompt(onSave = { onSetOwnerPin(it); setOwner = false; managing = true }, onCancel = { setOwner = false })
    if (authorizeManagement) PinPrompt("profile management", ownerPin, { authorizeManagement = false; managing = true }, { authorizeManagement = false })
    pending?.let { profile ->
        if (profile.id == "owner") {
            if (ownerPin.isBlank()) ProviderPinSetupPrompt(onSave = { onSetOwnerPin(it); pending = null; onSelected(profile) }, onCancel = { pending = null })
            else PinPrompt(profile.name, ownerPin, { pending = null; onSelected(profile) }, { pending = null })
        } else if (profile.pinHash.isBlank()) {
            LaunchedEffect(profile.id) { pending = null; onSelected(profile) }
        } else ViewerPinPrompt(profile, { pending = null; onSelected(profile) }, { pending = null })
    }
}

@Composable
private fun ProfileField(label: String, value: String, number: Boolean = false, onChange: (String) -> Unit) {
    val p = LocalStbPalette.current
    Text(label, color = p.muted, fontSize = 13.sp)
    RemoteTextField(value, onChange, Modifier.fillMaxWidth().height(48.dp).background(p.panelSoft).padding(12.dp), singleLine = true,
        visualTransformation = if (label.contains("PIN")) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = TextStyle(color = p.text, fontSize = 16.sp), keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text))
}

@Composable
private fun ViewerPinPrompt(profile: ViewerProfile, onVerified: () -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        val p = LocalStbPalette.current
        Column(Modifier.fillMaxWidth().background(p.panel).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${profile.name} · Personal PIN", color = p.text)
            ProfileField("PIN", pin, true) { pin = it.filter(Char::isDigit).take(8); failed = false }
            if (failed) Text("Incorrect PIN", color = p.danger)
            QuestButton(onClick = { if (!busy) { busy = true; scope.launch {
                val valid = withContext(Dispatchers.Default) { verifyViewerPin(pin, profile.pinHash) }
                busy = false; if (valid) onVerified() else failed = true
            } } }) { Text(if (busy) "Checking…" else "Continue") }
            QuestButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
fun PhoneFirstSetup(
    position: com.example.stbplay.data.CategoryDropdownPosition, columns: Int,
    theme: com.example.stbplay.data.ThemePreference,
    onPosition: (com.example.stbplay.data.CategoryDropdownPosition) -> Unit,
    onColumns: (Int) -> Unit, onTheme: (com.example.stbplay.data.ThemePreference) -> Unit, onDone: () -> Unit
) {
    val p = LocalStbPalette.current
    Column(Modifier.fillMaxSize().background(p.background).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Make it yours", color = p.text, fontSize = 26.sp)
        Text("Live TV & Movies category selector", color = p.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.example.stbplay.data.CategoryDropdownPosition.entries.forEach { option ->
                QuestButton(onClick = { onPosition(option) }) { Text("${if (position == option) "✓ " else ""}${option.name.lowercase().replaceFirstChar(Char::uppercase)}") }
            }
        }
        Text("Movie grid", color = p.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (2..4).forEach { count ->
            QuestButton(onClick = { onColumns(count) }) { Text("${if (columns == count) "✓ " else ""}$count") }
        } }
        Text("Theme", color = p.text)
        com.example.stbplay.data.ThemePreference.entries.forEach { option ->
            QuestButton(onClick = { onTheme(option) }) { Text("${if (theme == option) "✓ " else ""}${when(option) { com.example.stbplay.data.ThemePreference.LIGHT -> "Ivory"; com.example.stbplay.data.ThemePreference.BLUE -> "Blue"; else -> "Black" }}") }
        }
        QuestButton(onClick = onDone) { Text("Save & continue") }
    }
}
