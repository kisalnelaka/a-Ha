package org.audhd.aha.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class QuarantinedNotification(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages the Notification Air-Gap: holds intercepted notifications in memory/cache
 * so the user is shielded from real-time variable-ratio dopamine pings.
 */
class QuarantineRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("aha_quarantine", Context.MODE_PRIVATE)

    private val _isAirGapEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AIR_GAP_ENABLED, false)
    )
    val isAirGapEnabled: StateFlow<Boolean> = _isAirGapEnabled.asStateFlow()

    private val _notifications = MutableStateFlow<List<QuarantinedNotification>>(emptyList())
    val notifications: StateFlow<List<QuarantinedNotification>> = _notifications.asStateFlow()

    fun setAirGapEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AIR_GAP_ENABLED, enabled).apply()
        _isAirGapEnabled.value = enabled
    }

    fun addNotification(notification: QuarantinedNotification) {
        val current = _notifications.value.toMutableList()
        // Deduplicate or insert at top (newest first)
        current.removeAll { it.packageName == notification.packageName && it.title == notification.title && it.text == notification.text }
        current.add(0, notification)
        // Cap to 50 most recent notifications to avoid memory bloat
        if (current.size > 50) {
            current.removeAt(current.lastIndex)
        }
        _notifications.value = current
    }

    fun dismissNotification(id: String) {
        val current = _notifications.value.toMutableList()
        current.removeAll { it.id == id }
        _notifications.value = current
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    companion object {
        private const val KEY_AIR_GAP_ENABLED = "air_gap_enabled"

        @Volatile
        private var INSTANCE: QuarantineRepository? = null

        fun getInstance(context: Context): QuarantineRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QuarantineRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
