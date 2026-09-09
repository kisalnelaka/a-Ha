package org.audhd.aha.presentation.desk

import android.service.dreams.DreamService
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
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
import org.audhd.aha.presentation.theme.RedNightFilter
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Android native [DreamService] executing Desk Mode (Landscape Charging StandBy).
 *
 * Hardware Optimizations:
 * - Burn-in mitigation: Shifts UI layout offset by [-5dp..+5dp] every 60 seconds.
 * - Circadian sleep hygiene: Applies a red-tint overlay past sunset (after 20:00).
 */
class DeskModeDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

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
            setContent {
                AHaTheme {
                    DeskModeScreen(flowmodoroEngine = flowmodoroEngine)
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
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

@Composable
fun DeskModeScreen(
    flowmodoroEngine: FlowmodoroEngine,
    modifier: Modifier = Modifier
) {
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

    // Circadian night check
    val isNightTime = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour >= 20 || hour < 6
    }

    // Current clock time
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeString = format.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .offset(x = animatedX, y = animatedY)
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Bar: Clock & Circadian indicator
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTimeString,
                    color = if (isNightTime) AmberWarm else TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isNightTime) {
                    Text(
                        text = " • Night Filter Active",
                        color = AmberWarm,
                        fontSize = 14.sp
                    )
                }
            }

            // Central Display: Massive Flowmodoro or Focus State
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = flowState) {
                    is FlowmodoroState.Idle -> {
                        Text(
                            text = "StandBy Active".toFixationPoint(),
                            fontSize = 36.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap to Begin Focus Sprint".toFixationPoint(boldWeight = FontWeight.Bold),
                            fontSize = 18.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable { flowmodoroEngine.startFocus() }
                                .padding(12.dp)
                        )
                    }

                    is FlowmodoroState.Focusing -> {
                        Text(
                            text = state.formattedTime,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightTime) AmberWarm else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Momentum Sprint Active • Tap to Stop".toFixationPoint(),
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable { flowmodoroEngine.stopFocus() }
                                .padding(8.dp)
                        )
                    }

                    is FlowmodoroState.OnBreak -> {
                        Text(
                            text = state.formattedTime,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarm
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Restorative Break • Recharge Autonomic Nervous System".toFixationPoint(),
                            fontSize = 16.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable { flowmodoroEngine.reset() }
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Bottom Bar: Ambient Noise quick toggle & instructions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desk Mode • Nokia G50 Burn-in Guard Active".toFixationPoint(),
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        // Night filter overlay
        if (isNightTime) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RedNightFilter)
            )
        }
    }
}
