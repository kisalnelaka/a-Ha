package org.audhd.aha.presentation.quest

import android.view.HapticFeedbackConstants
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.audhd.aha.data.local.entity.EnergyLevel
import org.audhd.aha.data.local.entity.TaskItem
import org.audhd.aha.domain.typography.FixationPointParser
import org.json.JSONArray

@Composable
fun QuestBoard(
    tasks: List<TaskItem>,
    isLowSpoonMode: Boolean,
    onToggleLowSpoon: () -> Unit,
    onTaskCompleted: (TaskItem) -> Unit,
    onAddTask: (title: String, energyLevel: EnergyLevel, decompose: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var rerollSeed by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Filter by low spoon if active
    val filteredTasks = remember(tasks, isLowSpoonMode) {
        if (isLowSpoonMode) {
            tasks.filter { it.energyLevel == EnergyLevel.LOW.name }
        } else {
            tasks
        }
    }

    // Select at most 3 active tasks to prevent PDA demand overload
    val selectedQuests = remember(filteredTasks, rerollSeed) {
        if (filteredTasks.size <= 3) {
            filteredTasks
        } else {
            filteredTasks.shuffled(kotlin.random.Random(rerollSeed)).take(3)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with low cognitive load
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Side Quests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0E0E0),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isLowSpoonMode) "Low-spoon mode active" else "Max 3 active paths",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Low spoon toggle button
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onToggleLowSpoon()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLowSpoonMode) Color(0xFF2A3A2A) else Color(0xFF1A1A1A),
                            contentColor = if (isLowSpoonMode) Color(0xFF88D888) else Color(0xFF888888)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isLowSpoonMode) "Spoon: Low" else "Spoon: All",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Close / back button
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1A1A),
                            contentColor = Color(0xFFCCCCCC)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Done", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quest Cards List
            if (selectedQuests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Zero demands pending.",
                            fontSize = 16.sp,
                            color = Color(0xFF777777),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rest without guilt or add a gentle quest below.",
                            fontSize = 12.sp,
                            color = Color(0xFF555555),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(selectedQuests, key = { it.id }) { quest ->
                        QuestCard(
                            task = quest,
                            onComplete = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onTaskCompleted(quest)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reroll Button (zero-shame avoidance bypass)
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        rerollSeed = System.currentTimeMillis()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E1E1E),
                        contentColor = Color(0xFFDDDDDD)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text(
                        text = "🎲 Reroll Paths",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Add Quest Button
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showAddDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A2A2A),
                        contentColor = Color(0xFFFFFFFF)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text(
                        text = "+ Add Quest",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if (showAddDialog) {
            AddQuestDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, energy, decompose ->
                    onAddTask(title, energy, decompose)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun QuestCard(
    task: TaskItem,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val steps = remember(task.subStepsJson) {
        org.audhd.aha.domain.decomposer.TaskDecomposerEngine.parseJsonSteps(task.subStepsJson)
    }


    val energyColor = when (task.energyLevel) {
        EnergyLevel.LOW.name -> Color(0xFF66AA66)
        EnergyLevel.MEDIUM.name -> Color(0xFFC0A060)
        EnergyLevel.HIGH.name -> Color(0xFFA06060)
        else -> Color(0xFF888888)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF121212), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF252525), RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF555555),
                    uncheckedColor = Color(0xFF777777),
                    checkmarkColor = Color(0xFFFFFFFF)
                ),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title with fixation point typography
                Text(
                    text = FixationPointParser.parse(task.title),
                    fontSize = 15.sp,
                    color = if (task.isCompleted) Color(0xFF666666) else Color(0xFFE8E8E8),
                    fontFamily = FontFamily.Monospace,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "[${task.energyLevel}]",
                        fontSize = 11.sp,
                        color = energyColor,
                        fontFamily = FontFamily.Monospace
                    )

                    if (steps.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (expanded) "▲ Hide steps" else "▼ ${steps.size} micro-steps",
                            fontSize = 11.sp,
                            color = Color(0xFF888888),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Expanded micro-steps (physical low-friction actions)
        AnimatedVisibility(visible = expanded && steps.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, top = 10.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        fontSize = 13.sp,
                        color = Color(0xFFBBBBBB),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddQuestDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, energy: EnergyLevel, decompose: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedEnergy by remember { mutableStateOf(EnergyLevel.LOW) }
    var autoDecompose by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF181818),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "New Side Quest",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0),
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Task (e.g. Clean kitchen)", color = Color(0xFF666666)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFFFFFFF),
                        unfocusedTextColor = Color(0xFFCCCCCC),
                        focusedBorderColor = Color(0xFF888888),
                        unfocusedBorderColor = Color(0xFF444444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Energy level selector
                Text(
                    text = "Required Energy / Spoons:",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnergyLevel.values().forEach { energy ->
                        val isSelected = selectedEnergy == energy
                        Button(
                            onClick = { selectedEnergy = energy },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF333333) else Color(0xFF202020),
                                contentColor = if (isSelected) Color(0xFFFFFFFF) else Color(0xFF777777)
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Text(energy.name, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Decompose Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoDecompose = !autoDecompose }
                ) {
                    Checkbox(
                        checked = autoDecompose,
                        onCheckedChange = { autoDecompose = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF888888),
                            uncheckedColor = Color(0xFF555555),
                            checkmarkColor = Color(0xFFFFFFFF)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Break into 3-5 micro-actions (Goblin Mode)",
                        fontSize = 12.sp,
                        color = Color(0xFFCCCCCC),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF888888)
                        )
                    ) {
                        Text("Cancel", fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAdd(title, selectedEnergy, autoDecompose)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3A3A3A),
                            contentColor = Color(0xFFFFFFFF),
                            disabledContainerColor = Color(0xFF222222),
                            disabledContentColor = Color(0xFF555555)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Add", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
