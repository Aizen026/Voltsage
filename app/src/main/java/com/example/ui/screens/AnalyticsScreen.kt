package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VoltSageViewModel
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltBlue
import com.example.ui.theme.VoltCyan
import com.example.ui.theme.VoltFlame
import com.example.ui.theme.VoltNeonBlue

@Composable
fun AnalyticsScreen(
    viewModel: VoltSageViewModel,
    modifier: Modifier = Modifier
) {
    val allPacks by viewModel.allPacks.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()
    val streak by viewModel.streakDays.collectAsState()
    val studyMins = viewModel.getTotalStudyMinutes()
    val quizzesTaken = viewModel.getQuizzesTaken()
    val accuracy = viewModel.getQuizAccuracy()

    val completedReviewsCount = allReviews.count { it.completed }
    val totalReviewsCount = allReviews.size

    val scholarRank = viewModel.getScholarRank()
    val animatedXpProgress by animateFloatAsState(
        targetValue = scholarRank.progress,
        animationSpec = tween(800),
        label = "scholar_xp_progress"
    )

    val targetCompletionPct = if (totalReviewsCount > 0) {
        (completedReviewsCount.toFloat() / totalReviewsCount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedCompletionPct by animateFloatAsState(
        targetValue = targetCompletionPct,
        animationSpec = tween(800),
        label = "completion_progress"
    )

    val avgMastery = if (allPacks.isNotEmpty()) {
        (allPacks.map { it.masteryScore }.average()).toInt()
    } else 0
    val animatedAvgMastery by animateFloatAsState(
        targetValue = (avgMastery.toFloat() / 100f).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "avg_mastery_progress"
    )

    val subjectCounts = allPacks.groupBy { it.subject }.mapValues { it.value.size }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = VoltCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PROGRESS & RETENTION TRACKING",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Track your spaced repetition velocity, quiz accuracy, and memory consolidation",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Super Sage Scholar Rank Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(VoltBlue.copy(alpha = 0.85f), Color(0xFF1E293B))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(VoltAmber, VoltFlame))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "VOLTSAGE SCHOLAR RANK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = VoltCyan
                                )
                                Text(
                                    text = "Level ${scholarRank.level} • ${scholarRank.title}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { animatedXpProgress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = VoltAmber,
                                        trackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${scholarRank.currentLevelXp}/${scholarRank.levelCapacityXp} XP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4-Card Metric Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Study Streak",
                        value = "$streak Days",
                        sub = "Unbroken consistency",
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = VoltFlame,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Study Velocity",
                        value = "$studyMins Mins",
                        sub = "High-yield focused time",
                        icon = Icons.Default.Timer,
                        iconTint = VoltCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Quizzes Mastered",
                        value = "$quizzesTaken",
                        sub = "Instant recall tests",
                        icon = Icons.Default.Quiz,
                        iconTint = VoltAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Quiz Accuracy",
                        value = "$accuracy%",
                        sub = "Knowledge retention rate",
                        icon = Icons.Default.CheckCircle,
                        iconTint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Spaced Repetition Retention Health
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = VoltCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SPACED REPETITION HEALTH",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Scheduled Reviews Completed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val reviewText = if (totalReviewsCount == 0) "0 / 0" else "$completedReviewsCount / $totalReviewsCount (${(targetCompletionPct * 100).toInt()}%)"
                            Text(reviewText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VoltCyan)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { animatedCompletionPct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SuccessGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "According to the Ebbinghaus Forgetting Curve, reviewing concepts at 24h, 3d, and 7d intervals locks knowledge into permanent long-term memory.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Overall Knowledge Retention Mastery
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = VoltAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RETENTION MASTERY",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$avgMastery% Average",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (avgMastery >= 80) SuccessGreen else if (avgMastery >= 50) VoltCyan else VoltAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { animatedAvgMastery },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (avgMastery >= 80) SuccessGreen else if (avgMastery >= 50) VoltCyan else VoltAmber,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (allPacks.isEmpty()) "Create study packs and complete recall quizzes to establish your retention baseline."
                            else "Calculated dynamically across your active study packs based on active recall quiz performance.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Subject Distribution
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SUBJECT BREAKDOWN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (subjectCounts.isEmpty()) {
                            Text("No study sessions created yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val totalPacks = allPacks.size
                            subjectCounts.forEach { (subject, count) ->
                                val fraction = if (totalPacks > 0) (count.toFloat() / totalPacks.toFloat()).coerceIn(0f, 1f) else 0f
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(subject, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("$count packs (${(fraction * 100).toInt()}%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoltCyan)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = VoltCyan,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
