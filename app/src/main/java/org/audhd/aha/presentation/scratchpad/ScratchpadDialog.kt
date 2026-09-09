package org.audhd.aha.presentation.scratchpad

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.audhd.aha.data.repository.ScratchpadRepository
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.PureBlack
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary

/**
 * Working Memory Scratchpad dialog opened by downward swipe gestures.
 * Captures fleeting thoughts instantly and commits them to the plain-text `.txt` dump.
 */
@Composable
fun ScratchpadDialog(
    scratchpadRepository: ScratchpadRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var textInput by remember { mutableStateOf("") }
    var existingNotes by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        existingNotes = scratchpadRepository.readAllThoughts()
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack.copy(alpha = 0.95f))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Working Memory Scratchpad".toFixationPoint(),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )

                        Text(
                            text = "Done".toFixationPoint(),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable {
                                    if (textInput.isNotBlank()) {
                                        scope.launch {
                                            scratchpadRepository.appendThought(textInput)
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            onDismiss()
                                        }
                                    } else {
                                        onDismiss()
                                    }
                                }
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Dump thoughts immediately. Saved directly to offline .txt log.".toFixationPoint(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderSubtle)
                }

                // Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .background(SurfaceCharcoal, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    if (textInput.isEmpty()) {
                        Text(
                            text = "What is currently consuming your working memory?".toFixationPoint(),
                            color = TextMuted,
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                    )
                }

                // Action Footer & Previous Log Peek
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (existingNotes.isNotEmpty()) {
                        Text(
                            text = "Recent Dump Log:".toFixationPoint(),
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(PureBlack)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = existingNotes,
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Commit Thought [Enter]".toFixationPoint(boldWeight = FontWeight.Bold),
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        if (textInput.isNotBlank()) {
                                            scratchpadRepository.appendThought(textInput)
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            textInput = ""
                                            existingNotes = scratchpadRepository.readAllThoughts()
                                        }
                                        onDismiss()
                                    }
                                }
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
