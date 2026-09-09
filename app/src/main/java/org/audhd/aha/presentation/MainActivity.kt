package org.audhd.aha.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

import org.audhd.aha.data.local.AppDatabase
import org.audhd.aha.data.model.AppInfo
import org.audhd.aha.data.repository.AppRepository
import org.audhd.aha.data.repository.ScratchpadRepository
import org.audhd.aha.data.repository.TaskRepository
import org.audhd.aha.data.repository.WallpaperRepository
import org.audhd.aha.data.security.KeystoreManager
import org.audhd.aha.domain.audio.NoiseType
import org.audhd.aha.domain.audio.SoundScapeEngine
import org.audhd.aha.domain.decomposer.TaskDecomposerEngine
import org.audhd.aha.domain.screentime.ScreenTimeTintController
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.desk.DeskWidget
import org.audhd.aha.presentation.drawer.AppDrawer
import org.audhd.aha.presentation.onboarding.OnboardingScreen
import org.audhd.aha.presentation.quest.QuestBoard
import org.audhd.aha.presentation.scratchpad.ScratchpadDialog
import org.audhd.aha.presentation.settings.SettingsSheet
import org.audhd.aha.presentation.settings.checkIsDefaultLauncher
import org.audhd.aha.presentation.settings.requestDefaultLauncher
import org.audhd.aha.presentation.theme.AHaTheme
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary
import org.audhd.aha.presentation.wallpaper.WallpaperPickerSheet

/**
 * Root Launcher Activity orchestrating the AuDHD executive functioning interface.
 */
class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var scratchpadRepository: ScratchpadRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var wallpaperRepository: WallpaperRepository
    private lateinit var soundScapeEngine: SoundScapeEngine
    private lateinit var screenTimeTintController: ScreenTimeTintController
    private lateinit var keystoreManager: KeystoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appDatabase = AppDatabase.getInstance(applicationContext)
        keystoreManager = KeystoreManager(applicationContext)
        val decomposerEngine = TaskDecomposerEngine(appDatabase.taskDao(), keystoreManager)

        appRepository = AppRepository(applicationContext)
        scratchpadRepository = ScratchpadRepository(applicationContext)
        taskRepository = TaskRepository(appDatabase.taskDao(), decomposerEngine)
        wallpaperRepository = WallpaperRepository(applicationContext)
        soundScapeEngine = SoundScapeEngine()
        screenTimeTintController = ScreenTimeTintController(applicationContext)

        setContent {
            AHaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherRoot(
                        appRepository = appRepository,
                        scratchpadRepository = scratchpadRepository,
                        taskRepository = taskRepository,
                        wallpaperRepository = wallpaperRepository,
                        soundScapeEngine = soundScapeEngine,
                        screenTimeTintController = screenTimeTintController,
                        keystoreManager = keystoreManager,
                        onCallClicked = ::launchDialer,
                        onTextClicked = ::launchSms,
                        onNavigateClicked = ::launchNavigation
                    )
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        soundScapeEngine.stop()
    }


    private fun launchDialer() {
        val dialIntent = Intent(Intent.ACTION_DIAL)
        if (dialIntent.resolveActivity(packageManager) != null) {
            startActivity(dialIntent)
        }
    }

    private fun launchSms() {
        val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
        if (smsIntent.resolveActivity(packageManager) != null) {
            startActivity(smsIntent)
        }
    }

    private fun launchNavigation() {
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        }
    }
}

@Composable
fun LauncherRoot(
    appRepository: AppRepository,
    scratchpadRepository: ScratchpadRepository,
    taskRepository: TaskRepository,
    wallpaperRepository: WallpaperRepository,
    soundScapeEngine: SoundScapeEngine,
    screenTimeTintController: ScreenTimeTintController,
    keystoreManager: KeystoreManager,
    onCallClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onNavigateClicked: () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val apps by appRepository.installedApps.collectAsState()
    val tasks by taskRepository.getActiveTasks().collectAsState(initial = emptyList())

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aha_prefs", Context.MODE_PRIVATE) }
    val hasCompletedOnboarding = remember { prefs.getBoolean("has_completed_onboarding", false) }

    var isDrawerOpen by remember { mutableStateOf(false) }
    var isScratchpadOpen by remember { mutableStateOf(false) }
    var isQuestBoardOpen by remember { mutableStateOf(false) }
    var isWallpaperPickerOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isOnboardingOpen by remember { mutableStateOf(!hasCompletedOnboarding) }
    var isLowSpoonMode by remember { mutableStateOf(false) }
    var currentNoise by remember { mutableStateOf<NoiseType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Circadian / Hyperfocus Continuous Session Guardrail
    val continuousDuration = remember { screenTimeTintController.getContinuousSessionDurationMs() }
    val tintColor = remember(continuousDuration) {
        screenTimeTintController.computeCircadianTint(continuousDuration)
    }

    LaunchedEffect(Unit) {
        appRepository.refreshApps()
    }

    BackHandler(enabled = isDrawerOpen || isScratchpadOpen || isQuestBoardOpen || isWallpaperPickerOpen || isOnboardingOpen || isSettingsOpen) {
        when {
            isSettingsOpen -> isSettingsOpen = false
            isOnboardingOpen -> {
                prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                isOnboardingOpen = false
            }
            isWallpaperPickerOpen -> isWallpaperPickerOpen = false
            isQuestBoardOpen -> isQuestBoardOpen = false
            isScratchpadOpen -> isScratchpadOpen = false
            isDrawerOpen -> {
                isDrawerOpen = false
                searchQuery = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (!isDrawerOpen && !isScratchpadOpen && !isQuestBoardOpen && !isWallpaperPickerOpen && !isOnboardingOpen && !isSettingsOpen) {
                        // Downward drag triggers Working Memory Scratchpad
                        if (dragAmount > 35) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            isScratchpadOpen = true
                        }
                        // Upward drag triggers Text-Only App Drawer
                        else if (dragAmount < -35) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            isDrawerOpen = true
                        }
                    }
                }
            }
    ) {
        when {
            isOnboardingOpen -> {
                OnboardingScreen(
                    onComplete = {
                        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                        isOnboardingOpen = false
                    }
                )
            }
            isSettingsOpen -> {
                SettingsSheet(
                    keystoreManager = keystoreManager,
                    onOpenOnboarding = {
                        isSettingsOpen = false
                        isOnboardingOpen = true
                    },
                    onDismiss = { isSettingsOpen = false }
                )
            }
            isQuestBoardOpen -> {
                QuestBoard(
                    tasks = tasks,
                    isLowSpoonMode = isLowSpoonMode,
                    onToggleLowSpoon = { isLowSpoonMode = !isLowSpoonMode },
                    onTaskCompleted = { task ->
                        coroutineScope.launch {
                            taskRepository.toggleTaskCompletion(task)
                        }
                    },
                    onAddTask = { title, energy, decompose ->
                        coroutineScope.launch {
                            taskRepository.createTask(title, "", energy, decompose)
                        }
                    },
                    onDismiss = { isQuestBoardOpen = false }
                )
            }
            isWallpaperPickerOpen -> {
                WallpaperPickerSheet(
                    wallpaperRepository = wallpaperRepository,
                    onDismiss = { isWallpaperPickerOpen = false }
                )
            }
            isDrawerOpen -> {
                val filteredApps = remember(searchQuery, apps) {
                    appRepository.filterApps(searchQuery, apps)
                }

                AppDrawer(
                    apps = filteredApps,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onAppClick = { app ->
                        appRepository.launchApp(app)
                        isDrawerOpen = false
                        searchQuery = ""
                    },
                    onCloseDrawer = {
                        isDrawerOpen = false
                        searchQuery = ""
                    },
                    modifier = Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                )
            }
            else -> {
                val isDefault = remember { checkIsDefaultLauncher(context) }
                val activeCount = remember(tasks) { tasks.count { !it.isCompleted } }

                LauncherHomeScreen(
                    isLowSpoonMode = isLowSpoonMode,
                    currentNoise = currentNoise,
                    isDefaultLauncher = isDefault,
                    activeTaskCount = activeCount,
                    onToggleNoise = {
                        val next = when (currentNoise) {
                            null -> NoiseType.BROWN
                            NoiseType.BROWN -> NoiseType.PINK
                            NoiseType.PINK -> NoiseType.WHITE
                            NoiseType.WHITE -> null
                        }
                        currentNoise = next
                        if (next == null) {
                            soundScapeEngine.stop()
                        } else {
                            soundScapeEngine.start(coroutineScope, next)
                        }
                    },
                    onOpenDrawer = { isDrawerOpen = true },
                    onOpenScratchpad = { isScratchpadOpen = true },
                    onOpenQuestBoard = { isQuestBoardOpen = true },
                    onOpenWallpaperPicker = { isWallpaperPickerOpen = true },
                    onOpenSettings = { isSettingsOpen = true },
                    onToggleLowSpoon = { isLowSpoonMode = !isLowSpoonMode },
                    onRequestDefaultLauncher = { requestDefaultLauncher(context) },
                    onCallClicked = onCallClicked,
                    onTextClicked = onTextClicked,
                    onNavigateClicked = onNavigateClicked
                )
            }
        }

        if (isScratchpadOpen) {
            ScratchpadDialog(
                scratchpadRepository = scratchpadRepository,
                onDismiss = { isScratchpadOpen = false }
            )
        }

        // Ambient Circadian Amber Tinting Layer
        if (tintColor.alpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tintColor)
            )
        }
    }
}

@Composable
fun LauncherHomeScreen(
    isLowSpoonMode: Boolean,
    currentNoise: NoiseType?,
    isDefaultLauncher: Boolean,
    activeTaskCount: Int,
    onToggleNoise: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenScratchpad: () -> Unit,
    onOpenQuestBoard: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLowSpoon: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onCallClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onNavigateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Main Scrollable Dashboard
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "a-Ha AuDHD",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isLowSpoonMode) "🥄 Low-Spoon Mode" else "⚡ Dopamine-Neutral",
                        fontSize = 11.sp,
                        color = if (isLowSpoonMode) Color(0xFFDDDD88) else Color(0xFF779977),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Audio Ambient Pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (currentNoise != null) Color(0xFF1E2D1E) else Color(0xFF161616),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (currentNoise != null) Color(0xFF448844) else Color(0xFF262626),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onToggleNoise()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentNoise == null) "🔇 Audio" else "🔊 ${currentNoise.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentNoise != null) Color(0xFF88DD88) else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Settings & BYOK Keys Pill
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF161616), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(6.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onOpenSettings()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚙ Settings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Default Launcher Prompt Banner
            if (!isDefaultLauncher) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF221A10), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF553A1A), RoundedCornerShape(8.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onRequestDefaultLauncher()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Set a-Ha as Default Launcher",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEEBB66),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Tap here to make a-Ha your primary home screen",
                                fontSize = 10.sp,
                                color = Color(0xFFBBAA88),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Set →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFFFF),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Persistent Desk & Flowmodoro Widget
            DeskWidget(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            // 3 High-Scaffolding Action Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quests Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceCharcoal, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenQuestBoard()
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "📋 Quests".toFixationPoint(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeTaskCount == 0) "No active quests" else "$activeTaskCount active",
                            fontSize = 11.sp,
                            color = if (activeTaskCount > 0) Color(0xFF88DD88) else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Max 3 tasks",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Scratchpad Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceCharcoal, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenScratchpad()
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "📝 Scratch".toFixationPoint(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Thought dump",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Swipe down",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Wallpapers Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceCharcoal, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenWallpaperPicker()
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "🎨 Wallpapers".toFixationPoint(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dark minimal",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "AMOLED",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Low-Spoon Mode Toggle Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isLowSpoonMode) Color(0xFF282818) else Color(0xFF141414),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isLowSpoonMode) Color(0xFF555522) else Color(0xFF242424),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onToggleLowSpoon()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLowSpoonMode) "🥄 Low-Spoon Mode: Demand reduction active" else "🥄 Overwhelmed? Switch to Low-Spoon Mode",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLowSpoonMode) Color(0xFFDDDD88) else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isLowSpoonMode) "[ ACTIVE ]" else "[ TOGGLE ]",
                        fontSize = 11.sp,
                        color = if (isLowSpoonMode) Color(0xFF88CC88) else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar Button Trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOpenDrawer()
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 Search or open apps...".toFixationPoint(),
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Swipe up ↑",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Bottom Section: Refined 4-item Utility Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UtilityDockItem(label = "📞 Phone", onClick = onCallClicked)
                Text(text = "•", color = Color(0xFF333333), fontSize = 12.sp)
                UtilityDockItem(label = "💬 Text", onClick = onTextClicked)
                Text(text = "•", color = Color(0xFF333333), fontSize = 12.sp)
                UtilityDockItem(label = "🧭 Maps", onClick = onNavigateClicked)
                Text(text = "•", color = Color(0xFF333333), fontSize = 12.sp)
                UtilityDockItem(label = "📱 Apps", onClick = onOpenDrawer)
            }
        }
    }
}

@Composable
private fun UtilityDockItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Text(
        text = label.toFixationPoint(boldWeight = FontWeight.Bold),
        style = MaterialTheme.typography.labelLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
            .clickable(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            })
            .padding(horizontal = 10.dp, vertical = 10.dp)
    )
}
