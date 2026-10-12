package com.example.stbplay.data

import com.example.stbplay.data.model.LoginResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PortalLoginRetryTest {
    @Test fun transientFailureRetriesAndStopsOnSuccess() = runBlocking {
        var calls = 0
        val pauses = mutableListOf<Long>()
        val result = retryPortalLogin(pause = { pauses.add(it) }) {
            calls++
            LoginResponse(calls == 3, "Network timeout")
        }
        assertTrue(result.success)
        assertEquals(3, calls)
        assertEquals(listOf(2000L, 4000L), pauses)
    }
    @Test fun retriesAreBounded() = runBlocking {
        var calls = 0
        val result = retryPortalLogin(pause = {}) { calls++; LoginResponse(false, "Network timeout") }
        assertFalse(result.success)
        assertEquals(4, calls)
    }
    @Test fun permanentFailureDoesNotRetry() = runBlocking {
        var calls = 0
        retryPortalLogin(pause = {}) { calls++; LoginResponse(false, "Portal access denied.") }
        assertEquals(1, calls)
    }
    @Test fun retryWindowCancelsAnUnresponsiveAttempt() = runBlocking {
        var calls = 0
        val result = retryPortalLogin(pause = {}, retryWindowMillis = 30L) {
            calls++
            if (calls == 1) LoginResponse(false, "Network timeout") else kotlinx.coroutines.awaitCancellation()
        }
        assertFalse(result.success)
        assertEquals(2, calls)
    }
}
