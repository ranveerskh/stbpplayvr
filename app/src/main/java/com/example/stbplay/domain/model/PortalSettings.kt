package com.example.stbplay.domain.model

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

/** Creates a locally administered MAC address.  It is editable before save. */
fun generateStbPlayMac(): String {
    val random = SecureRandom()
    return buildList {
        add("02")
        repeat(5) { add("%02X".format(random.nextInt(256))) }
    }.joinToString(":")
}
