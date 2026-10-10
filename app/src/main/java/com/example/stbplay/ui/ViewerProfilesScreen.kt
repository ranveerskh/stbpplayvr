@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.example.stbplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.tv.material3.ButtonDefaults
import com.example.stbplay.isAndroidTvDevice
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
    onBack: (() -> Unit)? = null,
    activeViewerId: String? = null,
    onDelete: (String, String, Boolean) -> Unit = { _, _, _ -> }
) {
    val palette = LocalStbPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val television = remember(context) { context.isAndroidTvDevice() }
    val uploadScope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<ViewerProfile?>(null) }
    var managing by remember { mutableStateOf(false) }
    var ownerAuthorized by remember { mutableStateOf(false) }
    var pinMode by remember { mutableStateOf(ViewerPinMode.PROFILE_ENTRY) }
    var authorizeManagement by remember { mutableStateOf(false) }
    var setOwner by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ViewerProfile?>(null) }
    var editing by remember { mutableStateOf<ViewerProfile?>(null) }
    var allowedLive by remember { mutableStateOf<Set<String>>(emptySet()) }
    var allowedVod by remember { mutableStateOf<Set<String>>(emptySet()) }
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf(familyAvatars.first().id) }
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
        editing = null; name = ""; age = ""; pin = ""; avatar = familyAvatars.first().id
        allowedLive = emptySet(); allowedVod = emptySet(); pinMode = ViewerPinMode.PROFILE_ENTRY
        error = null; managing = true
    }
    fun editProfile(existing: ViewerProfile) {
        editing = existing; name = existing.name; age = existing.age.toString()
        avatar = existing.avatar; pin = ""; pinMode = existing.pinMode; error = null
        allowedLive = if (existing.approvalPortalKey == approvalPortalKey) existing.allowedLiveCategories else emptySet()
        allowedVod = if (existing.approvalPortalKey == approvalPortalKey) existing.allowedVodCategories else emptySet()
        managing = true
    }
    fun saveProfile() {
        val original = editing
        val years = if (original?.id == "owner") original.age else age.toInt()
        val approval = ownerAuthorized || years < 18 || (original != null && !original.isKids && original.adultApproved)
        onSave(ViewerProfile(original?.id ?: UUID.randomUUID().toString(), name.trim(), years, avatar,
            if (years < 18) "" else original?.pinHash.orEmpty(),
            if (!ownerAuthorized && original != null) original.allowedLiveCategories else allowedLive,
            if (!ownerAuthorized && original != null) original.allowedVodCategories else allowedVod,
            if (!ownerAuthorized && original != null) original.approvalPortalKey else approvalPortalKey,
            pinMode, adultApproved = approval), if (years < 18 || original?.id == "owner") "" else pin)
        managing = false; error = null
    }
    fun openOwnerManagement() {
        ownerAuthorized = true
        profiles.firstOrNull { it.id == "owner" }?.let { editProfile(it) } ?: newProfile()
    }
    BackHandler(enabled = managing || onBack != null) {
        if (managing) { managing = false; error = null } else onBack?.invoke()
    }
    Box(Modifier.fillMaxSize().background(palette.background), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = if (managing) 680.dp else 960.dp).fillMaxWidth()
        .verticalScroll(rememberScrollState()).padding(horizontal = if (television) 40.dp else 20.dp, vertical = if (television) 32.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (television) 24.dp else 16.dp)) {
        Text(if (managing) (if (editing == null) "Add profile" else "Edit profile") else "Who's watching?", color = palette.text, fontSize = if (television) 32.sp else 26.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = if (television) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start)
        Text(if (managing) "Your favourites, history and movie grid stay with your profile." else "Choose your space. Each profile keeps its own favourites and history.", color = palette.muted, fontSize = if (television) 16.sp else 14.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = if (television) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start)
        if (!managing) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = if (television) 20.dp else 12.dp
                val columns = (maxWidth.value / if (television) 176f else 132f).toInt().coerceIn(2, if (television) 5 else 6)
                val tileWidth = ((maxWidth - gap * (columns - 1)) / columns).coerceAtMost(if (television) 168.dp else 200.dp)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    profiles.chunked(columns).forEachIndexed { rowIndex, row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                            row.forEachIndexed { index, profile ->
                                ProfileButton(onClick = { pending = profile }, modifier = Modifier.width(tileWidth).height(if (television) 176.dp else 148.dp)
                                    .then(if (rowIndex == 0 && index == 0) Modifier.questInitialFocus() else Modifier)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ProfileAvatar(profile.avatar, if (television) 112 else 88)
                                        Text(profile.name, fontSize = if (television) 18.sp else 16.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val actions = mutableListOf<Pair<String, () -> Unit>>()
            actions += "Add profile" to { ownerAuthorized = false; newProfile() }
            profiles.firstOrNull { it.id == activeViewerId && it.id != "owner" }?.let { ownProfile ->
                actions += "Edit my profile" to { ownerAuthorized = false; editProfile(ownProfile) }
            }
            actions += "Manage profiles" to { if (ownerPin.isBlank()) setOwner = true else authorizeManagement = true }
            onBack?.let { back -> actions += "Back" to back }
            actions.chunked(if (television) 4 else 2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, action) ->
                        ProfileButton(onClick = action, modifier = Modifier.weight(1f)) { Text(label) }
                    }
                    repeat((if (television) 4 else 2) - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        } else {
            if (ownerAuthorized) {
                profiles.forEach { existing ->
                    ProfileButton(onClick = {
                        editProfile(existing)
                    }, modifier = Modifier.fillMaxWidth()) { Text("Edit ${existing.name}") }
                }
                ProfileButton(onClick = { newProfile() }, modifier = Modifier.fillMaxWidth()) { Text("New profile") }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.panel).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileField("Name", name) { name = it.take(32); error = null }
                if (editing?.id != "owner") ProfileField("Your age (years)", age, true) { age = it.filter(Char::isDigit).take(3); error = null }
                val kids = age.toIntOrNull()?.let { it < 18 } == true
                if (editing?.id != "owner") Text(if (kids) "Kids enter without a PIN. Only owner-approved Kids categories appear." else "Anyone can save a profile. New adults need one-time owner approval before first entry.", color = palette.muted, fontSize = 13.sp)
                if (!kids && editing?.id != "owner") {
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
                ProfileAvatar(avatar, 88)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = (maxWidth.value / 88f).toInt().coerceIn(3, 8)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        familyAvatars.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { option ->
                                    ProfileButton(onClick = { avatar = option.id }, modifier = Modifier.weight(1f).height(72.dp)
                                        .then(if (avatar == option.id) Modifier.border(2.dp, palette.accent, RoundedCornerShape(12.dp)) else Modifier)) {
                                        ProfileAvatar(option.id, 48)
                                    }
                                }
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                ProfileButton(onClick = { runCatching { avatarPicker.launch("image/*") }.onFailure { error = "No photo picker is available. Choose a built-in avatar instead." } }, modifier = Modifier.fillMaxWidth()) { Text("Upload avatar photo") }
            }
            error?.let { Text(it, color = palette.danger) }
            ProfileButton(onClick = {
                val years = if (editing?.id == "owner") editing?.age else age.toIntOrNull()
                error = when {
                    name.isBlank() || years == null || years !in 1..120 -> "Enter a name and age from 1 to 120."
                    editing?.id != "owner" && years >= 18 && ((editing?.pinHash.isNullOrBlank() && pin.isEmpty()) || (pin.isNotEmpty() && pin.length !in 4..8)) -> "Use a 4–8 digit personal PIN."
                    else -> null
                }
                if (error == null) {
                    saveProfile()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
            editing?.takeIf { it.id != "owner" && (ownerAuthorized || it.id == activeViewerId) }?.let { profile ->
                ProfileButton(onClick = { deleting = profile }, modifier = Modifier.fillMaxWidth()) { Text("Delete profile", color = palette.danger) }
            }
            ProfileButton(onClick = { managing = false; error = null }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
    }
    if (setOwner) ProviderPinSetupPrompt(onSave = { onSetOwnerPin(it); setOwner = false; openOwnerManagement() }, onCancel = { setOwner = false })
    if (authorizeManagement) PinPrompt("profile management", ownerPin, { authorizeManagement = false; openOwnerManagement() }, { authorizeManagement = false })
    deleting?.let { profile ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { deleting = null }) {
            Column(Modifier.fillMaxWidth().background(palette.panel, RoundedCornerShape(16.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Delete ${profile.name}?", color = palette.text, fontSize = 20.sp)
                Text("This removes this profile's favourites, history and settings.", color = palette.muted)
                ProfileButton(onClick = {
                    onDelete(profile.id, if (ownerAuthorized) "owner" else activeViewerId.orEmpty(), ownerAuthorized)
                    deleting = null; managing = false; editing = null; error = null
                }, modifier = Modifier.fillMaxWidth()) { Text("Delete profile", color = palette.danger) }
                ProfileButton(onClick = { deleting = null }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
    pending?.let { profile ->
        if (profile.id == "owner") {
            if (ownerPin.isBlank()) ProviderPinSetupPrompt(onSave = { onSetOwnerPin(it); pending = null; onSelected(profile) }, onCancel = { pending = null })
            else PinPrompt(profile.name, ownerPin, { pending = null; onSelected(profile) }, { pending = null })
        } else if (!profile.isKids && !profile.adultApproved) {
            val approve = {
                val approved = profile.copy(adultApproved = true)
                onSave(approved, ""); pending = approved
            }
            if (ownerPin.isBlank()) ProviderPinSetupPrompt(onSave = { onSetOwnerPin(it); approve() }, onCancel = { pending = null })
            else PinPrompt("Approve adult profile ${profile.name}", ownerPin, approve, { pending = null })
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
    val builtIn = familyAvatars.firstOrNull { it.id == avatar } ?: familyAvatars.first().takeIf { avatar == "🙂" }
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        if (builtIn != null) androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(builtIn.drawable), contentDescription = builtIn.label,
            modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        else if (avatar.startsWith("/")) coil.compose.AsyncImage(avatar, "Avatar",
            Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        else Text(avatar, fontSize = (size * 0.65).sp)
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
