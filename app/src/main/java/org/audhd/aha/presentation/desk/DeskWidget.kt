package org.audhd.aha.presentation.desk

import android.app.Activity
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.audhd.aha.domain.flowmodoro.FlowmodoroEngine
import org.audhd.aha.domain.flowmodoro.FlowmodoroState
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Persistent Desk and Focus Hero Widget.
 *
 * Capabilities:
 * - Real-time glanceable desk clock and date with AMOLED burn-in mitigation.
 * - Flowmodoro count-up timer with proportional break calculator directly in-widget.
 * - Persistent Desk Standby toggle (FLAG_KEEP_SCREEN_ON) keeping screen awake on desk mounts.
 */
@Composable
fun DeskWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context as? Activity }

    // Time State
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    // Persistent Screen-On (Desk Standby)
    var isDeskStandbyActive by remember { mutableStateOf(false) }

    // AMOLED Burn-in protection jitter (shifts by -3 to +3 px randomly)
    var pixelShiftX by remember { mutableIntStateOf(0) }
    var pixelShiftY by remember { mutableIntStateOf(0) }

    // Flowmodoro State
    val flowmodoro = remember { FlowmodoroEngine() }
    val flowState by flowmodoro.state.collectAsState()

    // Live Clock & Burn-in loop
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

        while (isActive) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)

            // Shift pixels slightly if desk standby is on
            if (isDeskStandbyActive && Random.nextFloat() < 0.2f) {
                pixelShiftX = Random.nextInt(-3, 4)
                pixelShiftY = Random.nextInt(-3, 4)
            }

            delay(1000L)
        }
    }

    // Keep screen on when Desk Standby is active
    DisposableEffect(isDeskStandbyActive) {
        if (isDeskStandbyActive) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(pixelShiftX, pixelShiftY) }
            .background(SurfaceCharcoal, RoundedCornerShape(12.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Column {
            // Clock & Standby Indicator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = if (currentTimeString.isEmpty()) "--:--" else currentTimeString,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = currentDateString.toFixationPoint(),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Desk Standby Button
                Box(
                    modifier = Modifier
                        .background(
                            if (isDeskStandbyActive) Color(0xFF1E2D1E) else Color(0xFF161616),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isDeskStandbyActive) Color(0xFF448844) else Color(0xFF262626),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            isDeskStandbyActive = !isDeskStandbyActive
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isDeskStandbyActive) "Desk: Stay Awake" else "Desk: Sleep Normal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDeskStandbyActive) Color(0xFF88DD88) else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1E1E1E), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Flowmodoro Integrated Section
            when (val state = flowState) {
                is FlowmodoroState.Idle -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Flowmodoro Focus".toFixationPoint(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Count up • Earn proportional break (1:5)",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF222222), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF383838), RoundedCornerShape(6.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    flowmodoro.startFocus()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "▶ Start Focus",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                is FlowmodoroState.Focusing -> {
                    val earnedBreak = FlowmodoroEngine.calculateBreakSeconds(state.elapsedSeconds)
                    val breakMins = earnedBreak / 60
                    val breakSecs = earnedBreak % 60

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF44AA44), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "FOCUSING",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.formattedTime,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF88DD88),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Earned break: ${breakMins}m ${breakSecs}s",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF282818), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF555522), RoundedCornerShape(4.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        flowmodoro.stopFocus()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Take Break",
                                    fontSize = 11.sp,
                                    color = Color(0xFFDDDD88),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF281818), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF552222), RoundedCornerShape(4.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        flowmodoro.reset()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                Text(
                                    text = "Stop",
                                    fontSize = 11.sp,
                                    color = Color(0xFFDD8888),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                is FlowmodoroState.OnBreak -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDD9933), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "BREAK",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.formattedTime,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEEBB66),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Rest your executive prefrontal cortex",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    flowmodoro.reset()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Finish Break",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
