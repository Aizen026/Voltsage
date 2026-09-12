package com.aizen.voltsage.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aizen.voltsage.ui.VoltSageViewModel
import com.aizen.voltsage.ui.components.QuizSetupDialog
import com.aizen.voltsage.ui.theme.SuccessGreen
import com.aizen.voltsage.ui.theme.VoltAmber
import com.aizen.voltsage.ui.theme.VoltBlue
import com.aizen.voltsage.ui.theme.VoltCyan
import com.aizen.voltsage.ui.theme.VoltNeonBlue

@Composable
fun StudyDetailScreen(
    packId: Long,
    viewModel: VoltSageViewModel,
    onBack: () -> Unit,
    onStartQuiz: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenGroups: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pack by viewModel.activePack.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview & Summary", "Crash-Course Notes", "Key Takeaways")

    if (pack == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading study pack...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val currentPack = pack!!
    var showQuizSetup by remember { mutableStateOf(false) }
    val defaultDifficulty by viewModel.defaultQuizDifficulty.collectAsState()
    val defaultCount by viewModel.defaultQuizQuestionCount.collectAsState()

    if (showQuizSetup) {
        val quizState by viewModel.quizState.collectAsState()
        QuizSetupDialog(
            packTitle = currentPack.title,
            availableQuestionsCount = currentPack.quizQuestions.size.coerceAtLeast(5),
            initialDifficulty = defaultDifficulty,
            initialQuestionCount = defaultCount,
            isGenerating = quizState.isGeneratingNewQuiz,
            errorMessage = quizState.generationError,
            onDismissError = { viewModel.dismissQuizGenerationError() },
            onDismiss = { showQuizSetup = false },
            onStartWithExisting = { diff, count ->
                viewModel.startQuizSession(currentPack.id, difficulty = diff, questionCount = count)
                showQuizSetup = false
                onStartQuiz(currentPack.id)
            },
            onGenerateNewQuiz = { diff, count ->
                viewModel.generateAndStartCustomQuiz(currentPack.id, difficulty = diff, questionCount = count)
                showQuizSetup = false
                onStartQuiz(currentPack.id)
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
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
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

                    Row {
                        IconButton(onClick = { viewModel.toggleFavorite(currentPack.id, currentPack.isFavorite) }) {
                            Icon(
                                imageVector = if (currentPack.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentPack.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "⚡ VoltSage Study Notes: ${currentPack.title}\n\nSummary:\n${currentPack.summary}\n\nKey Takeaways:\n" +
                                        currentPack.keyTakeaways.joinToString("\n• ", prefix = "• ")
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Study Pack"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VoltCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = currentPack.subject,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Source: ${currentPack.sourceType}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentPack.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                }

                // Section Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = VoltNeonBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VoltCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) VoltCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Schedule", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showQuizSetup = true },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("start_quiz_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = VoltAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Quiz (${currentPack.quizQuestions.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Executive Summary
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = VoltAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EXECUTIVE SUMMARY",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = currentPack.summary,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RETENTION MASTERY",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${currentPack.masteryScore}%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (currentPack.masteryScore >= 80) SuccessGreen else if (currentPack.masteryScore >= 50) VoltCyan else VoltAmber
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (currentPack.masteryScore.toFloat() / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (currentPack.masteryScore >= 80) SuccessGreen else if (currentPack.masteryScore >= 50) VoltCyan else VoltAmber,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = VoltBlue.copy(alpha = 0.1f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCyan.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Spaced Repetition Active",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltCyan
                                    )
                                    Text(
                                        text = "Next scheduled review on ${currentPack.nextReviewDate.ifBlank { "Tomorrow" }}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = onOpenCalendar,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VoltBlue)
                                ) {
                                    Text("Calendar", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Detailed but less complicated notes
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = VoltCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LESS-COMPLICATED DETAILED NOTES",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Render readable notes with nice typography
                                val noteLines = currentPack.detailedNotes.lines()
                                noteLines.forEach { line ->
                                    val trimmed = line.trim()
                                    when {
                                        trimmed.startsWith("# ") -> {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = trimmed.removePrefix("# "),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = VoltNeonBlue
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                        trimmed.startsWith("### ") || trimmed.startsWith("## ") -> {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = trimmed.replace(Regex("^#+\\s*"), ""),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        trimmed.startsWith("- ") -> {
                                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                                Text("⚡ ", color = VoltAmber, fontSize = 13.sp)
                                                Text(
                                                    text = trimmed.removePrefix("- "),
                                                    fontSize = 13.sp,
                                                    lineHeight = 19.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        trimmed.isNotBlank() -> {
                                            Text(
                                                text = trimmed,
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Key Takeaways list
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = VoltAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CORE KEY TAKEAWAYS (${currentPack.keyTakeaways.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    itemsIndexed(currentPack.keyTakeaways) { index, takeaway ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(VoltCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoltCyan
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = takeaway,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
