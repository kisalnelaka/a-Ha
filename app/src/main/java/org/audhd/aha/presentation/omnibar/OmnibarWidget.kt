package org.audhd.aha.presentation.omnibar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audhd.aha.data.model.AppInfo
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary

@Composable
fun OmnibarWidget(
    apps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onCreateTask: (String) -> Unit,
    onTriggerGoblinAI: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val trimmed = query.trim()
    val isMath = trimmed.startsWith("=") || trimmed.startsWith(":calc")
    val isTask = trimmed.startsWith("+")
    val isAi = trimmed.startsWith("?") || trimmed.startsWith(":ai")

    // Evaluate math inline if query is an expression
    val mathResult = if (isMath) {
        val expr = trimmed.removePrefix("=").removePrefix(":calc").trim()
        SimpleMathEvaluator.evaluate(expr)
    } else null

    // Search apps if standard text query
    val matchingApps = if (!isMath && !isTask && !isAi && trimmed.isNotBlank()) {
        apps.filter { it.label.contains(trimmed, ignoreCase = true) }.take(3)
    } else emptyList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCharcoal)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        // Omnibar Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">",
                color = Color(0xFFBEFFC8),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "search apps, :calc 14*8, + task, ? ai",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color(0xFFBEFFC8)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[ ✕ ]",
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { query = "" }
                        .padding(2.dp)
                )
            }
        }

        // Action / Result Dropdown
        AnimatedVisibility(visible = isMath && mathResult != null) {
            val formatted = if (mathResult != null && mathResult % 1.0 == 0.0) {
                mathResult.toLong().toString()
            } else {
                mathResult.toString()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF162416))
                    .border(1.dp, Color(0xFF285028), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "= $formatted",
                    color = Color(0xFFBEFFC8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "[ COPY ]",
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    modifier = Modifier
                        .clickable {
                            clipboardManager.setText(AnnotatedString(formatted))
                            query = ""
                        }
                        .padding(2.dp)
                )
            }
        }

        AnimatedVisibility(visible = isTask && trimmed.length > 1) {
            val taskText = trimmed.removePrefix("+").trim()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1B241B))
                    .border(1.dp, Color(0xFF325432), RoundedCornerShape(4.dp))
                    .clickable {
                        if (taskText.isNotBlank()) {
                            onCreateTask(taskText)
                            query = ""
                            keyboardController?.hide()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+ ADD TASK: \"$taskText\"",
                    color = Color(0xFF88DD88),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "[ INSERT ]",
                    color = Color(0xFFBEFFC8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = isAi && trimmed.length > 1) {
            val prompt = trimmed.removePrefix("?").removePrefix(":ai").trim()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF221A2B))
                    .border(1.dp, Color(0xFF53366B), RoundedCornerShape(4.dp))
                    .clickable {
                        if (prompt.isNotBlank()) {
                            onTriggerGoblinAI(prompt)
                            query = ""
                            keyboardController?.hide()
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "? GOBLIN DECOMPOSE: \"$prompt\"",
                    color = Color(0xFFD1B3FF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "[ EXECUTE ]",
                    color = Color(0xFFE2CCFF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = matchingApps.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                matchingApps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF141414))
                            .clickable {
                                onLaunchApp(app)
                                query = ""
                                keyboardController?.hide()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "[ OPEN ]",
                            color = Color(0xFF88DD88),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }
    }
}
