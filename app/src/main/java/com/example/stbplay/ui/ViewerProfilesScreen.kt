@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.tv.material3.ButtonDefaults
import com.example.stbplay.data.ViewerPinMode
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

/** Keeps Settings mounted so cancel/back restores its subsection, scroll and focus. */
@Composable
fun ViewerSwitchDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false), content = content)
}

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
    var ownerAuthorized by remember { mutableStateOf(false) }
    var approvingAdult by remember { mutableStateOf(false) }
    var pinMode by remember { mutableStateOf(ViewerPinMode.PROFILE_ENTRY) }
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

    fun newProfile() {
        editing = null; name = ""; age = ""; pin = ""; avatar = "🙂"
        allowedLive = emptySet(); allowedVod = emptySet(); pinMode = ViewerPinMode.PROFILE_ENTRY
        error = null; managing = true
    }
    fun saveProfile() {
        val years = age.toInt()
        onSave(ViewerProfile(editing?.id ?: UUID.randomUUID().toString(), name.trim(), years, avatar,
            if (years < 18) "" else editing?.pinHash.orEmpty(), allowedLive, allowedVod,
            approvalPortalKey, pinMode), if (years < 18) "" else pin)
        managing = false; error = null
    }
    BackHandler(enabled = managing || onBack != null) {
        if (managing) { managing = false; error = null } else onBack?.invoke()
    }
    Column(Modifier.fillMaxSize().background(palette.background).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (managing) (if (editing == null) "Add profile" else "Edit profile") else "Who's watching?", color = palette.text, fontSize = 26.sp)
        Text(if (managing) "Your favourites, history and movie grid stay with your profile." else "Choose your space. Each profile keeps its own favourites and history.", color = palette.muted, fontSize = 14.sp)
        if (!managing) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 700.dp) 3 else 2
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    profiles.chunked(columns).forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEachIndexed { index, profile ->
                                ProfileButton(onClick = { pending = profile }, modifier = Modifier.weight(1f).height(128.dp)
                                    .then(if (rowIndex == 0 && index == 0) Modifier.questInitialFocus() else Modifier)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ProfileAvatar(profile.avatar, 44)
                                        Text(profile.name, maxLines = 1)
                                        Text(if (profile.isKids) "Kids · No PIN" else if (profile.id == "owner") "Owner" else if (profile.needsEntryPin) "Profile PIN" else "Restricted content PIN", fontSize = 12.sp)
                                    }
                                }
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            ProfileButton(onClick = { ownerAuthorized = false; newProfile() }, modifier = Modifier.fillMaxWidth()) { Text("Add profile") }
            ProfileButton(onClick = { if (ownerPin.isBlank()) setOwner = true else authorizeManagement = true }, modifier = Modifier.fillMaxWidth()) { Text("Manage profiles · Owner PIN") }
            onBack?.let { ProfileButton(onClick = it, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
        } else {
            if (ownerAuthorized) {
                profiles.filter { it.id != "owner" }.forEach { existing ->
                    ProfileButton(onClick = {
                        editing = existing; name = existing.name; age = existing.age.toString(); avatar = existing.avatar; pin = ""; pinMode = existing.pinMode; error = null
                        allowedLive = if (existing.approvalPortalKey == approvalPortalKey) existing.allowedLiveCategories else emptySet()
                        allowedVod = if (existing.approvalPortalKey == approvalPortalKey) existing.allowedVodCategories else emptySet()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Edit ${existing.name}") }
                }
                ProfileButton(onClick = { newProfile() }, modifier = Modifier.fillMaxWidth()) { Text("New profile") }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.panel).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileField("Name", name) { name = it.take(32); error = null }
                ProfileField("Your age (years)", age, true) { age = it.filter(Char::isDigit).take(3); error = null }
                val kids = age.toIntOrNull()?.let { it < 18 } == true
                Text(if (kids) "Kids enter without a PIN. Only owner-approved Kids categories appear." else "Age is self-declared. Adult profiles need owner approval when created.", color = palette.muted, fontSize = 13.sp)
                if (!kids) {
                    ProfileField(if (editing == null) "Personal PIN (4–8 digits)" else "New personal PIN (blank keeps current PIN)", pin, true) { pin = it.filter(Char::isDigit).take(8); error = null }
                    Text("When should we ask for your PIN?", color = palette.text, fontSize = 14.sp)
                    ProfileButton(onClick = { pinMode = ViewerPinMode.PROFILE_ENTRY }, modifier = Modifier.fillMaxWidth()) { Text("${if (pinMode == ViewerPinMode.PROFILE_ENTRY) "✓ " else ""}Every time I enter my profile") }
                    ProfileButton(onClick = { pinMode = ViewerPinMode.RESTRICTED_ONLY }, modifier = Modifier.fillMaxWidth()) { Text("${if (pinMode == ViewerPinMode.RESTRICTED_ONLY) "✓ " else ""}Only for restricted content") }
                }
                if (kids && ownerAuthorized) {
                    Text("Approve Kids categories", color = palette.text)
                    Text("Only checked categories appear. Provider adult flags always apply.", color = palette.muted, fontSize = 12.sp)
                    liveCategories.filter { com.example.stbplay.data.isKidsCategory(it) }.forEach { category ->
                        ProfileButton(onClick = { allowedLive = if (category.id in allowedLive) allowedLive - category.id else allowedLive + category.id }, modifier = Modifier.fillMaxWidth()) { Text("${if (category.id in allowedLive) "✓ " else ""}Live · ${category.name}") }
                    }
                    vodCategories.filter { com.example.stbplay.data.isKidsCategory(it) }.forEach { category ->
                        ProfileButton(onClick = { allowedVod = if (category.id in allowedVod) allowedVod - category.id else allowedVod + category.id }, modifier = Modifier.fillMaxWidth()) { Text("${if (category.id in allowedVod) "✓ " else ""}Movies/Series · ${category.name}") }
                    }
                    if (liveCategories.isEmpty() && vodCategories.isEmpty()) Text("Connect the portal, then approve categories here.", color = palette.muted)
                }
                Text("Avatar", color = palette.text)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("🙂", "🦊", "🐼", "🚀").forEach { face -> ProfileButton(onClick = { avatar = face }, modifier = Modifier.weight(1f)) { Text(face) } }
                }
                ProfileButton(onClick = { runCatching { avatarPicker.launch("image/*") }.onFailure { error = "No photo picker is available. Choose a built-in avatar instead." } }, modifier = Modifier.fillMaxWidth()) { Text("Upload avatar photo") }
                ProfileAvatar(avatar, 56)
            }
            error?.let { Text(it, color = palette.danger) }
            ProfileButton(onClick = {
                val years = age.toIntOrNull()
                error = when {
                    name.isBlank() || years == null || years !in 1..120 -> "Enter a name and age from 1 to 120."
                    years >= 18 && ((editing?.pinHash.isNullOrBlank() && pin.isEmpty()) || (pin.isNotEmpty() && pin.length !in 4..8)) -> "Use a 4–8 digit personal PIN."
                    else -> null
                }
                if (error == null) {
                    if (years!! >= 18 && !ownerAuthorized) approvingAdult = true else saveProfile()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
            ProfileButton(onClick = { managing = false; error = null }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
    if (setOwner) ProviderPinSetupPrompt(onSave = { onSetOwnerPin(it); setOwner = false; ownerAuthorized = true; newProfile() }, onCancel = { setOwner = false })
    if (authorizeManagement) PinPrompt("profile management", ownerPin, { authorizeManagement = false; ownerAuthorized = true; newProfile() }, { authorizeManagement = false })
    if (approvingAdult) {
        if (ownerPin.isBlank()) ProviderPinSetupPrompt(onSave = { onSetOwnerPin(it); approvingAdult = false; saveProfile() }, onCancel = { approvingAdult = false })
        else PinPrompt("Approve adult profile", ownerPin, { approvingAdult = false; saveProfile() }, { approvingAdult = false })
    }
    pending?.let { profile ->
        if (profile.id == "owner") {
            if (ownerPin.isBlank()) ProviderPinSetupPrompt(onSave = { onSetOwnerPin(it); pending = null; onSelected(profile) }, onCancel = { pending = null })
            else PinPrompt(profile.name, ownerPin, { pending = null; onSelected(profile) }, { pending = null })
        } else if (!profile.needsEntryPin || profile.pinHash.isBlank()) {
            LaunchedEffect(profile.id) { pending = null; onSelected(profile) }
        } else ViewerPinPrompt(profile, { pending = null; onSelected(profile) }, { pending = null })
    }
}

@Composable
private fun ProfileButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val p = LocalStbPalette.current
    QuestButton(onClick, modifier.heightIn(min = 48.dp), colors = ButtonDefaults.colors(
        containerColor = p.panelSoft, contentColor = p.text,
        focusedContainerColor = p.focusedAccent, focusedContentColor = p.onAccent,
        pressedContainerColor = p.accent, pressedContentColor = p.onAccent
    ), focusScale = 1f, content = content)
}

@Composable
private fun ProfileAvatar(avatar: String, size: Int) {
    if (avatar.startsWith("/")) coil.compose.AsyncImage(avatar, "Avatar", Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    else Text(avatar, fontSize = (size * 0.65).sp)
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
fun ViewerPinPrompt(profile: ViewerProfile, onVerified: () -> Unit, onCancel: () -> Unit, title: String = profile.name) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        val p = LocalStbPalette.current
        Column(Modifier.fillMaxWidth().background(p.panel).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("$title · Personal PIN", color = p.text)
            ProfileField("PIN", pin, true) { pin = it.filter(Char::isDigit).take(8); failed = false }
            if (failed) Text("Incorrect PIN", color = p.danger)
            ProfileButton(onClick = { if (!busy) { busy = true; scope.launch {
                val valid = withContext(Dispatchers.Default) { verifyViewerPin(pin, profile.pinHash) }
                busy = false; if (valid) onVerified() else failed = true
            } } }) { Text(if (busy) "Checking…" else "Continue") }
            ProfileButton(onClick = onCancel) { Text("Cancel") }
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
