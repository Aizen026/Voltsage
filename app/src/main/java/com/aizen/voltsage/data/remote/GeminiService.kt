package com.aizen.voltsage.data.remote

import android.util.Log
import com.aizen.voltsage.BuildConfig
import com.aizen.voltsage.data.model.QuizQuestion
import com.aizen.voltsage.data.model.withRandomizedOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AIStudyResult(
    val title: String,
    val subject: String,
    val summary: String,
    val detailedNotes: String,
    val keyTakeaways: List<String>,
    val quizQuestions: List<QuizQuestion>
)

class GeminiStudyService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateStudyPack(
        inputUrlOrText: String,
        extractedContent: String,
        difficulty: String = "Medium",
        questionCount: Int = 5,
        modelName: String = "gemini-3.5-flash",
        customApiKey: String? = null
    ): Result<AIStudyResult> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() && it != "MY_GEMINI_API_KEY" }

        // If no API key is set or placeholder, do NOT generate fake hallucinated study packs. Return failure so the user is prompted for their key.
        if (apiKey.isNullOrEmpty()) {
            Log.w("VoltSage", "No Gemini API Key provided.")
            return@withContext Result.failure(
                IllegalStateException("A Gemini API Key is required to summarize content and generate study packs. Please configure your API key in Gemini Settings.")
            )
        }

        val difficultyGuideline = when (difficulty.lowercase()) {
            "easy" -> "EASY difficulty: focus on fundamental definitions, core facts, straightforward comprehension, and clear distinctions. Distractors should be beginner-friendly."
            "hard" -> "HARD / ADVANCED difficulty: focus on multi-step reasoning, tricky plausible distractor options, edge cases, subtle distinctions, and exam-level scenario-based questions."
            else -> "MEDIUM difficulty: focus on standard application, mechanisms, cause-and-effect, and common conceptual misunderstandings."
        }

        val prompt = """
You are VoltSage, an expert academic tutor and study companion.
A student needs a comprehensive, high-retention study breakdown and quiz for the following study resource:

Resource / Topic: $inputUrlOrText
Context & Extracted Details:
$extractedContent

CRITICAL DIRECTIVES:
1. STRICT TOPIC FIDELITY: The generated title, summary, detailed notes, takeaways, and quiz questions MUST strictly focus on the real subject and topic described above. DO NOT drift into computer science, operating systems, or unrelated fields unless the source material is explicitly about that. For example, if the content is about Biology (e.g. cells, respiration, genetics, ecology), all notes, key takeaways, and quiz questions MUST be specifically and accurately about that biology topic.
2. ACCURATE SUBJECT: Assign the subject accurately ("Biology", "Physics", "STEM", "Computer Science", "History", "Literature", "Mathematics", "Economics", or "General").
3. HIGH-YIELD RETENTION: Break down difficult concepts into simple mental models, analogies, bulleted mechanisms, and numbered steps. Use simple, clear language without condescension.
4. QUIZ CONFIGURATION: Generate exactly $questionCount questions adhering to $difficultyGuideline.
5. QUIZ INTEGRITY: For each quiz question, provide 4 plausible options and distribute the correct answer randomly across positions 0, 1, 2, and 3 (Option A, B, C, or D). Include clear explanations for why the correct option is right.

Provide your response in STRICT VALID JSON format only, with no markdown code blocks, starting with '{' and ending with '}':
{
  "title": "Clear, accurate title of the topic",
  "subject": "One of: Biology, Physics, STEM, Computer Science, History, Literature, Mathematics, Economics, or General",
  "summary": "A punchy, high-yield executive summary in 2-3 engaging paragraphs that students can read in 60 seconds before an exam.",
  "detailedNotes": "Clear, detailed, but intuitive study notes. Break down difficult concepts into simple mental models, analogies, bulleted mechanisms, and numbered steps.",
  "keyTakeaways": [
    "Core Takeaway 1 with key definition or principle",
    "Core Takeaway 2 with critical mechanism",
    "Core Takeaway 3 with common exam pitfall or insight",
    "Core Takeaway 4 with practical application or formula"
  ],
  "quiz": [
    {
      "question": "Concept-testing question strictly on this topic",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "correctIndex": 0,
      "explanation": "Why this option is correct and why other options are common misconceptions."
    }
  ]
}
""".trimIndent()

        try {
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            val part = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(part)
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                val genConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                }
                put("generationConfig", genConfig)
            }

            val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(requestUrl)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("VoltSage", "API Error HTTP ${response.code}: $errBody")
                    val message = when (response.code) {
                        400 -> "Invalid request to Gemini API (HTTP 400). Please check your model selection."
                        401, 403 -> "Gemini API key is invalid or unauthorized (HTTP ${response.code}). Please verify your key in Settings."
                        429 -> "Gemini API quota exceeded or rate limit reached (HTTP 429). Please wait a moment or try another model."
                        else -> "Gemini API error (HTTP ${response.code})."
                    }
                    return@withContext Result.failure(Exception(message))
                }

                val respStr = response.body?.string() ?: ""
                val root = JSONObject(respStr)
                val candidates = root.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                val parsed = parseJsonResponse(text, inputUrlOrText)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Log.e("VoltSage", "Exception during Gemini generation: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun testApiKey(apiKey: String, modelName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", "Respond in one word: READY") })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(requestUrl)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Success: Model $modelName is active and connected!")
                } else {
                    val err = response.body?.string() ?: ""
                    Result.failure(Exception("HTTP ${response.code}: $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseJsonResponse(rawText: String, fallbackUrl: String): AIStudyResult {
        // Strip markdown backticks if present
        var cleaned = rawText.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }

        // Locate first { and last }
        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1)
        }

        val json = JSONObject(cleaned)
        val title = json.optString("title", "Study Overview")
        val subject = json.optString("subject", "General")
        val summary = json.optString("summary", "Key summary points generated for study review.")
        val detailedNotes = json.optString("detailedNotes", "Detailed conceptual breakdown.")

        val takeaways = mutableListOf<String>()
        val takeawaysArr = json.optJSONArray("keyTakeaways")
        if (takeawaysArr != null) {
            for (i in 0 until takeawaysArr.length()) {
                takeaways.add(takeawaysArr.getString(i))
            }
        }
        if (takeaways.isEmpty()) {
            takeaways.add("Consolidate fundamental concepts through active recall.")
            takeaways.add("Review flashcards every 3 to 7 days for long-term retention.")
        }

        val quizArr = json.optJSONArray("quiz")
        val questions = parseQuizQuestions(quizArr)

        return AIStudyResult(
            title = title,
            subject = subject,
            summary = summary,
            detailedNotes = detailedNotes,
            keyTakeaways = takeaways,
            quizQuestions = questions
        )
    }

    suspend fun generateCustomQuiz(
        topicTitle: String,
        studyContext: String,
        difficulty: String = "Medium",
        questionCount: Int = 5,
        modelName: String = "gemini-3.5-flash",
        customApiKey: String? = null
    ): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() && it != "MY_GEMINI_API_KEY" }

        if (apiKey.isNullOrEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("A Gemini API Key is required to generate custom quizzes. Configure your key in Settings.")
            )
        }

        val difficultyGuideline = when (difficulty.lowercase()) {
            "easy" -> "EASY difficulty: focus on core definitions, foundational facts, straightforward comprehension, and clear distinctions. Distractor options must be beginner-friendly."
            "hard" -> "HARD / ADVANCED difficulty: focus on multi-concept synthesis, tricky distractor options, edge cases, subtle distinctions, and exam-level scenario-based questions."
            else -> "MEDIUM difficulty: focus on standard conceptual applications, mechanisms, cause-and-effect, and common student misconceptions."
        }

        val prompt = """
You are VoltSage, an expert academic tutor.
Generate a targeted $questionCount-question quiz strictly based on the following topic and study material:

Topic: $topicTitle
Context & Notes:
$studyContext

DIFFICULTY LEVEL: $difficultyGuideline
NUMBER OF QUESTIONS: Exactly $questionCount questions.

QUIZ DIRECTIVES:
1. STRICT ACCURACY: Every question must test real concepts from the notes above.
2. 4 OPTIONS: Every question must have exactly 4 choices.
3. BALANCED ANSWER KEY: Distribute the correct answer evenly across positions 0, 1, 2, and 3.
4. DETAILED EXPLANATION: Explain why the correct option is right and clarify common traps.

Return STRICT VALID JSON only, with no markdown code blocks, starting with '{' and ending with '}':
{
  "quiz": [
    {
      "question": "Question text here",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "correctIndex": 0,
      "explanation": "Detailed explanation of the concept."
    }
  ]
}
""".trimIndent()

        try {
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                })
            }

            val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(requestUrl)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("VoltSage", "Custom Quiz API Error HTTP ${response.code}: $errBody")
                    return@withContext Result.failure(Exception("Gemini API error (HTTP ${response.code})"))
                }

                val respStr = response.body?.string() ?: ""
                val root = JSONObject(respStr)
                val candidates = root.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

                var cleaned = text.trim()
                if (cleaned.startsWith("```json")) cleaned = cleaned.removePrefix("```json").trim()
                else if (cleaned.startsWith("```")) cleaned = cleaned.removePrefix("```").trim()
                if (cleaned.endsWith("```")) cleaned = cleaned.removeSuffix("```").trim()

                val startIndex = cleaned.indexOf('{')
                val endIndex = cleaned.lastIndexOf('}')
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    cleaned = cleaned.substring(startIndex, endIndex + 1)
                }

                val json = JSONObject(cleaned)
                val quizArr = json.optJSONArray("quiz")
                val parsed = parseQuizQuestions(quizArr)
                if (parsed.isEmpty()) {
                    Result.failure(Exception("No questions were generated. Please try again."))
                } else {
                    Result.success(parsed)
                }
            }
        } catch (e: Exception) {
            Log.e("VoltSage", "Exception during custom quiz generation: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseQuizQuestions(quizArr: JSONArray?): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        if (quizArr != null) {
            for (i in 0 until quizArr.length()) {
                val qObj = quizArr.getJSONObject(i)
                val qText = qObj.optString("question", "Question ${i + 1}")
                val opts = mutableListOf<String>()
                val optsArr = qObj.optJSONArray("options")
                if (optsArr != null) {
                    for (j in 0 until optsArr.length()) {
                        opts.add(optsArr.getString(j))
                    }
                }
                val correct = qObj.optInt("correctIndex", 0)
                val expl = qObj.optString("explanation", "Review key takeaways for explanation.")
                if (opts.isNotEmpty()) {
                    questions.add(
                        QuizQuestion(
                            question = qText,
                            options = opts,
                            correctIndex = correct,
                            explanation = expl
                        ).withRandomizedOptions()
                    )
                }
            }
        }
        return questions
    }
}
