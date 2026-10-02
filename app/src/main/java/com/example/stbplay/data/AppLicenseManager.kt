package com.example.stbplay.data

import android.content.Context
import android.provider.Settings
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AppLicenseState(
    val statusText: String = "No STB PLAY key activated",
    val expiryText: String = "",
    val message: String = "",
    val keyHint: String = "",
    val registered: Boolean = false,
    val busy: Boolean = false
)

/**
 * Optional STB PLAY license display/activation. It never gates portal or media playback.
 * Portal URL, portal MAC, catalogue and viewing history are deliberately not included in requests.
 */
class AppLicenseManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("stb_play_license", Context.MODE_PRIVATE)
    private val deviceId: String by lazy {
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }
            ?: preferences.getString("device_seed", null)
            ?: java.util.UUID.randomUUID().toString().also { preferences.edit().putString("device_seed", it).apply() }
        sha256("${appContext.packageName}:$androidId")
    }

    private val mutableState = MutableStateFlow(readSavedState())
    val state: StateFlow<AppLicenseState> = mutableState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .build()

    suspend fun activate(rawKey: String) = withContext(Dispatchers.IO) {
        val key = rawKey.trim().uppercase()
        if (key.isBlank() || key.length > 80) {
            mutableState.value = mutableState.value.copy(message = "Enter a valid license key.")
            return@withContext
        }
        if (mutableState.value.busy) return@withContext
        mutableState.value = mutableState.value.copy(busy = true, message = "Verifying license key…")
        try {
            val response = send("/api/register", key)
            if (response.optBoolean("registered")) {
                saveLicenseKey(key)
                applySuccess(response, key, "Key activated successfully.")
            } else {
                val message = response.optString("error").ifBlank { "Could not activate this key." }
                mutableState.value = mutableState.value.copy(busy = false, message = message)
            }
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(
                busy = false,
                message = "Could not reach the license service. Check your connection and try again."
            )
        }
    }

    suspend fun refresh(force: Boolean = false) = withContext(Dispatchers.IO) {
        val key = readLicenseKey()
        if (key.isNullOrBlank()) {
            if (!mutableState.value.busy) mutableState.value = readSavedState()
            return@withContext
        }
        val lastCheck = preferences.getLong("last_check", 0L)
        if (!force && System.currentTimeMillis() - lastCheck < TimeUnit.HOURS.toMillis(12)) return@withContext
        if (mutableState.value.busy) return@withContext
        mutableState.value = mutableState.value.copy(busy = true, message = "Checking license status…")
        try {
            val response = send("/api/heartbeat", key)
            if (response.optBoolean("registered")) {
                applySuccess(response, key, "License status checked.")
            } else {
                val message = response.optString("error").ifBlank { "This device is not registered for this key." }
                mutableState.value = mutableState.value.copy(
                    statusText = if (response.optBoolean("licenseExpired")) "License expired" else "License inactive",
                    message = message,
                    registered = false,
                    busy = false
                )
                preferences.edit().putBoolean("registered", false).putLong("last_check", System.currentTimeMillis()).apply()
            }
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(
                busy = false,
                message = "Could not check online. Showing the last saved license status."
            )
        }
    }

    private fun send(path: String, key: String): JSONObject {
        val payload = JSONObject()
            .put("licenseKey", key)
            .put("deviceId", deviceId)
            .put("platform", "android")
            .put("appVersion", com.example.stbplay.BuildConfig.VERSION_NAME)
        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(API_BASE_URL + path)
            .post(body)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(responseText) }.getOrDefault(JSONObject())
            if (!response.isSuccessful && json.optString("error").isBlank()) {
                json.put("error", "License service returned HTTP ${response.code}.")
            }
            return json
        }
    }

    private fun applySuccess(response: JSONObject, key: String, message: String) {
        val expiry = response.optString("licenseExpiresAt").takeUnless { it.isBlank() || it == "null" }
        val label = response.optString("licenseLabel").ifBlank { "STB PLAY license" }
        val expiryText = expiry?.let(::formatExpiry).orEmpty()
        val status = if (expiry != null && runCatching { java.time.Instant.parse(expiry).toEpochMilli() <= System.currentTimeMillis() }.getOrDefault(false)) {
            "License expired"
        } else "Active · $label"
        preferences.edit()
            .putString("license_expiry", expiry.orEmpty())
            .putString("license_label", label)
            .putString("key_hint", key.takeLast(4))
            .putBoolean("registered", status != "License expired")
            .putLong("last_check", System.currentTimeMillis())
            .apply()
        mutableState.value = AppLicenseState(
            statusText = status,
            expiryText = expiryText,
            message = message,
            keyHint = key.takeLast(4),
            registered = status != "License expired",
            busy = false
        )
    }

    private fun readSavedState(): AppLicenseState {
        val expiry = preferences.getString("license_expiry", "").orEmpty()
        val expired = expiry.isNotBlank() && runCatching {
            java.time.Instant.parse(expiry).toEpochMilli() <= System.currentTimeMillis()
        }.getOrDefault(false)
        val registered = preferences.getBoolean("registered", false) && !expired
        val label = preferences.getString("license_label", "STB PLAY license").orEmpty()
        return AppLicenseState(
            statusText = when {
                expired -> "License expired"
                registered -> "Active · $label"
                readLicenseKey() != null -> "License status not checked"
                else -> "No STB PLAY key activated"
            },
            expiryText = expiry.takeIf { it.isNotBlank() }?.let(::formatExpiry).orEmpty(),
            keyHint = preferences.getString("key_hint", "").orEmpty(),
            registered = registered,
            message = if (readLicenseKey() != null && !registered && !expired) "Connect to check the saved key." else ""
        )
    }

    private fun saveLicenseKey(key: String) {
        val encrypted = encrypt(key) ?: throw IllegalStateException("Could not secure the license key.")
        preferences.edit().putString("encrypted_key", encrypted).apply()
    }

    private fun readLicenseKey(): String? =
        preferences.getString("encrypted_key", null)?.let(::decrypt)

    private fun encrypt(value: String): String? = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        Base64.encodeToString(combined, Base64.NO_WRAP)
    }.getOrNull()

    private fun decrypt(value: String): String? = runCatching {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > GCM_IV_LENGTH)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun formatExpiry(value: String): String = runCatching {
        val date = Date.from(java.time.Instant.parse(value))
        "Expires " + java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.MEDIUM,
            java.text.DateFormat.SHORT
        ).format(date)
    }.getOrDefault("Expiry: $value")

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val API_BASE_URL = "https://northamerica-northeast1-stbpplay-platform.cloudfunctions.net/appApi"
        private const val KEY_ALIAS = "stb_play_license_key"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
