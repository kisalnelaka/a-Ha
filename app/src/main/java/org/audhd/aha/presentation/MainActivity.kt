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
import org.audhd.aha.data.repository.DailyAnchorRepository
import org.audhd.aha.data.repository.HiddenAppsRepository
import org.audhd.aha.data.repository.FrictionRepository
import org.audhd.aha.data.repository.QuarantineRepository
import org.audhd.aha.presentation.home.DailyAnchorWidget
import org.audhd.aha.presentation.home.CalendarGlanceCard
import org.audhd.aha.presentation.home.LastOpenedBar
import org.audhd.aha.presentation.components.QuarantineDigestCard
import org.audhd.aha.presentation.components.DayProgressRuler
import org.audhd.aha.presentation.omnibar.OmnibarWidget
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import android.app.Activity
import android.view.WindowManager

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
    private lateinit var dailyAnchorRepository: DailyAnchorRepository
    private lateinit var hiddenAppsRepository: HiddenAppsRepository
    private lateinit var frictionRepository: FrictionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appDatabase = AppDatabase.getInstance(applicationContext)
        keystoreManager = KeystoreManager(applicationContext)
        val decomposerEngine = TaskDecomposerEngine(appDatabase.taskDao(), keystoreManager)

        hiddenAppsRepository = HiddenAppsRepository(applicationContext)
        frictionRepository = FrictionRepository(applicationContext)
        appRepository = AppRepository(applicationContext, hiddenAppsRepository, frictionRepository)
        scratchpadRepository = ScratchpadRepository(applicationContext)
        taskRepository = TaskRepository(appDatabase.taskDao(), decomposerEngine)
        wallpaperRepository = WallpaperRepository(applicationContext)
        dailyAnchorRepository = DailyAnchorRepository(applicationContext)
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
                        dailyAnchorRepository = dailyAnchorRepository,
                        hiddenAppsRepository = hiddenAppsRepository,
                        frictionRepository = frictionRepository,
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
    dailyAnchorRepository: DailyAnchorRepository,
    hiddenAppsRepository: HiddenAppsRepository,
    frictionRepository: FrictionRepository,
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
    val quarantineRepository = remember { QuarantineRepository.getInstance(context) }
    var isBlackoutMode by remember { mutableStateOf(false) }

    val activity = context as? Activity
    LaunchedEffect(isBlackoutMode) {
        activity?.window?.attributes = activity?.window?.attributes?.apply {
            screenBrightness = if (isBlackoutMode) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        if (isBlackoutMode) {
            soundScapeEngine.stop()
            currentNoise = null
        }
    }

    // Circadian / Hyperfocus Continuous Session Guardrail
    val continuousDuration = remember { screenTimeTintController.getContinuousSessionDurationMs() }
    val tintColor = remember(continuousDuration) {
        screenTimeTintController.computeCircadianTint(continuousDuration)
    }

    LaunchedEffect(Unit) {
        appRepository.refreshApps()
    }

    BackHandler(enabled = isBlackoutMode || isDrawerOpen || isScratchpadOpen || isQuestBoardOpen || isWallpaperPickerOpen || isOnboardingOpen || isSettingsOpen) {
        when {
            isBlackoutMode -> isBlackoutMode = false
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
                    if (!isBlackoutMode && !isDrawerOpen && !isScratchpadOpen && !isQuestBoardOpen && !isWallpaperPickerOpen && !isOnboardingOpen && !isSettingsOpen) {
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
                    quarantineRepository = quarantineRepository,
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
                    onRegenerateTask = { task ->
                        coroutineScope.launch {
                            taskRepository.regenerateTaskDecomposition(task)
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
                    hiddenAppsRepository = hiddenAppsRepository,
                    frictionRepository = frictionRepository,
                    modifier = Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                )
            }
            else -> {
                val isDefault = remember { checkIsDefaultLauncher(context) }
                val activeCount = remember(tasks) { tasks.count { !it.isCompleted } }

                LauncherHomeScreen(
                    apps = apps,
                    quarantineRepository = quarantineRepository,
                    isLowSpoonMode = isLowSpoonMode,
                    currentNoise = currentNoise,
                    isDefaultLauncher = isDefault,
                    activeTaskCount = activeCount,
                    dailyAnchorRepository = dailyAnchorRepository,
                    onToggleNoise = {
                        val next = when (currentNoise) {
                            null -> NoiseType.RAIN
                            NoiseType.RAIN -> NoiseType.BINAURAL_GAMMA
                            NoiseType.BINAURAL_GAMMA -> NoiseType.BINAURAL_BETA
                            NoiseType.BINAURAL_BETA -> NoiseType.BROWN
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
                    onTriggerBlackout = { isBlackoutMode = true },
                    onOpenDrawer = { isDrawerOpen = true },
                    onOpenScratchpad = { isScratchpadOpen = true },
                    onOpenQuestBoard = { isQuestBoardOpen = true },
                    onOpenWallpaperPicker = { isWallpaperPickerOpen = true },
                    onOpenSettings = { isSettingsOpen = true },
                    onToggleLowSpoon = { isLowSpoonMode = !isLowSpoonMode },
                    onRequestDefaultLauncher = { requestDefaultLauncher(context) },
                    onCreateTask = { title ->
                        coroutineScope.launch {
                            taskRepository.createTask(title, autoDecompose = false)
                        }
                    },
                    onTriggerGoblinAI = { prompt ->
                        coroutineScope.launch {
                            taskRepository.createTask(prompt, autoDecompose = true)
                        }
                    },
                    onLaunchApp = { app ->
                        appRepository.launchApp(app)
                    },
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

        // Sensory Isolation Blackout Layer
        if (isBlackoutMode) {
            BlackoutOverlay(onDismiss = { isBlackoutMode = false })
        }
    }
}

@Composable
fun LauncherHomeScreen(
    apps: List<AppInfo>,
    quarantineRepository: QuarantineRepository,
    isLowSpoonMode: Boolean,
    currentNoise: NoiseType?,
    isDefaultLauncher: Boolean,
    activeTaskCount: Int,
    dailyAnchorRepository: DailyAnchorRepository,
    onToggleNoise: () -> Unit,
    onTriggerBlackout: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenScratchpad: () -> Unit,
    onOpenQuestBoard: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleLowSpoon: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onCreateTask: (String) -> Unit,
    onTriggerGoblinAI: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
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
                        text = "a-Ha".toFixationPoint(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isLowSpoonMode) "LOW-SPOON ACTIVE" else "DOPAMINE-NEUTRAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLowSpoonMode) Color(0xFFC8B870) else TextMuted,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sensory Panic / Blackout Pill
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E1414), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF4A2424), RoundedCornerShape(4.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onTriggerBlackout()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "CALM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9999),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Audio Ambient Pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (currentNoise != null) Color(0xFF1B261B) else Color(0xFF141414),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (currentNoise != null) Color(0xFF386038) else Color(0xFF242424),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onToggleNoise()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentNoise == null) "AUDIO: OFF" else "AUDIO: ${currentNoise.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentNoise != null) Color(0xFF88DD88) else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Settings & BYOK Keys Pill
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF141414), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF242424), RoundedCornerShape(4.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onOpenSettings()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SETTINGS",
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
                        .background(Color(0xFF1C1710), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF4A341A), RoundedCornerShape(6.dp))
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
                                text = "DEFAULT LAUNCHER NOT SET".toFixationPoint(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDFAB55),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Tap to set a-Ha as your primary home app",
                                fontSize = 10.sp,
                                color = Color(0xFFA09070),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "[ CONFIGURE ]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDFAB55),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Notification Air-Gap Quarantine Card (renders only if notifications are quarantined)
            QuarantineDigestCard(
                quarantineRepository = quarantineRepository,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Omnibar: Instant indexed search, math (:calc), task creation (+), AI (?):
            OmnibarWidget(
                apps = apps,
                onLaunchApp = onLaunchApp,
                onCreateTask = onCreateTask,
                onTriggerGoblinAI = onTriggerGoblinAI,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Daylight Progress Ruler (06:00 - 22:00)
            DayProgressRuler(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(10.dp))

            // Daily Anchor — one-thing-today focus field
            DailyAnchorWidget(
                repository = dailyAnchorRepository,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar Glance — next event within 24h
            CalendarGlanceCard(modifier = Modifier.fillMaxWidth())

            // Last opened context bar (manages its own padding only when data exists)
            LastOpenedBar()

            Spacer(modifier = Modifier.height(10.dp))

            // Persistent Desk & Flowmodoro Widget
            DeskWidget(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Scaffolding Action Modules
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quests Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenQuestBoard()
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "QUESTS".toFixationPoint(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeTaskCount == 0) "All clear" else "$activeTaskCount active",
                            fontSize = 11.sp,
                            color = if (activeTaskCount > 0) Color(0xFF88DD88) else TextMuted,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = "Max 3 tasks",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }

                // Scratchpad Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenScratchpad()
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "SCRATCH".toFixationPoint(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fast note",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = "Swipe down",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }

                // Wallpapers Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenWallpaperPicker()
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "SURFACE".toFixationPoint(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Wallpapers",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = "Custom / Dark",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Low-Spoon Mode Regulation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isLowSpoonMode) Color(0xFF1E1E14) else Color(0xFF121212),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isLowSpoonMode) Color(0xFF444422) else Color(0xFF202020),
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
                    Column {
                        Text(
                            text = "LOW-SPOON REGULATION".toFixationPoint(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLowSpoonMode) Color(0xFFDFDF88) else TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isLowSpoonMode) "Energy conservation active • Demand reduced" else "Demand reduction for executive fatigue",
                            fontSize = 10.sp,
                            color = if (isLowSpoonMode) Color(0xFFB0B070) else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = if (isLowSpoonMode) "[ ACTIVE ]" else "[ INACTIVE ]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowSpoonMode) Color(0xFF88DD88) else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Command-Style Search Trigger
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
                        text = "Type to search or launch...".toFixationPoint(),
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DRAWER ↑",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Bottom Section: Refined Monospace Utility Dock
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
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UtilityDockItem(label = "PHONE", onClick = onCallClicked)
                Text(text = "·", color = Color(0xFF444444), fontSize = 14.sp)
                UtilityDockItem(label = "MESSAGES", onClick = onTextClicked)
                Text(text = "·", color = Color(0xFF444444), fontSize = 14.sp)
                UtilityDockItem(label = "MAPS", onClick = onNavigateClicked)
                Text(text = "·", color = Color(0xFF444444), fontSize = 14.sp)
                UtilityDockItem(label = "DRAWER", onClick = onOpenDrawer)
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
        style = MaterialTheme.typography.labelMedium,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = modifier
            .clickable(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            })
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

/**
 * Fullscreen pure-black sensory reduction overlay.
 * Hardware brightness is clamped to 0.01 and audio is silenced.
 * Requires 3 intentional taps to exit, preventing accidental dismissal.
 */
@Composable
fun BlackoutOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tapCount by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .clickable {
                tapCount++
                if (tapCount >= 3) {
                    onDismiss()
                }
            }
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF333333), RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SENSORY ISOLATION ACTIVE",
                color = Color(0xFF666666),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hardware display minimized. Audio silenced.",
                color = Color(0xFF444444),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (tapCount == 0) "[ TRIPLE-TAP TO RESTORE ]" else "[ TAP ${3 - tapCount} MORE TO RESTORE ]",
                color = Color(0xFF555555),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

