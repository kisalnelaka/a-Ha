package org.audhd.aha.presentation.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audhd.aha.data.model.AppInfo
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
 */
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
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

        // Text-Only App List
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = apps,
                key = { it.packageName + "/" + it.activityName }
            ) { app ->
                AppDrawerItem(
                    app = app,
                    onClick = { onAppClick(app) }
                )
            }
        }
    }
}

@Composable
fun AppDrawerItem(
    app: AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fixationText = remember(app.label) { app.label.toFixationPoint() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Text(
                text = "• friction".toFixationPoint(),
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
