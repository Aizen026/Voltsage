package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.QuizSessionState
import com.example.ui.VoltSageViewModel
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltBlue
import com.example.ui.theme.VoltCyan
import com.example.ui.theme.VoltFlame
import com.example.ui.theme.VoltNeonBlue

import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.QuizSetupDialog

@Composable
fun QuizScreen(
    viewModel: VoltSageViewModel,
    onBack: () -> Unit,
    onViewNotes: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val quizState by viewModel.quizState.collectAsState()
    var showSetupDialog by remember { mutableStateOf(false) }

    val difficultyColor = when (quizState.difficulty.lowercase()) {
        "easy" -> Color(0xFF10B981)
        "hard" -> VoltFlame
        else -> VoltAmber
    }

    if (showSetupDialog) {
        QuizSetupDialog(
            packTitle = quizState.title.ifEmpty { "Study Pack Quiz" },
            availableQuestionsCount = quizState.questions.size.coerceAtLeast(5),
            initialDifficulty = quizState.difficulty,
            initialQuestionCount = quizState.requestedCount,
            isGenerating = quizState.isGeneratingNewQuiz,
            errorMessage = quizState.generationError,
            onDismissError = { viewModel.dismissQuizGenerationError() },
            onDismiss = { showSetupDialog = false },
            onStartWithExisting = { diff, count ->
                viewModel.startQuizSession(quizState.packId, difficulty = diff, questionCount = count)
                showSetupDialog = false
            },
            onGenerateNewQuiz = { diff, count ->
                viewModel.generateAndStartCustomQuiz(quizState.packId, difficulty = diff, questionCount = count)
                showSetupDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Retention Quiz Challenge",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(difficultyColor)
                            )
                            Text(
                                text = "${quizState.difficulty} • ${quizState.questions.size} Qs",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = difficultyColor
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSetupDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Customize Quiz",
                                tint = VoltCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.restartQuiz() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restart",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (!quizState.isCompleted && quizState.questions.isNotEmpty()) {
                    val totalQ = quizState.questions.size
                    val currentStep = quizState.currentIndex + 1
                    val targetProgress = (currentStep.toFloat() / totalQ.toFloat()).coerceIn(0f, 1f)
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(400),
                        label = "quiz_step_progress"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question $currentStep of $totalQ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltCyan
                            )
                            Text(
                                text = "${(targetProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = VoltCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (quizState.isCompleted) {
            QuizCelebrationView(
                quizState = quizState,
                onRestart = { viewModel.restartQuiz() },
                onOpenCustomizer = { showSetupDialog = true },
                onViewNotes = { onViewNotes(quizState.packId) },
                onDone = onBack,
                modifier = Modifier.padding(innerPadding)
            )
        } else if (quizState.questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No quiz questions available for this pack.")
            }
        } else {
            val currentQ = quizState.questions.getOrNull(quizState.currentIndex)
            if (currentQ != null) {
                QuizQuestionView(
                    question = currentQ,
                    questionNumber = quizState.currentIndex + 1,
                    totalQuestions = quizState.questions.size,
                    selectedOption = quizState.selectedOptionIndex,
                    isConfirmed = quizState.isAnswerConfirmed,
                    onSelectOption = { viewModel.selectQuizOption(it) },
                    onConfirm = { viewModel.confirmQuizAnswer() },
                    onNext = { viewModel.nextQuizQuestion() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun QuizQuestionView(
    question: com.example.data.model.QuizQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    selectedOption: Int?,
    isConfirmed: Boolean,
    onSelectOption: (Int) -> Unit,
    onConfirm: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // Question Header pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(VoltBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "QUESTION $questionNumber OF $totalQuestions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltCyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = VoltAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Instant Recall",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Question Statement Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = question.question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Option Cards
        items(question.options.size) { index ->
            val optText = question.options[index]
            val isSelected = selectedOption == index
            val isCorrectOption = index == question.correctIndex

            val borderColor = when {
                isConfirmed && isCorrectOption -> SuccessGreen
                isConfirmed && isSelected && !isCorrectOption -> ErrorCoral
                isSelected -> VoltCyan
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }

            val bgColor = when {
                isConfirmed && isCorrectOption -> SuccessGreen.copy(alpha = 0.12f)
                isConfirmed && isSelected && !isCorrectOption -> ErrorCoral.copy(alpha = 0.12f)
                isSelected -> VoltCyan.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable(enabled = !isConfirmed) { onSelectOption(index) }
                    .testTag("quiz_option_$index"),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val optionLabel = when (index) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        else -> "D"
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isConfirmed && isCorrectOption -> SuccessGreen
                                    isConfirmed && isSelected -> ErrorCoral
                                    isSelected -> VoltCyan
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isConfirmed && isCorrectOption) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else if (isConfirmed && isSelected && !isCorrectOption) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = optionLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = optText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Explanation Card (Shows after user confirms answer)
        item {
            AnimatedVisibility(visible = isConfirmed) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedOption == question.correctIndex)
                            SuccessGreen.copy(alpha = 0.1f)
                        else
                            VoltAmber.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedOption == question.correctIndex) SuccessGreen.copy(alpha = 0.3f) else VoltAmber.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (selectedOption == question.correctIndex) SuccessGreen else VoltAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedOption == question.correctIndex) "Correct! High-Yield Insight:" else "VoltSage Insight:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedOption == question.correctIndex) SuccessGreen else VoltAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = question.explanation,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Action Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            if (!isConfirmed) {
                Button(
                    onClick = onConfirm,
                    enabled = selectedOption != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_answer_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                ) {
                    Text("Confirm Answer", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("next_question_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltBlue)
                ) {
                    Text(
                        text = if (questionNumber == totalQuestions) "View Results" else "Next Question →",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun QuizCelebrationView(
    quizState: QuizSessionState,
    onRestart: () -> Unit,
    onOpenCustomizer: () -> Unit,
    onViewNotes: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = quizState.questions.size
    val correct = quizState.correctAnswersCount
    val percentage = if (total > 0) ((correct.toFloat() / total) * 100).toInt() else 100

    val (badgeTitle, badgeColor) = when {
        percentage >= 90 -> "⚡ Super Sage Mastery" to VoltAmber
        percentage >= 70 -> "🌟 Strong Retention" to VoltCyan
        else -> "📚 Review Recommended" to VoltFlame
    }

    val difficultyColor = when (quizState.difficulty.lowercase()) {
        "easy" -> Color(0xFF10B981)
        "hard" -> VoltFlame
        else -> VoltAmber
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(VoltAmber, VoltFlame))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = badgeTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = badgeColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(difficultyColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${quizState.difficulty} Level",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = difficultyColor
                )
            }
            Text(
                text = "• $correct of $total Correct ($percentage%)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            val targetScoreProgress = (percentage.toFloat() / 100f).coerceIn(0f, 1f)
            val animatedScoreProgress by animateFloatAsState(
                targetValue = targetScoreProgress,
                animationSpec = tween(900),
                label = "celebration_score_progress"
            )

            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Retention Accuracy",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedScoreProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = badgeColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Retention", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$percentage%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VoltCyan)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Questions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$total Qs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Next Review", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "In 3 Days", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VoltAmber)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
        ) {
            Text("Done", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retake", fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onOpenCustomizer,
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltCyan)
            ) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = VoltCyan, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adjust Difficulty", fontSize = 13.sp)
            }
        }
    }
}
