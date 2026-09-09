package org.audhd.aha.domain.usagestats

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the most recently used app via [UsageStatsManager].
 *
 * Requires PACKAGE_USAGE_STATS permission (a special app op, not a runtime permission).
 * If the op is not granted, returns null silently — this is an optional context feature.
 */
object LastOpenedProvider {

    data class LastOpened(
        val packageName: String,
        val label: String,
        val lastUsedMs: Long
    )

    /**
     * Returns true if PACKAGE_USAGE_STATS has been granted via App Ops.
     */
    fun hasPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns the most recently used external app (excluding a-Ha itself) within the last 24 hours.
     */
    suspend fun getLastOpened(context: Context): LastOpened? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext null

        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endMs = System.currentTimeMillis()
            val startMs = endMs - 24L * 60 * 60 * 1000

            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMs, endMs)
                ?.filter { it.packageName != context.packageName && it.lastTimeUsed > 0 }
                ?.maxByOrNull { it.lastTimeUsed }
                ?: return@withContext null

            val pm = context.packageManager
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(stats.packageName, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                stats.packageName
            }

            LastOpened(
                packageName = stats.packageName,
                label = label,
                lastUsedMs = stats.lastTimeUsed
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns a human-readable relative time: "just now", "4 min ago", "2h ago", "yesterday".
     */
    fun formatAgo(lastUsedMs: Long): String {
        val diffMs = System.currentTimeMillis() - lastUsedMs
        val diffMin = (diffMs / 60_000).toInt()
        return when {
            diffMin < 1 -> "just now"
            diffMin < 60 -> "${diffMin}min ago"
            diffMin < 1440 -> "${diffMin / 60}h ago"
            else -> "yesterday"
        }
    }
}
