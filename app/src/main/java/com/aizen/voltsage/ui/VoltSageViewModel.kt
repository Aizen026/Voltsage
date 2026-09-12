package com.aizen.voltsage.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aizen.voltsage.data.model.AvailableGeminiModels
import com.aizen.voltsage.data.model.GeminiModelOption
import com.aizen.voltsage.data.model.QuizQuestion
import com.aizen.voltsage.data.model.ReviewScheduleItem
import com.aizen.voltsage.data.model.ScholarRankInfo
import com.aizen.voltsage.data.model.StudyGroup
import com.aizen.voltsage.data.model.StudyPack
import com.aizen.voltsage.data.model.withRandomizedOptions
import com.aizen.voltsage.data.remote.GoogleDriveBackupManager
import com.aizen.voltsage.data.repository.VoltSageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    data class Detail(val packId: Long) : Screen()
    data class Quiz(val packId: Long) : Screen()
    object Calendar : Screen()
    object Groups : Screen()
    object Analytics : Screen()
    object Settings : Screen()
}

data class GenerationUiState(
    val isGenerating: Boolean = false,
    val statusMessage: String = "",
    val errorMessage: String? = null
)

data class QuizSessionState(
    val packId: Long = 0,
    val title: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerConfirmed: Boolean = false,
    val correctAnswersCount: Int = 0,
    val isCompleted: Boolean = false,
    val difficulty: String = "Medium",
    val requestedCount: Int = 5,
    val isGeneratingNewQuiz: Boolean = false,
    val generationError: String? = null
)

class VoltSageViewModel(application: Application) : AndroidViewModel(application) {
    val repository = VoltSageRepository(application)
    val driveManager = GoogleDriveBackupManager(application)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _inputUrlOrText = MutableStateFlow("")
    val inputUrlOrText: StateFlow<String> = _inputUrlOrText.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow("All")
    val selectedSubjectFilter: StateFlow<String> = _selectedSubjectFilter.asStateFlow()

    private val _defaultQuizDifficulty = MutableStateFlow("Medium")
    val defaultQuizDifficulty: StateFlow<String> = _defaultQuizDifficulty.asStateFlow()

    private val _defaultQuizQuestionCount = MutableStateFlow(5)
    val defaultQuizQuestionCount: StateFlow<Int> = _defaultQuizQuestionCount.asStateFlow()

    private val _generationState = MutableStateFlow(GenerationUiState())
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _selectedDate = MutableStateFlow(repository.getTodayDateStr())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _quizState = MutableStateFlow(QuizSessionState())
    val quizState: StateFlow<QuizSessionState> = _quizState.asStateFlow()

    private val _activePack = MutableStateFlow<StudyPack?>(null)
    val activePack: StateFlow<StudyPack?> = _activePack.asStateFlow()

    // Gemini Settings State
    private val _customApiKey = MutableStateFlow(repository.getCustomApiKey())
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _selectedModelId = MutableStateFlow(repository.getSelectedModel())
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val _isAmoledTheme = MutableStateFlow(repository.isAmoledThemeEnabled())
    val isAmoledTheme: StateFlow<Boolean> = _isAmoledTheme.asStateFlow()

    private val _apiTestStatus = MutableStateFlow<String?>(null)
    val apiTestStatus: StateFlow<String?> = _apiTestStatus.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    val isFirebaseAILogicActive: Boolean
        get() = repository.isFirebaseAILogicActive()

    // Google Sign-In & Automatic Drive Backup Prompt on Startup
    private val _showGoogleSignInStartPrompt = MutableStateFlow(false)
    val showGoogleSignInStartPrompt: StateFlow<Boolean> = _showGoogleSignInStartPrompt.asStateFlow()

    // Study Data Flows
    val allPacks: StateFlow<List<StudyPack>> = repository.allStudyPacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReviews: StateFlow<List<ReviewScheduleItem>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGroups: StateFlow<List<StudyGroup>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _streakDays = MutableStateFlow(repository.getStreakDays())
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _isStudiedToday = MutableStateFlow(repository.isStudiedToday())
    val isStudiedToday: StateFlow<Boolean> = _isStudiedToday.asStateFlow()

    fun refreshStats() {
        _streakDays.value = repository.getStreakDays()
        _isStudiedToday.value = repository.isStudiedToday()
    }

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            refreshStats()
        }
        // If not signed in to Google and user hasn't dismissed onboarding account prompt, show Google Sign-In
        if (driveManager.getLastSignedInAccount() == null && !repository.hasDismissedGoogleSignInPrompt()) {
            _showGoogleSignInStartPrompt.value = true
        }
    }

    fun dismissGoogleSignInPrompt() {
        repository.setDismissedGoogleSignInPrompt(true)
        _showGoogleSignInStartPrompt.value = false
    }

    fun onGoogleSignInSuccess() {
        repository.setDismissedGoogleSignInPrompt(true)
        _showGoogleSignInStartPrompt.value = false
        autoBackupToDrive()
    }

    fun hasConfiguredApiKey(): Boolean {
        if (isFirebaseAILogicActive) return true
        val custom = _customApiKey.value.trim()
        if (custom.isNotEmpty()) return true
        val buildKey = runCatching { com.aizen.voltsage.BuildConfig.GEMINI_API_KEY }.getOrNull()?.trim() ?: ""
        return buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY"
    }

    fun autoBackupToDrive() {
        viewModelScope.launch {
            if (driveManager.getLastSignedInAccount() != null) {
                try {
                    val res = driveManager.uploadBackupToDrive()
                    if (res.isSuccess) {
                        android.util.Log.d("VoltSage", "Auto-backup to Google Drive succeeded: ${res.getOrNull()}")
                    } else {
                        android.util.Log.w("VoltSage", "Auto-backup notice: ${res.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("VoltSage", "Auto-backup error: ${e.message}")
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.Detail) {
            loadPackDetail(screen.packId)
        } else if (screen is Screen.Quiz) {
            startQuizSession(screen.packId)
        }
    }

    fun updateInputText(text: String) {
        _inputUrlOrText.value = text
    }

    fun setSubjectFilter(subject: String) {
        _selectedSubjectFilter.value = subject
    }

    fun setSelectedDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        repository.setCustomApiKey(key)
    }

    fun setSelectedModel(modelId: String) {
        _selectedModelId.value = modelId
        repository.setSelectedModel(modelId)
    }

    fun setAmoledTheme(enabled: Boolean) {
        _isAmoledTheme.value = enabled
        repository.setAmoledThemeEnabled(enabled)
    }

    fun testApiConnection() {
        val key = _customApiKey.value.trim()
        val model = _selectedModelId.value
        _isTestingApi.value = true
        _apiTestStatus.value = "Connecting to Gemini ($model)..."

        viewModelScope.launch {
            val result = repository.testGeminiConnection(key, model)
            _isTestingApi.value = false
            _apiTestStatus.value = if (result.isSuccess) {
                result.getOrNull()
            } else {
                "Connection failed: ${result.exceptionOrNull()?.message ?: "Check your API key"}"
            }
        }
    }

    fun updateDefaultQuizDifficulty(difficulty: String) {
        _defaultQuizDifficulty.value = difficulty
    }

    fun updateDefaultQuizQuestionCount(count: Int) {
        _defaultQuizQuestionCount.value = count
    }

    fun generateStudyPackFromInput(
        urlOrText: String? = null,
        difficulty: String = _defaultQuizDifficulty.value,
        questionCount: Int = _defaultQuizQuestionCount.value
    ) {
        val targetInput = (urlOrText ?: _inputUrlOrText.value).trim()
        if (targetInput.isBlank()) return

        _generationState.value = GenerationUiState(
            isGenerating = true,
            statusMessage = "Analyzing source content & extracting key takeaways..."
        )

        viewModelScope.launch {
            try {
                _generationState.value = GenerationUiState(
                    isGenerating = true,
                    statusMessage = "VoltSage synthesizing mental models & $difficulty quiz ($questionCount Qs)..."
                )
                val result = repository.processAndSaveStudyPack(
                    inputUrlOrText = targetInput,
                    difficulty = difficulty,
                    questionCount = questionCount
                )
                if (result.isSuccess) {
                    val pack = result.getOrThrow()
                    _generationState.value = GenerationUiState(isGenerating = false)
                    _inputUrlOrText.value = ""
                    autoBackupToDrive()
                    refreshStats()
                    navigateTo(Screen.Detail(pack.id))
                } else {
                    _generationState.value = GenerationUiState(
                        isGenerating = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to generate study pack"
                    )
                }
            } catch (e: Exception) {
                _generationState.value = GenerationUiState(
                    isGenerating = false,
                    errorMessage = e.localizedMessage ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun dismissGenerationError() {
        _generationState.value = _generationState.value.copy(errorMessage = null)
    }

    private fun loadPackDetail(packId: Long) {
        viewModelScope.launch {
            _activePack.value = repository.getStudyPackById(packId)
        }
    }

    fun toggleFavorite(packId: Long, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(packId, !currentFav)
            _activePack.value = _activePack.value?.let {
                if (it.id == packId) it.copy(isFavorite = !currentFav) else it
            }
        }
    }

    fun deleteStudyPack(packId: Long) {
        viewModelScope.launch {
            repository.deleteStudyPack(packId)
            autoBackupToDrive()
            if (_currentScreen.value is Screen.Detail || _currentScreen.value is Screen.Quiz) {
                _currentScreen.value = Screen.Home
            }
        }
    }

    // --- Quiz Logic ---
    fun startQuizSession(
        packId: Long,
        difficulty: String = _defaultQuizDifficulty.value,
        questionCount: Int = _defaultQuizQuestionCount.value
    ) {
        viewModelScope.launch {
            val pack = repository.getStudyPackById(packId)
            if (pack != null && pack.quizQuestions.isNotEmpty()) {
                val shuffled = pack.quizQuestions.map { it.withRandomizedOptions() }.shuffled()
                val selected = if (questionCount in 1 until shuffled.size) {
                    shuffled.take(questionCount)
                } else {
                    shuffled
                }
                _quizState.value = QuizSessionState(
                    packId = pack.id,
                    title = pack.title,
                    questions = selected,
                    currentIndex = 0,
                    selectedOptionIndex = null,
                    isAnswerConfirmed = false,
                    correctAnswersCount = 0,
                    isCompleted = false,
                    difficulty = difficulty,
                    requestedCount = if (questionCount > 0) questionCount else selected.size
                )
            }
        }
    }

    fun generateAndStartCustomQuiz(
        packId: Long,
        difficulty: String,
        questionCount: Int
    ) {
        val currentTitle = _activePack.value?.title ?: _quizState.value.title.ifEmpty { "Study Pack" }
        _quizState.value = _quizState.value.copy(
            packId = packId,
            title = currentTitle,
            isGeneratingNewQuiz = true,
            generationError = null,
            difficulty = difficulty,
            requestedCount = questionCount
        )

        viewModelScope.launch {
            val result = repository.generateCustomQuizForPack(packId, difficulty, questionCount)
            if (result.isSuccess) {
                val questions = result.getOrThrow()
                _quizState.value = QuizSessionState(
                    packId = packId,
                    title = currentTitle,
                    questions = questions,
                    currentIndex = 0,
                    selectedOptionIndex = null,
                    isAnswerConfirmed = false,
                    correctAnswersCount = 0,
                    isCompleted = false,
                    difficulty = difficulty,
                    requestedCount = questions.size,
                    isGeneratingNewQuiz = false,
                    generationError = null
                )
                _activePack.value = repository.getStudyPackById(packId)
            } else {
                _quizState.value = _quizState.value.copy(
                    isGeneratingNewQuiz = false,
                    generationError = result.exceptionOrNull()?.localizedMessage ?: "Failed to generate custom quiz questions"
                )
            }
        }
    }

    fun dismissQuizGenerationError() {
        _quizState.value = _quizState.value.copy(generationError = null)
    }

    fun selectQuizOption(index: Int) {
        if (!_quizState.value.isAnswerConfirmed) {
            _quizState.value = _quizState.value.copy(selectedOptionIndex = index)
        }
    }

    fun confirmQuizAnswer() {
        val state = _quizState.value
        val sel = state.selectedOptionIndex ?: return
        if (state.isAnswerConfirmed) return

        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        val isCorrect = sel == currentQ.correctIndex
        val newCorrectCount = if (isCorrect) state.correctAnswersCount + 1 else state.correctAnswersCount

        _quizState.value = state.copy(
            isAnswerConfirmed = true,
            correctAnswersCount = newCorrectCount
        )
    }

    fun nextQuizQuestion() {
        val state = _quizState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx < state.questions.size) {
            _quizState.value = state.copy(
                currentIndex = nextIdx,
                selectedOptionIndex = null,
                isAnswerConfirmed = false
            )
        } else {
            // Completed quiz!
            val total = state.questions.size
            val correct = state.correctAnswersCount
            val masteryPct = if (total > 0) ((correct.toFloat() / total) * 100).toInt() else 100

            viewModelScope.launch {
                repository.recordQuizResult(correct, total)
                repository.updateMastery(state.packId, masteryPct)
                autoBackupToDrive()
                refreshStats()
            }

            _quizState.value = state.copy(isCompleted = true)
        }
    }

    fun restartQuiz() {
        val state = _quizState.value
        val reRandomized = state.questions.map { it.withRandomizedOptions() }.shuffled()
        _quizState.value = state.copy(
            questions = reRandomized,
            currentIndex = 0,
            selectedOptionIndex = null,
            isAnswerConfirmed = false,
            correctAnswersCount = 0,
            isCompleted = false
        )
    }

    // --- Reviews ---
    fun toggleReviewCompleted(reviewId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.markReviewCompleted(reviewId, completed)
        }
    }

    fun addCustomReview(packId: Long, title: String, subject: String, dateStr: String, type: String) {
        viewModelScope.launch {
            repository.scheduleCustomReview(packId, title, subject, dateStr, type)
        }
    }

    fun deleteReview(reviewId: Long) {
        viewModelScope.launch {
            repository.deleteReview(reviewId)
        }
    }

    // --- Study Groups ---
    fun createGroup(name: String, subject: String, desc: String) {
        viewModelScope.launch {
            repository.createStudyGroup(name, subject, desc)
        }
    }

    fun joinGroup(code: String) {
        viewModelScope.launch {
            repository.joinStudyGroup(code)
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            repository.deleteStudyGroup(groupId)
        }
    }

    // --- Google Drive Auto Backup ---
    fun triggerAutoBackup() {
        com.aizen.voltsage.data.remote.AutoBackupService.triggerImmediateBackup(getApplication())
    }

    // --- Stats Getters ---
    fun getStreakDays(): Int = repository.getStreakDays()
    fun getTotalStudyMinutes(): Int = repository.getTotalStudyMinutes()
    fun getQuizzesTaken(): Int = repository.getQuizzesTaken()
    fun getQuizAccuracy(): Int = repository.getQuizAccuracyPercentage()

    fun getScholarRank(): ScholarRankInfo {
        val mins = getTotalStudyMinutes()
        val quizzes = getQuizzesTaken()
        val accuracy = getQuizAccuracy()
        val completedReviews = allReviews.value.count { it.completed }
        val packsCount = allPacks.value.size

        val accuracyBonus = if (quizzes > 0) (quizzes * (accuracy / 2)) else 0
        val xp = (mins * 10) + (quizzes * 50) + accuracyBonus + (completedReviews * 30) + (packsCount * 25)

        val levelCapacity = 250
        val level = 1 + (xp / levelCapacity)
        val currentLevelXp = xp % levelCapacity
        val progress = (currentLevelXp.toFloat() / levelCapacity.toFloat()).coerceIn(0f, 1f)

        val title = when (level) {
            1 -> "Novice Scholar"
            2 -> "Active Learner"
            3 -> "Recall Apprentice"
            4 -> "Lightning Polymath"
            5 -> "Knowledge Alchemist"
            else -> "Super Sage Grandmaster"
        }

        return ScholarRankInfo(
            level = level,
            title = title,
            totalXp = xp,
            currentLevelXp = currentLevelXp,
            levelCapacityXp = levelCapacity,
            progress = progress
        )
    }
}
