package com.aizen.voltsage.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.aizen.voltsage.data.local.AppDatabase
import com.aizen.voltsage.data.local.ReviewScheduleEntity
import com.aizen.voltsage.data.local.StudyGroupEntity
import com.aizen.voltsage.data.local.StudyPackEntity
import com.aizen.voltsage.data.model.QuizQuestion
import com.aizen.voltsage.data.model.withRandomizedOptions
import com.aizen.voltsage.data.model.ReviewScheduleItem
import com.aizen.voltsage.data.model.StudyGroup
import com.aizen.voltsage.data.model.StudyPack
import com.aizen.voltsage.data.remote.GeminiStudyService
import com.aizen.voltsage.data.remote.UrlContentFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VoltSageRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val studyPackDao = db.studyPackDao()
    private val reviewDao = db.reviewScheduleDao()
    private val groupDao = db.studyGroupDao()

    private val geminiService = GeminiStudyService()
    private val urlFetcher = UrlContentFetcher()

    private val prefs: SharedPreferences = context.getSharedPreferences("voltsage_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "custom_gemini_api_key"
        private const val KEY_SELECTED_MODEL = "selected_gemini_model"
        private const val KEY_AMOLED_THEME = "is_amoled_theme_enabled"
        private const val KEY_STREAK_DAYS = "study_streak_days"
        private const val KEY_LAST_STUDY_DATE = "last_study_date_str"
        private const val KEY_TOTAL_STUDY_MINUTES = "total_study_minutes"
        private const val KEY_TOTAL_QUIZZES_TAKEN = "total_quizzes_taken"
        private const val KEY_QUIZ_CORRECT_COUNT = "total_quiz_correct_count"
        private const val KEY_QUIZ_TOTAL_COUNT = "total_quiz_total_count"
        private const val KEY_INITIAL_SEEDED = "initial_data_seeded_v1"
        private const val KEY_DISMISSED_GOOGLE_SIGNIN_PROMPT = "dismissed_google_signin_prompt"
    }

    // --- Settings / Preferences ---
    fun hasDismissedGoogleSignInPrompt(): Boolean = prefs.getBoolean(KEY_DISMISSED_GOOGLE_SIGNIN_PROMPT, false)
    fun setDismissedGoogleSignInPrompt(dismissed: Boolean) = prefs.edit().putBoolean(KEY_DISMISSED_GOOGLE_SIGNIN_PROMPT, dismissed).apply()

    fun getCustomApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""
    fun setCustomApiKey(key: String) = prefs.edit().putString(KEY_API_KEY, key.trim()).apply()

    fun getSelectedModel(): String = prefs.getString(KEY_SELECTED_MODEL, "gemini-3.1-flash-lite-preview") ?: "gemini-3.1-flash-lite-preview"
    fun setSelectedModel(modelId: String) = prefs.edit().putString(KEY_SELECTED_MODEL, modelId).apply()

    fun isAmoledThemeEnabled(): Boolean = prefs.getBoolean(KEY_AMOLED_THEME, false)
    fun setAmoledThemeEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AMOLED_THEME, enabled).apply()

    fun getStreakDays(): Int {
        val lastDate = prefs.getString(KEY_LAST_STUDY_DATE, "") ?: ""
        if (lastDate.isBlank()) return 0
        val today = getTodayDateStr()
        val yesterday = getDateOffsetStr(-1)
        return if (lastDate == today || lastDate == yesterday) {
            prefs.getInt(KEY_STREAK_DAYS, 0)
        } else {
            0
        }
    }

    fun isStudiedToday(): Boolean {
        val lastDate = prefs.getString(KEY_LAST_STUDY_DATE, "") ?: ""
        return lastDate == getTodayDateStr()
    }

    fun getTotalStudyMinutes(): Int = prefs.getInt(KEY_TOTAL_STUDY_MINUTES, 0)
    fun getQuizzesTaken(): Int = prefs.getInt(KEY_TOTAL_QUIZZES_TAKEN, 0)

    fun getQuizAccuracyPercentage(): Int {
        val total = prefs.getInt(KEY_QUIZ_TOTAL_COUNT, 0)
        val correct = prefs.getInt(KEY_QUIZ_CORRECT_COUNT, 0)
        return if (total > 0) ((correct.toFloat() / total) * 100).toInt() else 0
    }

    fun recordQuizResult(correctQuestions: Int, totalQuestions: Int) {
        val currentQuizzes = prefs.getInt(KEY_TOTAL_QUIZZES_TAKEN, 0)
        val currentCorrect = prefs.getInt(KEY_QUIZ_CORRECT_COUNT, 0)
        val currentTotal = prefs.getInt(KEY_QUIZ_TOTAL_COUNT, 0)
        val currentMinutes = prefs.getInt(KEY_TOTAL_STUDY_MINUTES, 0)

        prefs.edit()
            .putInt(KEY_TOTAL_QUIZZES_TAKEN, currentQuizzes + 1)
            .putInt(KEY_QUIZ_CORRECT_COUNT, currentCorrect + correctQuestions)
            .putInt(KEY_QUIZ_TOTAL_COUNT, currentTotal + totalQuestions)
            .putInt(KEY_TOTAL_STUDY_MINUTES, currentMinutes + 8) // +8 min per quiz & review
            .apply()

        checkAndUpdateStreak()
    }

    private fun checkAndUpdateStreak() {
        val today = getTodayDateStr()
        val yesterday = getDateOffsetStr(-1)
        val lastDate = prefs.getString(KEY_LAST_STUDY_DATE, "") ?: ""
        val currentStreak = prefs.getInt(KEY_STREAK_DAYS, 0)

        if (lastDate == today) {
            // Already recorded study streak today
            return
        }

        val newStreak = if (lastDate == yesterday) {
            currentStreak + 1
        } else {
            1
        }

        prefs.edit()
            .putString(KEY_LAST_STUDY_DATE, today)
            .putInt(KEY_STREAK_DAYS, newStreak)
            .apply()
    }

    // --- Study Packs Flow ---
    val allStudyPacks: Flow<List<StudyPack>> = studyPackDao.getAllPacks().map { list ->
        list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    suspend fun getStudyPackById(id: Long): StudyPack? = withContext(Dispatchers.IO) {
        studyPackDao.getPackById(id)?.toDomain()
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        studyPackDao.updateFavorite(id, isFavorite)
    }

    suspend fun updateMastery(id: Long, score: Int) = withContext(Dispatchers.IO) {
        studyPackDao.updateMastery(id, score, System.currentTimeMillis())
    }

    suspend fun deleteStudyPack(id: Long) = withContext(Dispatchers.IO) {
        studyPackDao.deletePackById(id)
        reviewDao.deleteReviewsForPack(id)
    }

    suspend fun updateStudyPackQuizQuestions(packId: Long, questions: List<QuizQuestion>) = withContext(Dispatchers.IO) {
        studyPackDao.updateQuizQuestions(packId, questionsToJson(questions))
    }

    suspend fun generateCustomQuizForPack(
        packId: Long,
        difficulty: String = "Medium",
        questionCount: Int = 5
    ): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        try {
            val pack = getStudyPackById(packId)
                ?: return@withContext Result.failure(IllegalArgumentException("Study pack not found"))
            val context = buildString {
                appendLine("Summary:")
                appendLine(pack.summary)
                appendLine()
                appendLine("Detailed Notes:")
                appendLine(pack.detailedNotes)
                appendLine()
                appendLine("Key Takeaways:")
                pack.keyTakeaways.forEach { appendLine("- $it") }
            }
            val model = getSelectedModel()
            val customKey = getCustomApiKey()

            val result = geminiService.generateCustomQuiz(
                topicTitle = pack.title,
                studyContext = context,
                difficulty = difficulty,
                questionCount = questionCount,
                modelName = model,
                customApiKey = customKey
            )

            if (result.isSuccess) {
                val questions = result.getOrThrow()
                // Update the study pack questions with the newly generated questions
                updateStudyPackQuizQuestions(packId, questions)
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Generate New Study Pack from URL/Text ---
    suspend fun processAndSaveStudyPack(
        inputUrlOrText: String,
        subjectOverride: String? = null,
        difficulty: String = "Medium",
        questionCount: Int = 5
    ): Result<StudyPack> = withContext(Dispatchers.IO) {
        try {
            val extracted = urlFetcher.fetchUrlContent(inputUrlOrText)
            val model = getSelectedModel()
            val customKey = getCustomApiKey()

            val aiResult = geminiService.generateStudyPack(
                inputUrlOrText = inputUrlOrText,
                extractedContent = extracted.fullContext,
                difficulty = difficulty,
                questionCount = questionCount,
                modelName = model,
                customApiKey = customKey
            ).getOrThrow()

            val effectiveSubject = subjectOverride?.takeIf { it.isNotBlank() } ?: aiResult.subject
            val sourceType = if (extracted.isVideo) "VIDEO" else if (inputUrlOrText.startsWith("http")) "WEBSITE" else "TEXT_NOTE"

            val tomorrowStr = getDateOffsetStr(1)

            val randomizedQuestions = aiResult.quizQuestions.map { it.withRandomizedOptions() }
            val entity = StudyPackEntity(
                title = aiResult.title,
                sourceUrl = inputUrlOrText,
                sourceType = sourceType,
                subject = effectiveSubject,
                summary = aiResult.summary,
                detailedNotes = aiResult.detailedNotes,
                keyTakeawaysJson = listToJson(aiResult.keyTakeaways),
                quizQuestionsJson = questionsToJson(randomizedQuestions),
                createdAt = System.currentTimeMillis(),
                nextReviewDate = tomorrowStr,
                masteryScore = 50 // initial baseline
            )

            val insertedId = studyPackDao.insertPack(entity)

            // Auto-schedule spaced repetition reviews: Day +1, Day +3, Day +7
            val review1 = ReviewScheduleEntity(
                studyPackId = insertedId,
                studyPackTitle = aiResult.title,
                subject = effectiveSubject,
                scheduledDate = getDateOffsetStr(1),
                reviewType = "QUIZ"
            )
            val review2 = ReviewScheduleEntity(
                studyPackId = insertedId,
                studyPackTitle = aiResult.title,
                subject = effectiveSubject,
                scheduledDate = getDateOffsetStr(3),
                reviewType = "FLASHCARD"
            )
            val review3 = ReviewScheduleEntity(
                studyPackId = insertedId,
                studyPackTitle = aiResult.title,
                subject = effectiveSubject,
                scheduledDate = getDateOffsetStr(7),
                reviewType = "NOTES"
            )
            reviewDao.insertReview(review1)
            reviewDao.insertReview(review2)
            reviewDao.insertReview(review3)

            checkAndUpdateStreak()

            val saved = studyPackDao.getPackById(insertedId)?.toDomain()
                ?: throw IllegalStateException("Failed to load saved pack")
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Review Calendar Schedules ---
    val allReviews: Flow<List<ReviewScheduleItem>> = reviewDao.getAllReviews().map { list ->
        list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    fun getReviewsForDate(dateStr: String): Flow<List<ReviewScheduleItem>> =
        reviewDao.getReviewsForDate(dateStr).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)

    suspend fun markReviewCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        reviewDao.updateCompleted(id, completed)
        if (completed) {
            checkAndUpdateStreak()
        }
    }

    suspend fun scheduleCustomReview(studyPackId: Long, packTitle: String, subject: String, dateStr: String, type: String) = withContext(Dispatchers.IO) {
        val review = ReviewScheduleEntity(
            studyPackId = studyPackId,
            studyPackTitle = packTitle,
            subject = subject,
            scheduledDate = dateStr,
            reviewType = type
        )
        reviewDao.insertReview(review)
    }

    suspend fun deleteReview(id: Long) = withContext(Dispatchers.IO) {
        reviewDao.deleteReviewById(id)
    }

    // --- Collaborative Study Groups ---
    val allGroups: Flow<List<StudyGroup>> = groupDao.getAllGroups().map { list ->
        list.map { it.toDomain() }
    }.flowOn(Dispatchers.IO)

    suspend fun createStudyGroup(name: String, subject: String, description: String): Long = withContext(Dispatchers.IO) {
        val code = "VOLT-" + (1000..9999).random()
        val group = StudyGroupEntity(
            name = name,
            subject = subject,
            description = description,
            code = code,
            activeChallenge = "Score 85%+ on $subject Quiz"
        )
        groupDao.insertGroup(group)
    }

    suspend fun joinStudyGroup(code: String): Boolean = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase()
        val existing = groupDao.getAllGroups().firstOrNull()?.find { it.code.equals(cleanCode, ignoreCase = true) }
        
        return@withContext existing != null
    }

    suspend fun deleteStudyGroup(id: Long) = withContext(Dispatchers.IO) {
        groupDao.deleteGroupById(id)
    }

    suspend fun testGeminiConnection(apiKey: String, modelId: String): Result<String> {
        return geminiService.testConnection(apiKey.takeIf { it.isNotBlank() }, modelId)
    }

    fun isFirebaseAILogicActive(): Boolean {
        return geminiService.isFirebaseReady()
    }

    // --- Seed Initial Starter Content ---
    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        // Real-world, zero pre-generated fake packs or groups.
        // User creates genuine study packs and squad codes as needed.
        prefs.edit().putBoolean(KEY_INITIAL_SEEDED, true).apply()
    }

    // --- Helpers ---
    fun getTodayDateStr(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getDateOffsetStr(daysOffset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }

    private fun listToJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun jsonToList(jsonStr: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (e: Exception) {
            if (jsonStr.isNotBlank()) list.add(jsonStr)
        }
        return list
    }

    private fun questionsToJson(questions: List<QuizQuestion>): String {
        val arr = JSONArray()
        questions.forEach { q ->
            val obj = JSONObject().apply {
                put("id", q.id)
                put("question", q.question)
                val optsArr = JSONArray()
                q.options.forEach { optsArr.put(it) }
                put("options", optsArr)
                put("correctIndex", q.correctIndex)
                put("explanation", q.explanation)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun jsonToQuestions(jsonStr: String): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optsArr = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optsArr.length()) {
                    opts.add(optsArr.getString(j))
                }
                list.add(QuizQuestion(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    question = obj.getString("question"),
                    options = opts,
                    correctIndex = obj.getInt("correctIndex"),
                    explanation = obj.optString("explanation", "")
                ))
            }
        } catch (e: Exception) {
            // Empty if parsing fails
        }
        return list
    }

    private fun StudyPackEntity.toDomain(): StudyPack {
        return StudyPack(
            id = id,
            title = title,
            sourceUrl = sourceUrl,
            sourceType = sourceType,
            subject = subject,
            summary = summary,
            detailedNotes = detailedNotes,
            keyTakeaways = jsonToList(keyTakeawaysJson),
            quizQuestions = jsonToQuestions(quizQuestionsJson),
            createdAt = createdAt,
            lastReviewedAt = lastReviewedAt,
            nextReviewDate = nextReviewDate,
            masteryScore = masteryScore,
            isFavorite = isFavorite,
            sharedInGroupId = sharedInGroupId
        )
    }

    private fun ReviewScheduleEntity.toDomain(): ReviewScheduleItem {
        return ReviewScheduleItem(
            id = id,
            studyPackId = studyPackId,
            studyPackTitle = studyPackTitle,
            subject = subject,
            scheduledDate = scheduledDate,
            completed = completed,
            reviewType = reviewType
        )
    }

    private fun StudyGroupEntity.toDomain(): StudyGroup {
        return StudyGroup(
            id = id,
            name = name,
            subject = subject,
            description = description,
            code = code,
            createdTimestamp = createdTimestamp,
            activeChallenge = activeChallenge
        )
    }
}
