package org.audhd.aha.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.audhd.aha.data.repository.QuarantineRepository
import org.audhd.aha.data.repository.QuarantinedNotification
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuarantineDigestCard(
    quarantineRepository: QuarantineRepository,
    modifier: Modifier = Modifier
) {
    val notifications by quarantineRepository.notifications.collectAsState()
    var isDigestDialogOpen by remember { mutableStateOf(false) }

    if (notifications.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141208))
            .border(1.dp, Color(0xFF423812), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFFFC107), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AIR-GAP: ${notifications.size} NOTIFICATIONS QUARANTINED",
                    color = Color(0xFFFFD54F),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "[ CLEAR ]",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                modifier = Modifier
                    .clickable { quarantineRepository.clearAll() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Real-time pings suppressed to protect working memory.",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF241F0C))
                .border(1.dp, Color(0xFF5A4D1A), RoundedCornerShape(4.dp))
                .clickable { isDigestDialogOpen = true }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[ INSPECT MONOSPACE DIGEST ]",
                color = Color(0xFFFFE082),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (isDigestDialogOpen) {
        QuarantineDigestDialog(
            notifications = notifications,
            onDismissItem = { id -> quarantineRepository.dismissNotification(id) },
            onClearAll = {
                quarantineRepository.clearAll()
                isDigestDialogOpen = false
            },
            onDismissRequest = { isDigestDialogOpen = false }
        )
    }
}

@Composable
fun QuarantineDigestDialog(
    notifications: List<QuarantinedNotification>,
    onDismissItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack.copy(alpha = 0.94f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCharcoal)
                    .border(1.dp, Color(0xFF423812), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUARANTINE DIGEST (${notifications.size})",
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "[ CLOSE ✕ ]",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onDismissRequest() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.appLabel.uppercase(),
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = timeFormat.format(Date(item.timestamp)),
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            }

                            if (item.title.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.title,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (item.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.text,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "[ DISMISS ]",
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    modifier = Modifier
                                        .clickable { onDismissItem(item.id) }
                                        .padding(4.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "[ LAUNCH APP ]",
                                    color = Color(0xFF88DD88),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            val intent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                                            if (intent != null) {
                                                context.startActivity(intent)
                                            }
                                            onDismissItem(item.id)
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E1414))
                        .border(1.dp, Color(0xFF4A2020), RoundedCornerShape(4.dp))
                        .clickable { onClearAll() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[ CLEAR ENTIRE DIGEST ]",
                        color = Color(0xFFFF8888),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
