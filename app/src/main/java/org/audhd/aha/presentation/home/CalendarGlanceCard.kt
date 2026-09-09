package org.audhd.aha.presentation.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.audhd.aha.domain.calendar.CalendarGlanceProvider
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextSecondary

/**
 * Displays the next upcoming calendar event within 24 hours.
 *
 * Permission behaviour:
 * - If READ_CALENDAR is already granted, loads immediately on composition.
 * - If not granted, renders a single-tap request prompt. After grant, auto-loads.
 * - If denied, renders silent "No calendar access" fallback with no retry pressure.
 */
@Composable
fun CalendarGlanceCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var event by remember { mutableStateOf<CalendarGlanceProvider.NextEvent?>(null) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) permissionDenied = true
    }

    // Auto-load once permission is confirmed
    LaunchedEffect(permissionGranted) {
        if (permissionGranted && !loaded) {
            loaded = true
            event = CalendarGlanceProvider.getNextEvent(context)
        }
    }

    if (permissionDenied) return // Silent — no calendar access, show nothing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E2836), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (!permissionGranted) {
            // Tap to grant
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CALENDAR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to enable calendar glance",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "[ Allow ]",
                    fontSize = 11.sp,
                    color = Color(0xFF6699AA),
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column {
                Text(
                    text = "NEXT EVENT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (event != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event!!.title.toFixationPoint(),
                            fontSize = 13.sp,
                            color = Color(0xFFBBCCDD),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = CalendarGlanceProvider.formatRelativeTime(
                                event!!.startMs,
                                event!!.isAllDay
                            ),
                            fontSize = 11.sp,
                            color = Color(0xFF6699AA),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else if (loaded) {
                    Text(
                        text = "No events in the next 24h",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = "Loading...",
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
