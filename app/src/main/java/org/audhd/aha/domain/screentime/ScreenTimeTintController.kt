package org.audhd.aha.domain.screentime

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.ui.graphics.Color

/**
 * Circadian and Hyperfocus Dissociation Guardrail.
 * Uses UsageStatsManager to detect continuous interactive screen sessions.
 * At 45+ minutes of unbroken use, calculates an amber / sepia warmth overlay
 * that gently breaks time blindness without punitive locks.
 */
open class ScreenTimeTintController(private val context: Context? = null) {

    companion object {
        const val HYPERFOCUS_THRESHOLD_MS = 45 * 60 * 1000L // 45 minutes
        const val MAX_TINT_ALPHA = 0.35f
    }

    /**
     * Calculates the continuous session duration in milliseconds.
     * Returns 0 if usage access permission is not granted or device was recently locked.
     */
    fun getContinuousSessionDurationMs(): Long {
        val ctx = context ?: return 0L
        val usageStatsManager = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0L


        val now = System.currentTimeMillis()
        val startTime = now - (3 * 60 * 60 * 1000L) // Look back last 3 hours

        val events = usageStatsManager.queryEvents(startTime, now)
        val event = UsageEvents.Event()

        var lastInteractiveTime = 0L
        var isCurrentlyInteractive = false

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    lastInteractiveTime = event.timeStamp
                    isCurrentlyInteractive = true
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    isCurrentlyInteractive = false
                    lastInteractiveTime = 0L
                }
            }
        }

        return if (isCurrentlyInteractive && lastInteractiveTime > 0L) {
            now - lastInteractiveTime
        } else {
            0L
        }
    }

    /**
     * Computes the ambient tint color to overlay on the UI.
     * Starts at transparent and smoothly interpolates to warm amber after 45 minutes.
     */
    fun computeCircadianTint(sessionDurationMs: Long): Color {
        if (sessionDurationMs < HYPERFOCUS_THRESHOLD_MS) {
            return Color.Transparent
        }

        // Ramp alpha smoothly over the 15 minutes following threshold
        val overDurationMs = sessionDurationMs - HYPERFOCUS_THRESHOLD_MS
        val rampFraction = (overDurationMs.toFloat() / (15 * 60 * 1000f)).coerceIn(0f, 1f)
        val alpha = rampFraction * MAX_TINT_ALPHA

        // Warm amber (#FFB040)
        return Color(0xFFFF, 0xB0, 0x40, (alpha * 255).toInt())
    }
}
