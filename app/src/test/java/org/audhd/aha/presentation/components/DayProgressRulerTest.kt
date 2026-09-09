package org.audhd.aha.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DayProgressRulerTest {

    @Test
    fun testWakeTimeProgress() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
        }
        val stats = calculateDayProgress(cal)
        assertEquals(0f, stats.fraction, 0.001f)
        assertEquals(0, stats.percent)
        assertEquals(16.0f, stats.hoursRemaining, 0.01f)
    }

    @Test
    fun testMiddayProgress() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14) // 8 hours into 16h waking day = 50%
            set(Calendar.MINUTE, 0)
        }
        val stats = calculateDayProgress(cal)
        assertEquals(0.5f, stats.fraction, 0.001f)
        assertEquals(50, stats.percent)
        assertEquals(8.0f, stats.hoursRemaining, 0.01f)
    }

    @Test
    fun testRestTimeProgress() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
        }
        val stats = calculateDayProgress(cal)
        assertEquals(1.0f, stats.fraction, 0.001f)
        assertEquals(100, stats.percent)
        assertEquals(0.0f, stats.hoursRemaining, 0.01f)
    }

    @Test
    fun testNightBoundaryClamping() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2) // 02:00 AM
            set(Calendar.MINUTE, 0)
        }
        val stats = calculateDayProgress(cal)
        assertEquals(0f, stats.fraction, 0.001f)
        assertEquals(0, stats.percent)
        assertTrue(stats.hoursRemaining >= 0f)
    }
}
