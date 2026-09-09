package org.audhd.aha.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import java.util.concurrent.CopyOnWriteArrayList

data class BatchedNotification(
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isQuarantined: Boolean = false
)

/**
 * AuDHD sensory regulation notification service.
 * Intercepts relentless notification chimes and batches non-critical notifications
 * into 3 calm intervals per day (09:00, 13:00, 18:00).
 * Supports zero-delay VIP bypass and emotional quarantine for high-stress triggers.
 */
class NotificationBatchService : NotificationListenerService() {

    companion object {
        private const val CHANNEL_ID = "aha_calm_digest"
        private const val DIGEST_NOTIFICATION_ID = 42001

        val batchedNotifications = CopyOnWriteArrayList<BatchedNotification>()

        // Stress words that trigger emotional quarantine
        private val STRESS_TRIGGERS = listOf(
            "urgent", "alert", "past due", "immediately", "warning", "action required", "critical", "overdue"
        )

        // VIP whitelist for immediate bypass (e.g., dialer, telecom emergency)
        val vipPackages = mutableSetOf(
            "com.google.android.dialer",
            "com.android.server.telecom",
            "com.android.phone"
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName
        // Do not intercept our own launcher notifications
        if (pkg == packageName) return

        val notification = sbn.notification ?: return

        // Ongoing notifications (music playback, turn-by-turn navigation, active calls) bypass batching
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing) return

        // VIP bypass (calls, telecom) passes through instantly
        if (vipPackages.contains(pkg)) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val lowerContent = "$title $text".lowercase()
        val isQuarantined = STRESS_TRIGGERS.any { lowerContent.contains(it) }

        val batched = BatchedNotification(
            id = sbn.key,
            packageName = pkg,
            title = title,
            text = text,
            isQuarantined = isQuarantined
        )
        batchedNotifications.add(batched)

        // Dismiss the intrusive pop-up notification from the status bar
        try {
            cancelNotification(sbn.key)
        } catch (_: Exception) {
            // Safe fallback if permission is limited
        }
    }

    fun dispatchCalmDigest() {
        if (batchedNotifications.isEmpty()) return

        val count = batchedNotifications.size
        val quarantinedCount = batchedNotifications.count { it.isQuarantined }

        val summary = if (quarantinedCount > 0) {
            "$count updates batched ($quarantinedCount quarantined for calm review)."
        } else {
            "$count updates batched for calm review."
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("a-Ha Calm Digest")
            .setContentText(summary)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(DIGEST_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Calm Notification Digest",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Low-dopamine scheduled batch summaries for AuDHD attention preservation"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
