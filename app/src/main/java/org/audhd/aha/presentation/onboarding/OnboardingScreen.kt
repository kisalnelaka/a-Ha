package org.audhd.aha.presentation.onboarding

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audhd.aha.domain.typography.FixationPointParser

data class OnboardingStep(
    val stepNumber: String,
    val title: String,
    val body: String,
    val principle: String
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var currentStepIndex by remember { mutableIntStateOf(0) }

    val steps = remember {
        listOf(
            OnboardingStep(
                stepNumber = "01 / 04",
                title = "Neutrality Over Stimulation",
                body = "Standard launchers are casinos engineered by slot-machine psychologists. Bright red badges and vibrant app icons hijack your amygdala before your executive prefrontal cortex can evaluate your intention.\n\na-Ha eliminates all icon bitmaps. Apps are indexed purely by text.",
                principle = "DOPAMINE-NEUTRAL ARCHITECTURE"
            ),
            OnboardingStep(
                stepNumber = "02 / 04",
                title = "Friction as Compassion",
                body = "ADHD impulsivity operates on instant gratification loops. When opening high-friction apps (like social media), a-Ha introduces a 12-second mindful breathing pacer and asks: 'What was your true intention?'\n\nThis is not a parent lock. You can proceed anytime—you simply pause to choose consciously.",
                principle = "EXECUTIVE PREFRONTAL ENGAGEMENT"
            ),
            OnboardingStep(
                stepNumber = "03 / 04",
                title = "Working Memory Persistence",
                body = "Working memory in AuDHD minds lasts seconds. If you unlock your phone to write an idea, the home screen stimulus wipes it away.\n\nSwipe Down anywhere to summon the Scratchpad. Jot your thought immediately. It persists safely into a zero-latency local text log.",
                principle = "EXTERNALIZED COGNITIVE BUFFER"
            ),
            OnboardingStep(
                stepNumber = "04 / 04",
                title = "Sensory Regulation & Low-Spoon Mode",
                body = "When demand avoidance or sensory overload strikes, switch to Low-Spoon Mode. The launcher contracts to a single gentle path.\n\nBuilt-in DSP Brown Noise provides auditory shielding, while the 45-minute continuous session guardrail gently warms the screen to halt hyperfocus dissociation.",
                principle = "EMPOWERING SENSORY RESTORATION"
            )
        )
    }

    val currentStep = steps[currentStepIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "a-Ha AuDHD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = currentStep.stepNumber,
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Animated step content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_content"
            ) { step ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "[ ${step.principle} ]",
                        fontSize = 11.sp,
                        color = Color(0xFF779977),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = FixationPointParser.parse(step.title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEEEEEE),
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = step.body,
                        fontSize = 15.sp,
                        color = Color(0xFFCCCCCC),
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 24.sp
                    )
                }
            }

            // Footer controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStepIndex > 0) {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            currentStepIndex--
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF161616),
                            contentColor = Color(0xFF888888)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("← Back", fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        if (currentStepIndex < steps.size - 1) {
                            currentStepIndex++
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = Color(0xFFFFFFFF)
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (currentStepIndex < steps.size - 1) "Continue →" else "Enter a-Ha",
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
