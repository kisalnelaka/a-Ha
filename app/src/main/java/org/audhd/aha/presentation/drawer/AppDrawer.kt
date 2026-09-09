package org.audhd.aha.presentation.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.audhd.aha.data.model.AppInfo
import org.audhd.aha.data.repository.FrictionRepository
import org.audhd.aha.data.repository.HiddenAppsRepository
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary

/**
 * High-performance, text-only app drawer designed for dopamine neutrality.
 *
 * Invariants:
 * - Strips all icon bitmaps to eliminate visual clutter and comply with <120MB idle RAM budget.
 * - Formats every app label through [FixationPointParser] for rapid saccadic scanning.
 * - Long-press on any app opens an action sheet: Hide App, set Friction Level, or Cancel.
 */
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onCloseDrawer: () -> Unit,
    hiddenAppsRepository: HiddenAppsRepository? = null,
    frictionRepository: FrictionRepository? = null,
    modifier: Modifier = Modifier
) {
    var contextMenuApp by remember { mutableStateOf<AppInfo?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search and Dismiss Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search apps...".toFixationPoint(),
                        color = TextMuted,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Close".toFixationPoint(),
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onCloseDrawer)
                    .padding(vertical = 12.dp, horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No apps found for \"$searchQuery\"" else "No applications loaded yet",
                        color = TextMuted,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "[ Clear Search ]".toFixationPoint(),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onSearchQueryChange("") }
                                .background(SurfaceCharcoal, RoundedCornerShape(4.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            // Text-Only App List with long-press context menu
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = apps,
                    key = { it.packageName + "/" + it.activityName }
                ) { app ->
                    AppDrawerItem(
                        app = app,
                        frictionDelaySeconds = frictionRepository?.getDelay(app.packageName),
                        onClick = { onAppClick(app) },
                        onLongPress = { contextMenuApp = app }
                    )
                }
            }
        }
    }

    // Context Menu Dialog: Hide / Friction
    contextMenuApp?.let { app ->
        AppContextMenu(
            app = app,
            currentFrictionSeconds = frictionRepository?.getDelay(app.packageName),
            onHide = {
                hiddenAppsRepository?.hideApp(app.packageName)
                contextMenuApp = null
            },
            onSetFriction = { seconds ->
                if (seconds == null) {
                    frictionRepository?.clearDelay(app.packageName)
                } else {
                    frictionRepository?.setDelay(app.packageName, seconds)
                }
                contextMenuApp = null
            },
            onDismiss = { contextMenuApp = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerItem(
    app: AppInfo,
    frictionDelaySeconds: Int?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fixationText = remember(app.label) { app.label.toFixationPoint() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = fixationText,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        if (app.isDistractionApp) {
            val frictionLabel = frictionDelaySeconds?.let { "${it}s" } ?: "12s"
            Text(
                text = "friction ${frictionLabel}",
                color = Color(0xFF886666),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AppContextMenu(
    app: AppInfo,
    currentFrictionSeconds: Int?,
    onHide: () -> Unit,
    onSetFriction: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF161616),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = app.label.toFixationPoint(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDDDDDD),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = app.packageName,
                    fontSize = 10.sp,
                    color = Color(0xFF666666),
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hide App
                ContextMenuRow(
                    label = "[ HIDE FROM DRAWER ]",
                    color = Color(0xFFD88888),
                    onClick = onHide
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Friction level selector — only if it's a distraction app
                if (app.isDistractionApp) {
                    Text(
                        text = "FRICTION DELAY",
                        fontSize = 10.sp,
                        color = Color(0xFF666666),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        // Default
                        FrictionChip(
                            label = "Default",
                            selected = currentFrictionSeconds == null,
                            onClick = { onSetFriction(null) }
                        )
                        FrictionRepository.FRICTION_LEVELS.forEach { sec ->
                            FrictionChip(
                                label = "${sec}s",
                                selected = currentFrictionSeconds == sec,
                                onClick = { onSetFriction(sec) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Cancel
                ContextMenuRow(
                    label = "[ Cancel ]",
                    color = Color(0xFF666666),
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ContextMenuRow(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label.toFixationPoint(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun FrictionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 11.sp,
        color = if (selected) Color(0xFF88DD88) else Color(0xFF888888),
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .background(
                if (selected) Color(0xFF1B281B) else Color(0xFF1C1C1C),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
