package org.audhd.aha.domain.calendar

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the next upcoming calendar event using [CalendarContract].
 *
 * Requires READ_CALENDAR permission. Returns null if permission not granted, no events exist,
 * or any exception occurs. This is intentionally a best-effort glance, never a hard dependency.
 */
object CalendarGlanceProvider {

    data class NextEvent(
        val title: String,
        val startMs: Long,
        val isAllDay: Boolean
    )

    /**
     * Queries the system calendar for the next upcoming event within 24 hours.
     */
    suspend fun getNextEvent(context: Context): NextEvent? = withContext(Dispatchers.IO) {
        return@withContext try {
            val resolver: ContentResolver = context.contentResolver
            val nowMs = System.currentTimeMillis()
            val endMs = nowMs + 24L * 60 * 60 * 1000 // 24h window

            val uri: Uri = CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.ALL_DAY
            )
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DELETED} = 0"
            val selectionArgs = arrayOf(nowMs.toString(), endMs.toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT 1"

            val cursor: Cursor? = resolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.use {
                if (it.moveToFirst()) {
                    val title = it.getString(0) ?: "Unnamed Event"
                    val start = it.getLong(1)
                    val allDay = it.getInt(2) == 1
                    NextEvent(title = title.take(60), startMs = start, isAllDay = allDay)
                } else null
            }
        } catch (_: SecurityException) {
            null // Permission not granted
        } catch (_: Exception) {
            null // Any other error: fail silently
        }
    }

    /**
     * Formats the event start time relative to now: "in 23 min", "in 2h 15m", "tomorrow".
     */
    fun formatRelativeTime(startMs: Long, isAllDay: Boolean): String {
        if (isAllDay) return "all day"
        val diffMs = startMs - System.currentTimeMillis()
        if (diffMs < 0) return "now"
        val diffMin = (diffMs / 60_000).toInt()
        return when {
            diffMin < 1 -> "now"
            diffMin < 60 -> "in ${diffMin}min"
            diffMin < 1440 -> {
                val h = diffMin / 60
                val m = diffMin % 60
                if (m == 0) "in ${h}h" else "in ${h}h ${m}m"
            }
            else -> "tomorrow"
        }
    }
}
