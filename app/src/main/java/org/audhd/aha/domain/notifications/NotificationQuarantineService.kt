package org.audhd.aha.domain.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.audhd.aha.data.repository.QuarantineRepository
import org.audhd.aha.data.repository.QuarantinedNotification

/**
 * Android NotificationListenerService that acts as the "Air-Gap" dopamine shield.
 * Suppresses external notifications and queues them into QuarantineRepository
 * for periodic, intentional review instead of real-time interruption.
 */
class NotificationQuarantineService : NotificationListenerService() {

    private lateinit var quarantineRepository: QuarantineRepository

    override fun onCreate() {
        super.onCreate()
        quarantineRepository = QuarantineRepository.getInstance(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // 1. Check if Air-Gap is active
        if (!quarantineRepository.isAirGapEnabled.value) return

        val pkg = sbn.packageName ?: return

        // 2. Do not quarantine ourselves or system core UI
        if (pkg == packageName || pkg == "android") return

        // 3. Do not quarantine ongoing events (active calls, alarms, media playback, navigation)
        val notif = sbn.notification ?: return
        if (sbn.isOngoing) return
        val flags = notif.flags
        if ((flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
            (flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
        ) {
            return
        }

        // 4. Extract notification text contents
        val extras = notif.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        // 5. Lookup human-readable application label
        val appLabel = try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg
        }

        // 6. Suppress from system notification shade if clearable
        if (sbn.isClearable) {
            try {
                cancelNotification(sbn.key)
            } catch (_: Exception) {
                // Ignore if security manager restricts cancellation
            }
        }

        // 7. Store in QuarantineRepository for on-demand monospace digest
        quarantineRepository.addNotification(
            QuarantinedNotification(
                packageName = pkg,
                appLabel = appLabel,
                title = title,
                text = text,
                timestamp = sbn.postTime
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Handled internally by user action in launcher UI
    }
}
