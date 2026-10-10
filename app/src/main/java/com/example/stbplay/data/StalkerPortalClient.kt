package com.example.stbplay.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StalkerSession(
    val token: String,
    val random: String,
    val cookie: String = ""
)

class StalkerPortalClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    /** Keep cancellation attached until response parsing finishes, including body reads. */
    private suspend fun <T> executeCancellable(request: Request, consume: (okhttp3.Response) -> T): T =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, error: java.io.IOException) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val result = runCatching { response.use(consume) }
                    if (continuation.isActive) continuation.resumeWith(result)
                }
            })
        }

    suspend fun handshake(
        portalUrl: String,
        macAddress: String
    ): StalkerSession = withContext(Dispatchers.IO) {

        val url = Uri.parse(PortalUrl.apiUrl(portalUrl))
            .buildUpon()
            .appendQueryParameter("type", "stb")
            .appendQueryParameter("action", "handshake")
            .appendQueryParameter("token", "")
            .appendQueryParameter("JsHttpRequest", "1-xml")
            .build()
            .toString()

        val mac = macAddress.uppercase(Locale.ROOT)

        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 MAG254"
            )
            .header("X-User-Agent", "Model: MAG254; Link: Ethernet")
            .header("Referer", PortalUrl.refererUrl(portalUrl))
            .header(
                "Cookie",
                "mac=$mac; stb_lang=en; timezone=America/Toronto"
            )
            .get()
            .build()

        executeCancellable(request) { response ->

            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IllegalStateException("Portal HTTP ${response.code}")
            }

            val js = JSONObject(body).optJSONObject("js")
                ?: throw IllegalStateException("Invalid Stalker response")

            val token = js.optString("token").trim()
            val random = js.optString("random").trim()

            if (token.isBlank()) {
                throw IllegalStateException("Portal authentication failed")
            }

            val responseCookies = response.headers.values("Set-Cookie")
                .mapNotNull { it.substringBefore(';').trim().takeIf { value -> value.isNotBlank() } }
                .joinToString("; ")

            StalkerSession(token, random, responseCookies)
        }
    }

    suspend fun call(
        portalUrl: String,
        macAddress: String,
        session: StalkerSession,
        type: String,
        action: String,
        parameters: Map<String, String> = emptyMap()
    ): JSONObject = withContext(Dispatchers.IO) {

        val builder = Uri.parse(PortalUrl.apiUrl(portalUrl))
            .buildUpon()
            .appendQueryParameter("type", type)
            .appendQueryParameter("action", action)
            .appendQueryParameter("JsHttpRequest", "1-xml")

        parameters.forEach { (key, value) ->
            builder.appendQueryParameter(key, value)
        }

        val mac = macAddress.uppercase(Locale.ROOT)

        val request = Request.Builder()
            .url(builder.build().toString())
            .header(
                "User-Agent",
                "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 MAG254"
            )
            .header("X-User-Agent", "Model: MAG254; Link: Ethernet")
            .header("Referer", PortalUrl.refererUrl(portalUrl))
            .header("Authorization", "Bearer ${session.token}")
            .header("Cookie", buildCookieHeader(session, mac))
            .get()
            .build()

        executeCancellable(request) { response ->

            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IllegalStateException("Portal HTTP ${response.code}")
            }

            JSONObject(body)
        }
    }

    fun artworkRequestHeaders(
        portalUrl: String,
        macAddress: String,
        session: StalkerSession
    ): Map<String, String> {
        return mapOf(
            "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 MAG254",
            "X-User-Agent" to "Model: MAG254; Link: Ethernet",
            "Referer" to PortalUrl.refererUrl(portalUrl),
            "Authorization" to "Bearer ${session.token}",
            "Cookie" to buildCookieHeader(session, macAddress.uppercase(Locale.ROOT))
        )
    }

    private fun buildCookieHeader(session: StalkerSession, mac: String): String {
        val cookies = session.cookie
            .split(';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()

        fun has(name: String): Boolean = cookies.any { it.startsWith("$name=", ignoreCase = true) }
        if (!has("mac")) cookies.add("mac=$mac")
        if (!has("stb_lang")) cookies.add("stb_lang=en")
        if (!has("timezone")) cookies.add("timezone=America/Toronto")
        if (!has("token")) cookies.add("token=${session.token}")
        return cookies.joinToString("; ")
    }
}
