package org.audhd.aha.presentation.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audhd.aha.data.repository.DailyAnchorRepository
import org.audhd.aha.domain.typography.toFixationPoint
import org.audhd.aha.presentation.theme.BorderSubtle
import org.audhd.aha.presentation.theme.SurfaceCharcoal
import org.audhd.aha.presentation.theme.TextMuted
import org.audhd.aha.presentation.theme.TextPrimary
import org.audhd.aha.presentation.theme.TextSecondary

/**
 * Single-field "one thing today" focus anchor, persisted to [DailyAnchorRepository].
 *
 * Design rules:
 * - One goal maximum: clearing the field is the only way to write a new one.
 * - Pressing Done (keyboard IME) or clicking the check mark commits the text.
 * - Displayed in monospace, styled subtly against the base surface to avoid visual dominance.
 */
@Composable
fun DailyAnchorWidget(
    repository: DailyAnchorRepository,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val anchor by repository.anchor.collectAsState()
    val focusManager = LocalFocusManager.current

    var editing by remember { mutableStateOf(false) }
    var draft by remember(anchor) { mutableStateOf(anchor) }
    val focusRequester = remember { FocusRequester() }

    fun commit() {
        val trimmed = draft.trim()
        repository.saveAnchor(trimmed)
        editing = false
        focusManager.clearFocus()
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCharcoal, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (anchor.isNotBlank()) Color(0xFF2A3A2A) else BorderSubtle,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = "TODAY'S ANCHOR",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (editing) {
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { commit() }),
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(TextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "[ Done ]",
                        fontSize = 11.sp,
                        color = Color(0xFF88BB88),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { commit() }
                    )
                }
            } else if (anchor.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = anchor.toFixationPoint(),
                        fontSize = 14.sp,
                        color = Color(0xFFCCEECC),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "[ Clear ]",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable {
                            draft = ""
                            repository.clearAnchor()
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                    )
                }
            } else {
                Text(
                    text = "Set one goal for today...".toFixationPoint(),
                    fontSize = 14.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editing = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                )
            }
        }
    }
}
