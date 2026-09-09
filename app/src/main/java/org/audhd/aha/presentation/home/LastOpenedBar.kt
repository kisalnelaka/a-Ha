package org.audhd.aha.presentation.home

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.audhd.aha.domain.usagestats.LastOpenedProvider
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.TextMuted

/**
 * Renders a subtle one-line "Last opened: App · Xmin ago" context bar.
 *
 * Requires PACKAGE_USAGE_STATS (special app op). If not granted, renders nothing.
 * Never shows a permission prompt inline — the user can grant via Usage Access Settings.
 */
@Composable
fun LastOpenedBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<LastOpenedProvider.LastOpened?>(null) }
    var checked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!checked) {
            checked = true
            scope.launch {
                data = LastOpenedProvider.getLastOpened(context)
            }
        }
    }

    val info = data ?: return // No permission or no data — render nothing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Last: ",
            fontSize = 11.sp,
            color = Color(0xFF484848),
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = info.label.toFixationPoint(),
            fontSize = 11.sp,
            color = Color(0xFF585858),
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "  ·  ${LastOpenedProvider.formatAgo(info.lastUsedMs)}",
            fontSize = 11.sp,
            color = Color(0xFF404040),
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.weight(1f))

        // Tap to reopen
        Text(
            text = "[ reopen ]",
            fontSize = 10.sp,
            color = Color(0xFF3A3A3A),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clickable {
                    context.packageManager.getLaunchIntentForPackage(info.packageName)?.let { i ->
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(i)
                    }
                }
        )
    }
}
