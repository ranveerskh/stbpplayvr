package com.example.stbplay.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val notes: String = ""
)

/** Native Android replacement for the desktop installer check. */
class UpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(UPDATE_MANIFEST_URL)
            .header("Cache-Control", "no-cache")
            .build()
        val json = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            JSONObject(response.body?.string().orEmpty())
        }
        val version = json.optString("version").trim()
        // The Windows manifest may contain an EXE URL. Android only accepts a
        // dedicated APK field so it can never download the wrong installer.
        val url = listOf("androidApkUrl", "apkUrl")
            .asSequence()
            .map { json.optString(it).trim() }
            .firstOrNull { it.startsWith("https://", true) || it.startsWith("http://", true) }
            .orEmpty()
        if (version.isBlank() || url.isBlank() || compareVersions(version, currentVersion) <= 0) return@withContext null
        UpdateInfo(version, url, json.optString("notes", json.optString("message")).trim())
    }

    fun download(info: UpdateInfo): Long {
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("STB Play ${info.version}")
            .setDescription("Downloading update")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "STB-Play-${info.version}.apk"
            )
        return context.getSystemService(DownloadManager::class.java).enqueue(request)
    }

    fun openInstaller(downloadId: Long) {
        val manager = context.getSystemService(DownloadManager::class.java)
        val uri = manager.getUriForDownloadedFile(downloadId) ?: return
        val installer = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            context.startActivity(installer)
        }
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.split('.').map { it.toIntOrNull() ?: 0 }
        val max = maxOf(a.size, b.size)
        for (index in 0 until max) {
            val compare = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (compare != 0) return compare
        }
        return 0
    }

    companion object {
        // Same checked-in manifest convention as the Windows source; only a
        // valid APK URL is accepted for Android TV.
        const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/ranveerskh/netplus-player/main/update.json"
    }
}
