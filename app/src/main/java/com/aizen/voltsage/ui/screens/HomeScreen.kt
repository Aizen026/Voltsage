package com.aizen.voltsage.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aizen.voltsage.data.model.AvailableGeminiModels
import com.aizen.voltsage.data.model.StudyPack
import com.aizen.voltsage.ui.GenerationUiState
import com.aizen.voltsage.ui.Screen
import com.aizen.voltsage.ui.VoltSageViewModel
import com.aizen.voltsage.ui.components.GoogleDriveAccountDialog
import com.aizen.voltsage.ui.components.GoogleSignInStartDialog
import com.aizen.voltsage.ui.components.HeroHeader
import com.aizen.voltsage.ui.components.ModelSelectorDialog
import com.aizen.voltsage.ui.components.QuizSetupDialog
import com.aizen.voltsage.ui.theme.SubjectBio
import com.aizen.voltsage.ui.theme.SubjectHistory
import com.aizen.voltsage.ui.theme.SubjectLit
import com.aizen.voltsage.ui.theme.SubjectMath
import com.aizen.voltsage.ui.theme.SubjectStem
import com.aizen.voltsage.ui.theme.SuccessGreen
import com.aizen.voltsage.ui.theme.VoltAmber
import com.aizen.voltsage.ui.theme.VoltBlue
import com.aizen.voltsage.ui.theme.VoltCyan
import com.aizen.voltsage.ui.theme.VoltFlame
import com.aizen.voltsage.ui.theme.VoltNeonBlue

@Composable
fun HomeScreen(
    viewModel: VoltSageViewModel,
    onNavigateToPack: (Long) -> Unit,
    onNavigateToQuiz: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allPacks by viewModel.allPacks.collectAsState()
    val inputUrl by viewModel.inputUrlOrText.collectAsState()
    val subjectFilter by viewModel.selectedSubjectFilter.collectAsState()
    val genState by viewModel.generationState.collectAsState()
    val currentModelId by viewModel.selectedModelId.collectAsState()
    val currentApiKey by viewModel.customApiKey.collectAsState()
    val isAmoledTheme by viewModel.isAmoledTheme.collectAsState()
    val isTestingApi by viewModel.isTestingApi.collectAsState()
    val apiTestStatus by viewModel.apiTestStatus.collectAsState()
    val showGoogleSignInPrompt by viewModel.showGoogleSignInStartPrompt.collectAsState()
    val defaultDifficulty by viewModel.defaultQuizDifficulty.collectAsState()
    val defaultCount by viewModel.defaultQuizQuestionCount.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()
    val isStudiedToday by viewModel.isStudiedToday.collectAsState()

    var showModelDialog by remember { mutableStateOf(false) }
    var modelDialogInitialTab by remember { mutableStateOf(0) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showMissingApiKeyDialog by remember { mutableStateOf(false) }
    var packForQuizSetup by remember { mutableStateOf<com.aizen.voltsage.data.model.StudyPack?>(null) }

    val activeModelFriendly = AvailableGeminiModels.find { it.id == currentModelId }?.friendlyName ?: "Flash Speedster"

    val sampleLinks = listOf(
        "https://en.wikipedia.org/wiki/Quantum_computing" to "Quantum Computing",
        "https://youtube.com/watch?v=00jbG_cfGuQ" to "Cellular Respiration",
        "https://en.wikipedia.org/wiki/Artificial_neural_network" to "Neural Networks",
        "https://en.wikipedia.org/wiki/Roman_Republic" to "Roman Republic"
    )

    val subjects = listOf("All", "Physics & STEM", "Biology", "Computer Science", "History", "Mathematics")

    val filteredPacks = if (subjectFilter == "All") {
        allPacks
    } else {
        allPacks.filter { it.subject.contains(subjectFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Header
        HeroHeader(
            streakDays = streakDays,
            isStudiedToday = isStudiedToday,
            activeModelName = activeModelFriendly,
            onOpenModelSelector = {
                modelDialogInitialTab = 0
                showModelDialog = true
            },
            onOpenAccount = { showAccountDialog = true }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Link Ingestion Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Link",
                                    tint = VoltCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PASTE ANY LINK OR TOPIC",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Quick Paste from Clipboard
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!text.isNullOrBlank()) {
                                        viewModel.updateInputText(text)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = VoltAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste", fontSize = 11.sp, color = VoltAmber)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { viewModel.updateInputText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("url_input_field"),
                            placeholder = {
                                Text(
                                    "Paste URL (article, video, docs) or write notes...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            trailingIcon = {
                                if (inputUrl.isNotBlank()) {
                                    IconButton(onClick = { viewModel.updateInputText("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VoltCyan,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick-Pick Sample Chips
                        Text(
                            text = "Or try popular study topics:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sampleLinks) { (url, label) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { viewModel.updateInputText(url) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "+ $label",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quiz Target Settings (Difficulty & Question Count)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = VoltCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Quiz Target",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "$defaultDifficulty • $defaultCount Questions",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VoltCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Difficulty row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Easy" to Color(0xFF10B981),
                                    "Medium" to VoltAmber,
                                    "Hard" to VoltFlame
                                ).forEach { (diff, color) ->
                                    val isSelected = defaultDifficulty.equals(diff, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                            .border(
                                                1.dp,
                                                if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.updateDefaultQuizDifficulty(diff) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = diff,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Count row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(3 to "3 Qs", 5 to "5 Qs", 8 to "8 Qs", 10 to "10 Qs").forEach { (count, label) ->
                                    val isSelected = defaultCount == count
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) VoltNeonBlue else MaterialTheme.colorScheme.surface)
                                            .clickable { viewModel.updateDefaultQuizQuestionCount(count) }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big Action Button
                        Button(
                            onClick = {
                                if (!viewModel.hasConfiguredApiKey()) {
                                    showMissingApiKeyDialog = true
                                } else {
                                    viewModel.generateStudyPackFromInput()
                                }
                            },
                            enabled = inputUrl.isNotBlank() && !genState.isGenerating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_study_pack_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VoltNeonBlue,
                                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Generate",
                                    tint = VoltAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Summarize & Generate Study Pack",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Subject Filter Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY STUDY SESSIONS (${filteredPacks.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subjects) { subj ->
                        val isSelected = subj == subjectFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) VoltBlue else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.setSubjectFilter(subj) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = subj,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Empty State
            if (filteredPacks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = VoltCyan,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Study Sessions Yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Paste a link or choose a sample topic above to generate your first crash course notes & quiz!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Study Packs List
                items(filteredPacks, key = { it.id }) { pack ->
                    StudyPackCard(
                        pack = pack,
                        onOpenNotes = { onNavigateToPack(pack.id) },
                        onStartQuiz = { packForQuizSetup = pack },
                        onToggleFavorite = { viewModel.toggleFavorite(pack.id, pack.isFavorite) },
                        onDelete = { viewModel.deleteStudyPack(pack.id) }
                    )
                }
            }
        }
    }

    // Quiz Customizer Dialog
    packForQuizSetup?.let { pack ->
        val quizState by viewModel.quizState.collectAsState()
        QuizSetupDialog(
            packTitle = pack.title,
            availableQuestionsCount = pack.quizQuestions.size.coerceAtLeast(5),
            initialDifficulty = defaultDifficulty,
            initialQuestionCount = defaultCount,
            isGenerating = quizState.isGeneratingNewQuiz,
            errorMessage = quizState.generationError,
            onDismissError = { viewModel.dismissQuizGenerationError() },
            onDismiss = { packForQuizSetup = null },
            onStartWithExisting = { diff, count ->
                viewModel.startQuizSession(pack.id, difficulty = diff, questionCount = count)
                packForQuizSetup = null
                onNavigateToQuiz(pack.id)
            },
            onGenerateNewQuiz = { diff, count ->
                viewModel.generateAndStartCustomQuiz(pack.id, difficulty = diff, questionCount = count)
                packForQuizSetup = null
                onNavigateToQuiz(pack.id)
            }
        )
    }

    // AI Generation Progress Dialog
    if (genState.isGenerating) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(VoltAmber, VoltFlame))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "VoltSage Powering Up...",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = genState.statusMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = VoltCyan,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CircularProgressIndicator(
                        color = VoltCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Missing API Key Dialog
    if (showMissingApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showMissingApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = VoltAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini API Key Required", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    "VoltSage requires an authentic Gemini API key to summarize links, analyze videos, and generate custom study packs. No fake or random data will be generated without your API key.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMissingApiKeyDialog = false
                        modelDialogInitialTab = 1
                        showModelDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                ) {
                    Text("Enter API Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMissingApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Dialog
    genState.errorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissGenerationError() },
            title = { Text("Study Generation Notice", fontWeight = FontWeight.Bold) },
            text = { Text(errorMsg, fontSize = 13.sp) },
            confirmButton = {
                if (errorMsg.contains("API key", ignoreCase = true)) {
                    Button(
                        onClick = {
                            viewModel.dismissGenerationError()
                            modelDialogInitialTab = 1
                            showModelDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VoltNeonBlue)
                    ) {
                        Text("Configure Key")
                    }
                } else {
                    TextButton(onClick = { viewModel.dismissGenerationError() }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (errorMsg.contains("API key", ignoreCase = true)) {
                    TextButton(onClick = { viewModel.dismissGenerationError() }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }

    // Model Selector & Settings Dialog
    if (showModelDialog) {
        ModelSelectorDialog(
            currentModelId = currentModelId,
            currentApiKey = currentApiKey,
            isAmoledTheme = isAmoledTheme,
            onModelSelected = { viewModel.setSelectedModel(it) },
            onApiKeySaved = { viewModel.setCustomApiKey(it) },
            onAmoledThemeChanged = { viewModel.setAmoledTheme(it) },
            onTestApiKey = { viewModel.testApiConnection() },
            isTesting = isTestingApi,
            testStatus = apiTestStatus,
            initialTab = modelDialogInitialTab,
            isFirebaseActive = viewModel.isFirebaseAILogicActive,
            onDismiss = { showModelDialog = false }
        )
    }

    // Google Drive Account Dialog
    if (showAccountDialog) {
        GoogleDriveAccountDialog(
            viewModel = viewModel,
            onDismiss = { showAccountDialog = false }
        )
    }

    // Google Sign-In on App Startup
    if (showGoogleSignInPrompt) {
        GoogleSignInStartDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissGoogleSignInPrompt() }
        )
    }
}

@Composable
fun StudyPackCard(
    pack: StudyPack,
    onOpenNotes: () -> Unit,
    onStartQuiz: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenNotes() }
            .testTag("study_pack_card_${pack.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Subject Badge + Source Type + Fav
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val subjectColor = when {
                        "Bio" in pack.subject -> SubjectBio
                        "Physics" in pack.subject || "STEM" in pack.subject -> SubjectStem
                        "History" in pack.subject -> SubjectHistory
                        "Math" in pack.subject -> SubjectMath
                        else -> VoltNeonBlue
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(subjectColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = pack.subject,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val isVideo = pack.sourceType.equals("VIDEO", ignoreCase = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.AutoMirrored.Filled.Article,
                            contentDescription = pack.sourceType,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = pack.sourceType,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (pack.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (pack.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = pack.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary excerpt
            Text(
                text = pack.summary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mastery progress & Next Review pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Retention Mastery",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${pack.masteryScore}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pack.masteryScore >= 80) SuccessGreen else VoltAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (pack.masteryScore.toFloat() / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (pack.masteryScore >= 80) SuccessGreen else if (pack.masteryScore >= 50) VoltCyan else VoltAmber,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (pack.nextReviewDate.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VoltBlue.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = VoltCyan,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Review: ${pack.nextReviewDate.takeLast(5)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = VoltCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (Read Notes vs Take Quiz)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenNotes,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read Notes", fontSize = 12.sp)
                }

                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoltBlue),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = VoltAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Quiz", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
