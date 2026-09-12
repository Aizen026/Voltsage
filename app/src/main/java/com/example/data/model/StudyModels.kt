package com.example.data.model

data class QuizQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

fun QuizQuestion.withRandomizedOptions(): QuizQuestion {
    if (options.size <= 1) return this
    val correctText = options.getOrNull(correctIndex) ?: return this
    val shuffled = options.shuffled()
    val newIndex = shuffled.indexOf(correctText)
    return this.copy(
        options = shuffled,
        correctIndex = if (newIndex >= 0) newIndex else 0
    )
}

data class StudyPack(
    val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val sourceType: String, // WEBSITE, VIDEO, ARTICLE, DOC
    val subject: String,
    val summary: String,
    val detailedNotes: String,
    val keyTakeaways: List<String>,
    val quizQuestions: List<QuizQuestion>,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long = 0,
    val nextReviewDate: String = "", // YYYY-MM-DD
    val masteryScore: Int = 0, // 0 - 100
    val isFavorite: Boolean = false,
    val sharedInGroupId: Long? = null
)

data class ReviewScheduleItem(
    val id: Long = 0,
    val studyPackId: Long,
    val studyPackTitle: String,
    val subject: String,
    val scheduledDate: String, // YYYY-MM-DD
    val completed: Boolean = false,
    val reviewType: String = "QUIZ" // QUIZ, FLASHCARD, NOTES
)

data class StudyGroup(
    val id: Long = 0,
    val name: String,
    val subject: String,
    val description: String,
    val code: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val activeChallenge: String = "Complete weekly quiz review"
)

data class ScholarRankInfo(
    val level: Int,
    val title: String,
    val totalXp: Int,
    val currentLevelXp: Int,
    val levelCapacityXp: Int,
    val progress: Float
)

data class GeminiModelOption(
    val id: String,
    val friendlyName: String,
    val subtitle: String,
    val badge: String,
    val description: String,
    val speedScore: String,
    val reasoningScore: String,
    val recommendedFor: String
)

val AvailableGeminiModels = listOf(
    GeminiModelOption(
        id = "gemini-3.1-flash-lite-preview",
        friendlyName = "Gemini 3.1 Flash-Lite",
        subtitle = "Ultra-Low Latency & High Efficiency",
        badge = "Most Efficient",
        description = "Google's lightest, fastest preview model engineered for extreme speed and cost efficiency. Perfect for real-time mobile study packs, active recall quizzes, and instant takeaways with minimal latency.",
        speedScore = "Instant (<0.5s)",
        reasoningScore = "High-Yield (8.8/10)",
        recommendedFor = "Instant study packs, rapid recall, lowest token cost & latency"
    ),
    GeminiModelOption(
        id = "gemini-2.5-flash",
        friendlyName = "Gemini 2.5 Flash",
        subtitle = "High-Speed Multimodal Intelligence",
        badge = "Fast & Balanced",
        description = "Next-generation workhorse model with sub-second response times and high efficiency for study summaries, lecture notes, and active recall quizzes.",
        speedScore = "Lightning (<1s)",
        reasoningScore = "High (9.5/10)",
        recommendedFor = "Everyday study packs, quick recall, web & video ingestion"
    ),
    GeminiModelOption(
        id = "gemini-3.5-flash",
        friendlyName = "Gemini 3.5 Flash",
        subtitle = "Next-Gen High-Throughput Intelligence",
        badge = "Top Quality Flash",
        description = "Cutting-edge high-throughput flash architecture balancing state-of-the-art comprehension with blazing-fast generation speed.",
        speedScore = "Lightning (<1s)",
        reasoningScore = "Exceptional (9.8/10)",
        recommendedFor = "Real-time interactive study sessions, extensive syllabus synthesis"
    ),
    GeminiModelOption(
        id = "gemini-2.5-pro",
        friendlyName = "Gemini 2.5 Pro",
        subtitle = "Advanced Reasoning & Complex Problems",
        badge = "Deep Thinking",
        description = "State-of-the-art reasoning model designed for multi-step STEM problem solving, academic research papers, algorithmic proofs, and deep conceptual synthesis.",
        speedScore = "Standard (2-4s)",
        reasoningScore = "Maximum (10/10)",
        recommendedFor = "Advanced STEM, dense academic papers, complex multi-step quizzes"
    ),
    GeminiModelOption(
        id = "gemini-3.1-pro-preview",
        friendlyName = "Gemini 3.1 Pro Preview",
        subtitle = "Next-Gen Frontier Reasoning",
        badge = "Frontier Pro",
        description = "Next-generation frontier reasoning model exceeding previous capabilities on complex logical deduction, mathematical derivation, and doctoral-level texts.",
        speedScore = "Standard (2-5s)",
        reasoningScore = "Frontier (10/10)",
        recommendedFor = "Doctoral research, proofs, dense mathematical formulations"
    )
)
