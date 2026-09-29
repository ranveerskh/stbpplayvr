package com.example.stbplay.data

import android.Manifest
import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.example.stbplay.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.DateFormat
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val notes: String = "",
    val publishedAtMillis: Long
) {
    val deadlineAtMillis: Long get() = publishedAtMillis + TimeUnit.DAYS.toMillis(14)
    fun deadlineText(): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(deadlineAtMillis))
    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean = now >= deadlineAtMillis
}

data class UpdateCheckResult(
    val update: UpdateInfo? = null,
    val hasPublishedRelease: Boolean = true
)

/** Only published GitHub Android releases with an APK are offered as updates. */
class UpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()
    private val prefs = context.getSharedPreferences("android_updates", Context.MODE_PRIVATE)

    suspend fun check(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(LATEST_RELEASE_URL).header("Cache-Control", "no-cache").build()
        val json = client.newCall(request).execute().use { response ->
            if (response.code == 404) {
                prefs.edit().remove("available").apply()
                return@withContext UpdateCheckResult(hasPublishedRelease = false)
            }
            if (!response.isSuccessful) error("Update service returned ${response.code}.")
            JSONObject(response.body?.string().orEmpty())
        }
        val version = json.optString("tag_name").trim().removePrefix("v")
        if (version.isBlank() || compareVersions(version, currentVersion) <= 0) {
            prefs.edit().remove("available").apply()
            return@withContext UpdateCheckResult()
        }
        val assets = json.optJSONArray("assets") ?: error("Release has no Android APK.")
        val url = (0 until assets.length()).asSequence()
            .mapNotNull { assets.optJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) && it.optLong("size") > 0 }
            ?.optString("browser_download_url").orEmpty()
        require(url.startsWith(RELEASE_DOWNLOAD_PREFIX) && url.endsWith(".apk", ignoreCase = true)) {
            "Release has no valid Android APK."
        }
        val publishedAt = Instant.parse(json.getString("published_at")).toEpochMilli()
        val info = UpdateInfo(version, url, json.optString("body").trim().take(400), publishedAt)
        prefs.edit().putString("available", JSONObject().put("version", info.version)
            .put("url", info.downloadUrl).put("notes", info.notes)
            .put("publishedAt", info.publishedAtMillis).toString()).apply()
        UpdateCheckResult(update = info)
    }

    fun cachedAvailable(currentVersion: String): UpdateInfo? {
        val cached = prefs.getString("available", null) ?: return null
        return runCatching {
            val json = JSONObject(cached)
            val info = UpdateInfo(json.getString("version"), json.getString("url"),
                json.optString("notes"), json.getLong("publishedAt"))
            info.takeIf { compareVersions(it.version, currentVersion) > 0 &&
                it.downloadUrl.startsWith(RELEASE_DOWNLOAD_PREFIX) && it.publishedAtMillis > 0 }
        }.getOrNull()
    }

    fun shouldPrompt(info: UpdateInfo): Boolean = prefs.getString("prompted", null) != info.version
    fun markPrompted(info: UpdateInfo) { prefs.edit().putString("prompted", info.version).apply() }

    fun notifyIfNew(info: UpdateInfo) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (prefs.getString("notified", null) == info.version) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT))
        val launch = Intent(context, MainActivity::class.java).putExtra("stb_open_updates", true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("STB Play ${info.version} is available")
            .setContentText("Update by ${info.deadlineText()}. Tap to open STB Play.")
            .setContentIntent(pending).setAutoCancel(true).build()
        manager.notify(UPDATE_NOTIFICATION_ID, notification)
        prefs.edit().putString("notified", info.version).apply()
    }

    fun download(info: UpdateInfo): Long {
        require(info.downloadUrl.startsWith(RELEASE_DOWNLOAD_PREFIX))
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("STB Play ${info.version}")
            .setDescription("Downloading update")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "STB-Play-${info.version}.apk")
        val id = context.getSystemService(DownloadManager::class.java).enqueue(request)
        prefs.edit().putLong("pending_download", id).apply()
        return id
    }

    fun pendingDownloadId(): Long = prefs.getLong("pending_download", -1L)

    fun openInstaller(downloadId: Long) {
        val manager = context.getSystemService(DownloadManager::class.java)
        val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId)) ?: return
        val done = cursor.use { it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL }
        if (!done) return
        val uri = manager.getUriForDownloadedFile(downloadId) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            if (prefs.getLong("unknown_sources_prompted", -1L) != downloadId) {
                prefs.edit().putLong("unknown_sources_prompted", downloadId).apply()
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return
        }
        val installer = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(installer)
        prefs.edit().remove("pending_download").remove("unknown_sources_prompted").apply()
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val result = a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 })
            if (result != 0) return result
        }
        return 0
    }

    companion object {
        private const val CHANNEL_ID = "stb_play_updates"
        private const val UPDATE_NOTIFICATION_ID = 1401
        private const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/ranveerskh/stbpplayvr/releases/download/"
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/ranveerskh/stbpplayvr/releases/latest"
    }
}
