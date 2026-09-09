package org.audhd.aha.presentation.desk

import android.service.dreams.DreamService
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import org.audhd.aha.domain.flowmodoro.FlowmodoroEngine
import org.audhd.aha.domain.flowmodoro.FlowmodoroState
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.AHaTheme
import org.audhd.aha.presentation.theme.AmberWarm
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Android native [DreamService] executing Desk Mode (Charging StandBy Screensaver).
 *
 * Implements [LifecycleOwner], [ViewModelStoreOwner], and [SavedStateRegistryOwner] to satisfy
 * Jetpack Compose host requirements in non-Activity window environments.
 *
 * Hardware Optimizations:
 * - Burn-in mitigation: Shifts UI layout offset by [-5dp..+5dp] every 60 seconds.
 * - Circadian sleep hygiene: Supports on-demand and sunset-triggered amber warm filter.
 */
class DeskModeDreamService : DreamService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val myViewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = myViewModelStore

    private val flowmodoroEngine = FlowmodoroEngine()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DeskModeDreamService)
            setViewTreeSavedStateRegistryOwner(this@DeskModeDreamService)
            setViewTreeViewModelStoreOwner(this@DeskModeDreamService)
            setContent {
                AHaTheme {
                    DeskModeScreen(
                        flowmodoroEngine = flowmodoroEngine,
                        onExit = { finish() }
                    )
                }
            }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        flowmodoroEngine.reset()
        myViewModelStore.clear()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

/**
 * Core StandBy / Desk Mode display composable.
 * Usable both within [DeskModeDreamService] and [DeskModeActivity].
 */
@Composable
fun DeskModeScreen(
    flowmodoroEngine: FlowmodoroEngine,
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null
) {
    val view = LocalView.current
    val flowState by flowmodoroEngine.state.collectAsState()

    // Burn-in prevention offset state (-5dp to +5dp shifted every 60s)
    var offsetX by remember { mutableStateOf(0.dp) }
    var offsetY by remember { mutableStateOf(0.dp) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            offsetX = Random.nextInt(-5, 6).dp
            offsetY = Random.nextInt(-5, 6).dp
        }
    }

    val animatedX by animateDpAsState(targetValue = offsetX, animationSpec = tween(1000), label = "burnInX")
    val animatedY by animateDpAsState(targetValue = offsetY, animationSpec = tween(1000), label = "burnInY")

    // Circadian night check (default active between 20:00 and 06:00, or user toggle)
    val autoNight = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour >= 20 || hour < 6
    }
    var isNightFilterActive by remember { mutableStateOf(autoNight) }

    // Current clock and date strings
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now).uppercase()
            delay(1000)
        }
    }

    val activeColor = if (isNightFilterActive) AmberWarm else TextPrimary
    val secondaryColor = if (isNightFilterActive) Color(0xFFD49B55) else TextSecondary
    val mutedColor = if (isNightFilterActive) Color(0xFF886030) else TextMuted

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .offset(x = animatedX, y = animatedY)
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status & Affordance Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // StandBy Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isNightFilterActive) AmberWarm else Color(0xFF88DD88), CircleShape)
                    )
                    Text(
                        text = "STANDBY ACTIVE",
                        color = secondaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circadian Night Filter Toggle
                    Box(
                        modifier = Modifier
                            .background(
                                if (isNightFilterActive) Color(0xFF332010) else Color(0xFF141414),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (isNightFilterActive) Color(0xFF885020) else Color(0xFF262626),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                isNightFilterActive = !isNightFilterActive
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isNightFilterActive) "NIGHT FILTER: ON" else "NIGHT FILTER: OFF",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightFilterActive) AmberWarm else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Exit button (if launched as an Activity)
                    if (onExit != null) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    onExit()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "[ EXIT ✕ ]",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Central Massive Display: Brutalist Clock & Flowmodoro
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = flowState) {
                    is FlowmodoroState.Idle -> {
                        Text(
                            text = if (currentTimeString.isEmpty()) "--:--" else currentTimeString,
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            text = currentDateString.toFixationPoint(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF161E16), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF2E482E), RoundedCornerShape(6.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    flowmodoroEngine.startFocus()
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "[ START FOCUS SPRINT ]",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNightFilterActive) AmberWarm else Color(0xFF88EE88),
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    is FlowmodoroState.Focusing -> {
                        Text(
                            text = state.formattedTime,
                            fontSize = 84.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "MOMENTUM SPRINT ACTIVE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF261818), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF552828), RoundedCornerShape(6.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    flowmodoroEngine.stopFocus()
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "[ STOP SPRINT & REST ]",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEE8888),
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    is FlowmodoroState.OnBreak -> {
                        Text(
                            text = state.formattedTime,
                            fontSize = 84.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarm,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "RESTORATIVE RECHARGE BREAK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(6.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    flowmodoroEngine.reset()
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "[ FINISH BREAK ]",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Bottom Bar: OLED Burn-in Guard Telemetry
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DESK STANDBY • OLED BURN-IN GUARD ACTIVE (±5PX / 60S)",
                    fontSize = 11.sp,
                    color = mutedColor,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
