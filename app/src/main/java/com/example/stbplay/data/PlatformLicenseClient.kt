package com.example.stbplay.data

import android.content.Context
import android.provider.Settings
import android.util.Base64
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Optional connection to the STB Play registration service. Failures never gate playback. */
class PlatformLicenseClient(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun activate(key: String, portalUrl: String): String = withContext(Dispatchers.IO) {
        val cleanedKey = key.trim()
        require(cleanedKey.isNotEmpty()) { "Enter your activation key." }
        val response = post("/api/register", payload(cleanedKey, portalUrl))
        if (!response.optBoolean("registered")) throw IllegalStateException("The server did not confirm this key.")
        saveKey(cleanedKey)
        "Key activated on this device."
    }

    suspend fun heartbeat(portalUrl: String): String? = withContext(Dispatchers.IO) {
        val key = readKey() ?: return@withContext null
        try {
            val response = post("/api/heartbeat", payload(key, portalUrl))
            if (response.optBoolean("registered") || response.optBoolean("ok")) "Device status synced."
            else null
        } catch (_: Exception) {
            null // Tracking is best-effort and must not interrupt portal playback.
        }
    }

    private fun payload(key: String, portalUrl: String) = JSONObject()
        .put("licenseKey", key)
        .put("deviceId", deviceId())
        .put("platform", "android")
        .put("appVersion", com.example.stbplay.BuildConfig.VERSION_NAME)
        .put("portalHost", portalUrl.trim())

    private fun post(path: String, json: JSONObject): JSONObject {
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder().url("$BASE_URL$path")
            .post(body).header("Accept", "application/json").build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val result = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    403 -> "This key is invalid, inactive, or expired. Check the key in your website dashboard."
                    409 -> "This key has reached its device limit. Remove an old device or increase the limit."
                    else -> result.optString("error").takeIf { it.isNotBlank() }
                        ?: "Could not verify the key (HTTP ${response.code}). Try again."
                }
                throw IllegalStateException(message)
            }
            return result
        }
    }

    private fun deviceId(): String = preferences.getString(DEVICE_ID, null) ?: run {
        val id = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
        preferences.edit().putString(DEVICE_ID, id).apply()
        id
    }

    private fun saveKey(key: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_CIPHER, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun readKey(): String? = runCatching {
        val encrypted = preferences.getString(KEY_CIPHER, null) ?: return null
        val iv = Base64.decode(preferences.getString(KEY_IV, null), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    companion object {
        private const val BASE_URL = "https://northamerica-northeast1-stbpplay-platform.cloudfunctions.net/appApi"
        private const val PREFS = "platform_license_secure"
        private const val DEVICE_ID = "device_id"
        private const val KEY_CIPHER = "license_key_cipher"
        private const val KEY_IV = "license_key_iv"
        private const val KEY_ALIAS = "stb_platform_license_aes"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
