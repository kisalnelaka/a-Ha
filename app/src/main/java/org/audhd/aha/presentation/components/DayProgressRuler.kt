package org.audhd.aha.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary
import java.util.Calendar
import java.util.Locale

/**
 * Visual Day Progress Ruler engineered for AuDHD "Time-Blindness" anchoring.
 * Converts abstract numerical time into a visceral, spatial progression of waking hours (06:00 - 22:00).
 */
@Composable
fun DayProgressRuler(
    modifier: Modifier = Modifier
) {
    val stats = remember { calculateDayProgress() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCharcoal)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAYLIGHT ANCHOR",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${stats.percent}% ELAPSED",
                color = if (stats.percent > 80) Color(0xFFFFB74D) else Color(0xFF88DD88),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Spatial Waking Hours Bar (06:00 -> 22:00)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF181818))
                .border(1.dp, Color(0xFF282828), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = stats.fraction)
                    .fillMaxHeight()
                    .background(Color(0xFFBEFFC8))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "06:00 WAKE",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp
            )
            Text(
                text = String.format(Locale.US, "%.1fH WAKING REMAINING", stats.hoursRemaining),
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "22:00 REST",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp
            )
        }
    }
}

data class DayProgressStats(
    val fraction: Float,
    val percent: Int,
    val hoursRemaining: Float
)

fun calculateDayProgress(calendar: Calendar = Calendar.getInstance()): DayProgressStats {
    val currentMinute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val wakeMinute = 6 * 60  // 06:00
    val restMinute = 22 * 60 // 22:00
    val totalWakingMinutes = restMinute - wakeMinute // 960 minutes

    val elapsedMinutes = (currentMinute - wakeMinute).coerceIn(0, totalWakingMinutes)
    val fraction = (elapsedMinutes.toFloat() / totalWakingMinutes.toFloat()).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()

    val minutesRemaining = (restMinute - currentMinute).coerceAtLeast(0)
    val hoursRemaining = minutesRemaining / 60.0f

    return DayProgressStats(
        fraction = fraction,
        percent = percent,
        hoursRemaining = hoursRemaining
    )
}
