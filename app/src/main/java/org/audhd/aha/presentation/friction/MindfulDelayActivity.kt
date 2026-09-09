package org.audhd.aha.presentation.friction

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.content.pm.LauncherApps
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.AHaTheme
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary

/**
 * Activity intercepting launches of designated distraction applications.
 * Enforces a 12-second mindful breathing pause and explicit intentionality capture.
 */
class MindfulDelayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        val targetActivity = intent.getStringExtra(EXTRA_TARGET_ACTIVITY) ?: ""
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: "Target Application"

        if (targetPackage.isEmpty()) {
            finish()
            return
        }

        setContent {
            AHaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureBlack
                ) {
                    MindfulDelayScreen(
                        appLabel = appLabel,
                        onTurnBack = { finish() },
                        onProceed = {
                            launchTarget(targetPackage, targetActivity)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun launchTarget(packageName: String, activityName: String) {
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as? LauncherApps
        try {
            if (activityName.isNotEmpty() && launcherApps != null) {
                launcherApps.startMainActivity(
                    ComponentName(packageName, activityName),
                    Process.myUserHandle(),
                    null,
                    null
                )
                return
            }
        } catch (_: Exception) { }

        val fallback = packageManager.getLaunchIntentForPackage(packageName)
        if (fallback != null) {
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(fallback)
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_ACTIVITY = "extra_target_activity"
        const val EXTRA_APP_LABEL = "extra_app_label"
    }
}

@Composable
fun MindfulDelayScreen(
    appLabel: String,
    onTurnBack: () -> Unit,
    onProceed: () -> Unit
) {
    var secondsRemaining by remember { mutableIntStateOf(12) }
    var intentionText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining -= 1
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = "Pause & Regulate".toFixationPoint(),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Opening $appLabel".toFixationPoint(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Breathing Visualizer with Canvas
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                drawCircle(
                    color = Color(0xFF222222),
                    radius = (size.minDimension / 2) * breathScale,
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (secondsRemaining > 0) "$secondsRemaining" else "Ready",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Breathe".toFixationPoint(),
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        // Intention Prompt
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "What is your intention for opening this app?".toFixationPoint(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (intentionText.isEmpty()) {
                    Text(
                        text = "e.g., Check work message, looking up recipe...".toFixationPoint(),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = intentionText,
                    onValueChange = { intentionText = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Turn Back".toFixationPoint(),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier
                    .clickable(onClick = onTurnBack)
                    .padding(16.dp)
            )

            val canProceed = secondsRemaining == 0
            Text(
                text = "Proceed".toFixationPoint(boldWeight = FontWeight.Bold),
                style = MaterialTheme.typography.labelLarge,
                color = if (canProceed) TextPrimary else TextMuted,
                modifier = Modifier
                    .clickable(enabled = canProceed, onClick = onProceed)
                    .padding(16.dp)
            )
        }
    }
}
