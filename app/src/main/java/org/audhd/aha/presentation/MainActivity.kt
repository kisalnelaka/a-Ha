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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audhd.aha.data.model.AppInfo
import org.audhd.aha.data.repository.AppRepository
import org.audhd.aha.data.repository.ScratchpadRepository
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.drawer.AppDrawer
import org.audhd.aha.presentation.scratchpad.ScratchpadDialog
import org.audhd.aha.presentation.theme.AHaTheme
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary

/**
 * Root Launcher Activity orchestrating the AuDHD executive functioning interface.
 */
class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var scratchpadRepository: ScratchpadRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appRepository = AppRepository(applicationContext)
        scratchpadRepository = ScratchpadRepository(applicationContext)

        setContent {
            AHaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherRoot(
                        appRepository = appRepository,
                        scratchpadRepository = scratchpadRepository,
                        onCallClicked = ::launchDialer,
                        onTextClicked = ::launchSms,
                        onNavigateClicked = ::launchNavigation
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh apps on resume in case packages changed
        // Handled reactively inside Composable
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
    onCallClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onNavigateClicked: () -> Unit
) {
    val view = LocalView.current
    val apps by appRepository.installedApps.collectAsState()
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isScratchpadOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        appRepository.refreshApps()
    }

    BackHandler(enabled = isDrawerOpen || isScratchpadOpen) {
        if (isScratchpadOpen) {
            isScratchpadOpen = false
        } else if (isDrawerOpen) {
            isDrawerOpen = false
            searchQuery = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Downward drag triggers Working Memory Scratchpad
                    if (dragAmount > 35 && !isDrawerOpen && !isScratchpadOpen) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        isScratchpadOpen = true
                    }
                    // Upward drag triggers Text-Only App Drawer
                    else if (dragAmount < -35 && !isDrawerOpen && !isScratchpadOpen) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        isDrawerOpen = true
                    }
                }
            }
    ) {
        if (isDrawerOpen) {
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
        } else {
            LauncherHomeScreen(
                onOpenDrawer = { isDrawerOpen = true },
                onOpenScratchpad = { isScratchpadOpen = true },
                onCallClicked = onCallClicked,
                onTextClicked = onTextClicked,
                onNavigateClicked = onNavigateClicked
            )
        }

        if (isScratchpadOpen) {
            ScratchpadDialog(
                scratchpadRepository = scratchpadRepository,
                onDismiss = { isScratchpadOpen = false }
            )
        }
    }
}

@Composable
fun LauncherHomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenScratchpad: () -> Unit,
    onCallClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onNavigateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Time Blindness Visualizer & Focus Scaffold
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text(
                text = "Momentum is your only asset.".toFixationPoint(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Swipe down for Scratchpad • Swipe up for Drawer".toFixationPoint(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Access Pills
            Row {
                Text(
                    text = "Scratchpad".toFixationPoint(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = onOpenScratchpad)
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                )
                Text(
                    text = "App Drawer".toFixationPoint(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = onOpenDrawer)
                        .padding(vertical = 8.dp, horizontal = 12.dp)
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
