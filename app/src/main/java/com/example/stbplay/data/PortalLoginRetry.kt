package com.example.stbplay.data

import com.example.stbplay.data.model.LoginResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

internal fun isRetryablePortalFailure(message: String?): Boolean {
    val text = message.orEmpty().lowercase()
    return listOf("access denied", "authentication failed", "endpoint not found", "expired", "blocked", "enter a portal", "enter the device").none(text::contains)
}

/** One initial attempt, then at most three retries within a 30 second window. */
internal suspend fun retryPortalLogin(
    pause: suspend (Long) -> Unit = { delay(it) },
    login: suspend () -> LoginResponse
): LoginResponse {
    val first = login()
    if (first.success || !isRetryablePortalFailure(first.errorMessage)) return first
    var last = first
    return withTimeoutOrNull(30_000L) {
        for (wait in listOf(2_000L, 4_000L, 6_000L)) {
            pause(wait)
            last = login()
            if (last.success || !isRetryablePortalFailure(last.errorMessage)) return@withTimeoutOrNull last
        }
        last
    } ?: last
}
