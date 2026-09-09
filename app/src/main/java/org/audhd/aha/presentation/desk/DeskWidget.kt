package org.audhd.aha.presentation.desk

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
 * - High-contrast glanceable desk clock and date with fixation point typography.
 * - Flowmodoro hyperfocus stopwatch with proportional break calculator (1:5).
 * - AMOLED anti-burn-in spatial jitter (shifts by +/-3 px every 60s).
 * - Persistent Desk Standby toggle keeping display awake on desktop docks.
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
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

        while (isActive) {
            val nowMillis = System.currentTimeMillis()
            val now = Date(nowMillis)
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now).uppercase()

            if (isDeskStandbyActive && Random.nextFloat() < 0.2f) {
                pixelShiftX = Random.nextInt(-3, 4)
                pixelShiftY = Random.nextInt(-3, 4)
            }

            // Power-efficiency optimization: Align wakeups to the exact minute boundary when idle,
            // dropping CPU wakeups from 60/min to 1/min. Standby mode updates every 15s for jitter.
            val millisUntilNextMinute = 60_000L - (nowMillis % 60_000L)
            val sleepDuration = if (isDeskStandbyActive) 15_000L else millisUntilNextMinute.coerceIn(500L, 60_000L)
            delay(sleepDuration)
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
            .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(18.dp)
    ) {
        Column {
            // Clock & Standby Indicator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Clickable Clock & Date (Tap to enter StandBy Desk Mode)
                Column(
                    modifier = Modifier.clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        context.startActivity(Intent(context, DeskModeActivity::class.java))
                    }
                ) {
                    Text(
                        text = if (currentTimeString.isEmpty()) "--:--" else currentTimeString,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1.5).sp
                    )
                    Text(
                        text = currentDateString.toFixationPoint(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Desk Standby Button
                Box(
                    modifier = Modifier
                        .background(
                            if (isDeskStandbyActive) Color(0xFF1B261B) else Color(0xFF141414),
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (isDeskStandbyActive) Color(0xFF386038) else Color(0xFF262626),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            isDeskStandbyActive = !isDeskStandbyActive
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isDeskStandbyActive) Color(0xFF88DD88) else Color(0xFF555555),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isDeskStandbyActive) "STANDBY: AWAKE" else "STANDBY: OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeskStandbyActive) Color(0xFF88DD88) else TextMuted,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1C1C1C), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Flowmodoro Integrated Section
            when (val state = flowState) {
                is FlowmodoroState.Idle -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "FLOW TIMER".toFixationPoint(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Count-up • 1:5 recovery",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF202020), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF383838), RoundedCornerShape(4.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    flowmodoro.startFocus()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "[ START FLOW ]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF88DD88), CircleShape)
                                )
                                Text(
                                    text = "IN FLOW",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF88DD88),
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = state.formattedTime,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
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
                                    .background(Color(0xFF222018), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF484024), RoundedCornerShape(4.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        flowmodoro.stopFocus()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "[ REST ]",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD8C070),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF221616), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF482424), RoundedCornerShape(4.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        flowmodoro.reset()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "[ STOP ]",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD88080),
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFDFAB55), CircleShape)
                                )
                                Text(
                                    text = "RECOVERY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDFAB55),
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = state.formattedTime,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDFAB55),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Autonomic nervous restoration",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF202020), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF383838), RoundedCornerShape(4.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    flowmodoro.reset()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "[ DISMISS ]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
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
