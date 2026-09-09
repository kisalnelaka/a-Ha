package org.audhd.aha.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.AHaTheme
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary

/**
 * Root Launcher Activity for the a-Ha AuDHD operating environment.
 * Configured as [android.content.Intent.CATEGORY_HOME] and [android.content.Intent.CATEGORY_DEFAULT].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AHaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherHomeScreen(
                        onCallClicked = ::launchDialer,
                        onTextClicked = ::launchSms,
                        onNavigateClicked = ::launchNavigation
                    )
                }
            }
        }
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
fun LauncherHomeScreen(
    onCallClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onNavigateClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Flowmodoro and executive scaffolding active.".toFixationPoint(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
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
