package org.audhd.aha.presentation.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.audhd.aha.data.security.AIProvider
import org.audhd.aha.data.security.KeystoreManager
import org.audhd.aha.domain.decomposer.TaskDecomposerEngine
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.desk.DeskModeActivity
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary

/**
 * Settings and API Keys configuration sheet.
 * Grants transparent control over BYOK AI providers, default launcher role, and desk standby.
 */
@Composable
fun SettingsSheet(
    keystoreManager: KeystoreManager,
    onOpenOnboarding: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedProvider by remember { mutableStateOf(keystoreManager.getSelectedProvider()) }
    var currentKey by remember { mutableStateOf(keystoreManager.getApiKey(selectedProvider) ?: "") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    val isDefaultLauncher = remember { checkIsDefaultLauncher(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(20.dp),
            color = PureBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Settings".toFixationPoint(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "[ Close ]".toFixationPoint(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onDismiss()
                            }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // Default Launcher Status & Setup
                Text(
                    text = "LAUNCHER ROLE".toFixationPoint(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF779977),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Default Home App",
                                fontSize = 15.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isDefaultLauncher) "[ ACTIVE ]" else "[ NOT DEFAULT ]",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDefaultLauncher) Color(0xFF88CC88) else Color(0xFFDD8888),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (!isDefaultLauncher) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "a-Ha requires Home role to handle gestures and eliminate distracting OEM home screen elements.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    requestDefaultLauncher(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2A2A2A),
                                    contentColor = Color(0xFFFFFFFF)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Set a-Ha as Default Launcher →",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // BYOK AI Credentials Section
                Text(
                    text = "AI INTELLIGENCE (BYOK)".toFixationPoint(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF779977),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Task Decomposer uses direct on-device HTTPS requests to your chosen provider. Keys are hardware-encrypted in Android KeyStore.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Provider Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AIProvider.values().forEach { provider ->
                                val isSelected = provider == selectedProvider
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) Color(0xFF333333) else Color(0xFF181818),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF666666) else Color(0xFF222222),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            selectedProvider = provider
                                            keystoreManager.setSelectedProvider(provider)
                                            currentKey = keystoreManager.getApiKey(provider) ?: ""
                                            saveMessage = null
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = provider.displayName,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // API Key Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedProvider.displayName} API Key",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )

                            val hasKey = !keystoreManager.getApiKey(selectedProvider).isNullOrBlank()
                            Text(
                                text = if (hasKey) "[ KEY CONFIGURED ]" else "[ NO KEY ]",
                                fontSize = 11.sp,
                                color = if (hasKey) Color(0xFF88CC88) else Color(0xFFDD8888),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF101010), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF282828), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = currentKey,
                                onValueChange = { currentKey = it },
                                singleLine = true,
                                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                textStyle = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                cursorBrush = SolidColor(TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Controls Row 1: Key Visibility & Clear
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isKeyVisible) "[ Hide Key ]" else "[ Show Key ]",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clickable { isKeyVisible = !isKeyVisible }
                                    .padding(vertical = 4.dp)
                            )

                            if (currentKey.isNotEmpty()) {
                                Text(
                                    text = "[ Clear Key ]",
                                    fontSize = 11.sp,
                                    color = Color(0xFFDD8888),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            keystoreManager.clearApiKey(selectedProvider)
                                            currentKey = ""
                                            saveMessage = "Key cleared"
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Controls Row 2: Action Buttons (Equal 50/50 Width)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    keystoreManager.setApiKey(selectedProvider, currentKey)
                                    keystoreManager.setSelectedProvider(selectedProvider)
                                    coroutineScope.launch {
                                        saveMessage = "Testing ${selectedProvider.displayName}..."
                                        val result = TaskDecomposerEngine(keystoreManager = keystoreManager)
                                            .testConnection(selectedProvider, currentKey)
                                        saveMessage = if (result.isSuccess) {
                                            "[ VALID ] ${result.getOrNull()}"
                                        } else {
                                            "[ FAILED ] ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B261B),
                                    contentColor = Color(0xFF88DD88)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Test Key",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    keystoreManager.setApiKey(selectedProvider, currentKey)
                                    keystoreManager.setSelectedProvider(selectedProvider)
                                    saveMessage = "${selectedProvider.displayName} key saved securely"
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2A3A2A),
                                    contentColor = Color(0xFFFFFFFF)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Save Key",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }

                        if (saveMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = saveMessage ?: "",
                                fontSize = 11.sp,
                                color = if (saveMessage?.startsWith("[ FAILED ]") == true) Color(0xFFDD8888) else Color(0xFF88CC88),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }



                Spacer(modifier = Modifier.height(24.dp))

                // StandBy Desk Mode Section
                Text(
                    text = "STANDBY & DESK MODE".toFixationPoint(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF999977),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Persistent landscape charging display with Flowmodoro sprint counter, circadian night filter, and OLED burn-in pixel shift.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    onDismiss()
                                    context.startActivity(Intent(context, DeskModeActivity::class.java))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF222822),
                                    contentColor = Color(0xFF88DD88)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Start StandBy",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF222222),
                                    contentColor = Color(0xFFCCCCCC)
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Screen Saver",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Walkthrough & Principles
                Text(
                    text = "NEURODIVERSITY ARCHITECTURE".toFixationPoint(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF779977),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Revisit the 4 core principles: Dopamine Neutrality, Mindful Launch Friction, Working Memory Persistence, and Low-Spoon Regulation.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onDismiss()
                                onOpenOnboarding()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF222222),
                                contentColor = Color(0xFFCCCCCC)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Interactive Walkthrough Guide →", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Checks if a-Ha is currently designated as the default Home app.
 */
fun checkIsDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return resolveInfo?.activityInfo?.packageName == context.packageName
}

/**
 * Launches the system prompt or settings screen to configure the default home app.
 */
fun requestDefaultLauncher(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
    }
}
