package org.audhd.aha.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
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
import org.audhd.aha.presentation.drawer.AppDrawer
import org.audhd.aha.presentation.onboarding.OnboardingScreen
import org.audhd.aha.presentation.quest.QuestBoard
import org.audhd.aha.presentation.scratchpad.ScratchpadDialog
import org.audhd.aha.presentation.theme.AHaTheme
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appDatabase = AppDatabase.getInstance(applicationContext)
        val keystoreManager = KeystoreManager(applicationContext)
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
    onCallClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onNavigateClicked: () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val apps by appRepository.installedApps.collectAsState()
    val tasks by taskRepository.getActiveTasks().collectAsState(initial = emptyList())

    var isDrawerOpen by remember { mutableStateOf(false) }
    var isScratchpadOpen by remember { mutableStateOf(false) }
    var isQuestBoardOpen by remember { mutableStateOf(false) }
    var isWallpaperPickerOpen by remember { mutableStateOf(false) }
    var isOnboardingOpen by remember { mutableStateOf(false) }
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

    BackHandler(enabled = isDrawerOpen || isScratchpadOpen || isQuestBoardOpen || isWallpaperPickerOpen || isOnboardingOpen) {
        when {
            isOnboardingOpen -> isOnboardingOpen = false
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
                    if (!isDrawerOpen && !isScratchpadOpen && !isQuestBoardOpen && !isWallpaperPickerOpen && !isOnboardingOpen) {
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
                    onComplete = { isOnboardingOpen = false }
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
                LauncherHomeScreen(
                    isLowSpoonMode = isLowSpoonMode,
                    currentNoise = currentNoise,
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
                    onOpenOnboarding = { isOnboardingOpen = true },
                    onToggleLowSpoon = { isLowSpoonMode = !isLowSpoonMode },
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
    onToggleNoise: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenScratchpad: () -> Unit,
    onOpenQuestBoard: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onToggleLowSpoon: () -> Unit,
    onCallClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onNavigateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Focus Scaffold & Working Memory
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
        ) {
            Text(
                text = (if (isLowSpoonMode) "Protect your spoons. One step." else "Momentum is your only asset.").toFixationPoint(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Swipe down for Scratchpad • Swipe up for Drawer".toFixationPoint(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Access Scaffolding Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quests".toFixationPoint(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenQuestBoard()
                        })
                        .background(Color(0xFF181818), RoundedCornerShape(4.dp))
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                )

                Text(
                    text = "Scratchpad".toFixationPoint(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onOpenScratchpad()
                        })
                        .background(Color(0xFF181818), RoundedCornerShape(4.dp))
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                )

                Text(
                    text = (if (currentNoise == null) "Audio: Off" else "Audio: ${currentNoise.name}").toFixationPoint(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (currentNoise != null) Color(0xFF88D888) else TextSecondary,
                    modifier = Modifier
                        .clickable(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onToggleNoise()
                        })
                        .background(Color(0xFF181818), RoundedCornerShape(4.dp))
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                )

                Text(
                    text = (if (isLowSpoonMode) "Spoon: Low" else "Spoon: All").toFixationPoint(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isLowSpoonMode) Color(0xFFD8D888) else TextSecondary,
                    modifier = Modifier
                        .clickable(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onToggleLowSpoon()
                        })
                        .background(Color(0xFF181818), RoundedCornerShape(4.dp))
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "App Drawer".toFixationPoint(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable(onClick = onOpenDrawer)
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                )
                Text(
                    text = "Wallpapers".toFixationPoint(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier
                        .clickable(onClick = onOpenWallpaperPicker)
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                )
                Text(
                    text = "Principles / Guide".toFixationPoint(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier
                        .clickable(onClick = onOpenOnboarding)
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                )
            }
        }



        // Bottom Section: The Monochromatic Utility Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            MonochromaticUtilityBar(
                onCallClicked = onCallClicked,
                onTextClicked = onTextClicked,
                onNavigateClicked = onNavigateClicked
            )
        }
    }
}

@Composable
fun MonochromaticUtilityBar(
    onCallClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onNavigateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UtilityBarItem(label = "Phone", onClick = onCallClicked)
        Text(text = "•", color = TextMuted, fontSize = 12.sp)
        UtilityBarItem(label = "Messages", onClick = onTextClicked)
        Text(text = "•", color = TextMuted, fontSize = 12.sp)
        UtilityBarItem(label = "Navigate", onClick = onNavigateClicked)
    }
}

@Composable
private fun UtilityBarItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = label.toFixationPoint(boldWeight = FontWeight.Bold),
        style = MaterialTheme.typography.labelLarge,
        color = TextPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
