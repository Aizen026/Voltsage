package com.aizen.voltsage.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aizen.voltsage.ui.theme.VoltAmber
import com.aizen.voltsage.ui.theme.VoltCyan
import com.aizen.voltsage.ui.theme.VoltFlame
import com.aizen.voltsage.ui.theme.VoltNeonBlue

data class DifficultyOption(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

val difficultyOptions = listOf(
    DifficultyOption(
        id = "Easy",
        label = "Easy",
        description = "Core definitions, foundational facts, and direct recall",
        icon = Icons.Default.CheckCircle,
        color = Color(0xFF10B981) // Emerald Green
    ),
    DifficultyOption(
        id = "Medium",
        label = "Medium",
        description = "Applied concepts, mechanisms, and common misconceptions",
        icon = Icons.Default.Tune,
        color = VoltAmber // Amber
    ),
    DifficultyOption(
        id = "Hard",
        label = "Hard / Exam",
        description = "Advanced synthesis, edge cases, and tricky distractor options",
        icon = Icons.Default.LocalFireDepartment,
        color = VoltFlame // Coral/Flame
    )
)

@Composable
fun QuizSetupDialog(
    packTitle: String,
    availableQuestionsCount: Int,
    initialDifficulty: String = "Medium",
    initialQuestionCount: Int = 5,
    isGenerating: Boolean = false,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {},
    onDismiss: () -> Unit,
    onStartWithExisting: (difficulty: String, count: Int) -> Unit,
    onGenerateNewQuiz: (difficulty: String, count: Int) -> Unit
) {
    var selectedDifficulty by remember(initialDifficulty) { mutableStateOf(initialDifficulty) }
    var selectedCount by remember(initialQuestionCount) {
        mutableStateOf(if (initialQuestionCount in listOf(3, 5, 8, 10, availableQuestionsCount)) initialQuestionCount else 5)
    }

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .testTag("quiz_setup_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VoltNeonBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = VoltNeonBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Customize Quiz",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = packTitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error Message if any
                if (!errorMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VoltFlame.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = VoltFlame,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                fontSize = 12.sp,
                                color = VoltFlame,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = VoltFlame, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // --- 1. Difficulty Level ---
                Text(
                    text = "SELECT DIFFICULTY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    difficultyOptions.forEach { opt ->
                        val isSelected = selectedDifficulty.equals(opt.id, ignoreCase = true)
                        val borderColor = if (isSelected) opt.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        val bgColor = if (isSelected) opt.color.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                .clickable(enabled = !isGenerating) { selectedDifficulty = opt.id }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = null,
                                tint = if (isSelected) opt.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = opt.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) opt.color else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = opt.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = opt.color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- 2. Number of Questions ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NUMBER OF QUESTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$selectedCount questions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltCyan
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                val lengthChips = listOf(
                    3 to "3 Qs (Sprint)",
                    5 to "5 Qs (Standard)",
                    8 to "8 Qs (Deep)",
                    10 to "10 Qs (Mastery)",
                    availableQuestionsCount.coerceAtLeast(1) to "All (${availableQuestionsCount})"
                ).distinctBy { it.first }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lengthChips.take(4).forEach { (count, label) ->
                        val isSelected = selectedCount == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) VoltNeonBlue else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable(enabled = !isGenerating) { selectedCount = count }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$count",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (count == 3) "Sprint" else if (count == 5) "Standard" else if (count == 8) "Deep" else "Exam",
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- Action Buttons ---
                if (isGenerating) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = VoltCyan.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = VoltCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "VoltSage is synthesizing new $selectedDifficulty questions...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Quick Start button with existing questions
                        Button(
                            onClick = { onStartWithExisting(selectedDifficulty, selectedCount) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("start_custom_quiz_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start Quiz ($selectedCount Questions • $selectedDifficulty)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Generate Fresh AI Quiz button
                        OutlinedButton(
                            onClick = { onGenerateNewQuiz(selectedDifficulty, selectedCount) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("generate_new_ai_quiz_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltAmber)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = VoltAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚡ Generate Fresh AI Questions with Gemini",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
