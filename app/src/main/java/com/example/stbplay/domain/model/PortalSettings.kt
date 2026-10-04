package com.example.stbplay.domain.model

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * A portal profile saved on this device.  The id is deliberately local: the
 * portal only receives the URL and MAC address supplied by its owner.
 */
data class PortalSettings(
    val name: String = "",
    val url: String = "",
    val mac: String = "",
    val pin: String = "",
    val id: String = ""
) {
    fun withStableId(): PortalSettings =
        if (id.isNotBlank()) this else copy(id = UUID.randomUUID().toString())

    fun normalized(): PortalSettings = copy(
        name = name.trim(),
        url = url.trim().trimEnd('/'),
        mac = mac.trim().uppercase(),
        pin = pin.filter(Char::isDigit).take(8)
    )
}

/** Creates an editable locally administered MAC address, stable per Android ID when supplied. */
fun generateStbPlayMac(androidId: String? = null): String {
    val suffix = if (androidId.isNullOrBlank()) {
        ByteArray(5).also { SecureRandom().nextBytes(it) }
    } else {
        MessageDigest.getInstance("SHA-256")
            .digest("stbplay:${androidId.trim()}".toByteArray(Charsets.UTF_8))
            .copyOfRange(0, 5)
    }
    return (byteArrayOf(0x02.toByte()) + suffix).joinToString(":") { byte ->
        "%02X".format(byte.toInt() and 0xFF)
    }
}
