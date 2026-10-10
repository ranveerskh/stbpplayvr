package com.example.stbplay.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class ViewerProfile(val id: String, val name: String, val age: Int, val avatar: String = "🙂", val pinHash: String = "", val allowedLiveCategories: Set<String> = emptySet(), val allowedVodCategories: Set<String> = emptySet(), val approvalPortalKey: String = "") {
    val isKids: Boolean get() = age < 18
}

internal fun encodeViewerPin(pin: String): String {
    require(pin.length in 4..8 && pin.all(Char::isDigit))
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pin.toCharArray(), salt, 60_000, 256)).encoded
    return salt.toHex() + ":" + hash.toHex()
}
internal fun verifyViewerPin(pin: String, encoded: String): Boolean = runCatching {
    val parts = encoded.split(':')
    val salt = parts[0].hexBytes()
    val expected = parts[1].hexBytes()
    val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pin.toCharArray(), salt, 60_000, 256)).encoded
    MessageDigest.isEqual(actual, expected)
}.getOrDefault(false)
private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
private fun String.hexBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
