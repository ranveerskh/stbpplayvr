package com.example.stbplay.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.stbplay.MainActivity
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/** Reminds locally from the portal expiry date already returned by the provider. */
class PortalExpiryReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expiry = prefs.getLong(EXPIRY, -1L)
        if (expiry <= 0L || Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()

        val remaining = ceil((expiry - System.currentTimeMillis()).toDouble() / DAY_MS).toInt()
        val due = remaining == 10 || remaining == 9 || remaining in 0..5
        if (!due) return Result.success()
        val today = LocalDate.now().toString()
        if (prefs.getString(LAST_NOTICE_DATE, null) == today) return Result.success()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Portal expiry reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val launch = Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(applicationContext, 1, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val message = if (remaining <= 0) "Your portal subscription expires today."
            else "Your portal subscription expires in $remaining day${if (remaining == 1) "" else "s"}."
        manager.notify(NOTIFICATION_ID, Notification.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("STB Play portal reminder")
            .setContentText(message)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build())
        prefs.edit().putString(LAST_NOTICE_DATE, today).apply()
        return Result.success()
    }

    companion object {
        private const val PREFS = "portal_expiry_reminders"
        private const val EXPIRY = "expiry_millis"
        private const val LAST_NOTICE_DATE = "last_notice_date"
        private const val CHANNEL_ID = "portal_expiry"
        private const val NOTIFICATION_ID = 1909
        private const val DAY_MS = 24L * 60L * 60L * 1000L

        fun setExpiry(context: Context, expiryAtMillis: Long?) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getLong(EXPIRY, -1L) != (expiryAtMillis ?: -1L)) {
                prefs.edit().putString(LAST_NOTICE_DATE, null).apply()
            }
            prefs.edit().putLong(EXPIRY, expiryAtMillis ?: -1L).apply()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "stb_portal_expiry_check_now", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PortalExpiryReminderWorker>().build()
            )
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PortalExpiryReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "stb_portal_expiry_reminders", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
