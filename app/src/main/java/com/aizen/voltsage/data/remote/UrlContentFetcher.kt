package com.aizen.voltsage.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedPageData(
    val title: String,
    val summarySnippet: String,
    val fullContext: String,
    val isVideo: Boolean
)

class UrlContentFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }

    suspend fun fetchUrlContent(input: String): ExtractedPageData = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            // Direct text input
            val lines = trimmed.lines().filter { it.isNotBlank() }
            val inferredTitle = lines.firstOrNull()?.take(60) ?: "Custom Study Material"
            return@withContext ExtractedPageData(
                title = inferredTitle,
                summarySnippet = trimmed.take(200),
                fullContext = trimmed,
                isVideo = false
            )
        }

        val isVideo = isVideoUrl(trimmed)

        // 1. If YouTube, use official oEmbed endpoint to get the 100% accurate video title and author
        if (trimmed.contains("youtube.com", ignoreCase = true) || trimmed.contains("youtu.be", ignoreCase = true)) {
            val ytData = fetchYouTubeOEmbed(trimmed)
            if (ytData != null) {
                return@withContext ytData
            }
        }

        // 2. If Vimeo, use Vimeo oEmbed
        if (trimmed.contains("vimeo.com", ignoreCase = true)) {
            val vimeoData = fetchVimeoOEmbed(trimmed)
            if (vimeoData != null) {
                return@withContext vimeoData
            }
        }

        // 3. General web page extraction
        try {
            val request = Request.Builder()
                .url(trimmed)
                .header("User-Agent", DESKTOP_UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val fallbackTitle = inferTitleFromUrl(trimmed)
                    return@withContext ExtractedPageData(
                        title = fallbackTitle,
                        summarySnippet = "Resource URL: $trimmed",
                        fullContext = "Study Topic Resource at $trimmed. Title: $fallbackTitle. Focus strictly on this academic subject.",
                        isVideo = isVideo
                    )
                }

                val html = response.body?.string() ?: ""
                val ogTitle = extractMetaProperty(html, "og:title")
                val pageTitle = extractTagContent(html, "title")
                val rawTitle = ogTitle ?: pageTitle ?: inferTitleFromUrl(trimmed)

                // Clean title - remove common suffix like " - Wikipedia", " | Khan Academy", etc.
                val title = cleanTitle(rawTitle, trimmed)
                val metaDesc = extractMetaProperty(html, "og:description") ?: extractMetaDescription(html) ?: ""
                val bodyText = extractCleanText(html)

                val context = buildString {
                    appendLine("Source Resource: $trimmed")
                    appendLine("Page Title: $title")
                    if (metaDesc.isNotBlank()) appendLine("Summary/Description: $metaDesc")
                    if (isVideo) appendLine("Content Type: Educational Video Lecture")
                    appendLine("Key Content Snippet:")
                    appendLine(bodyText.take(4000))
                }

                ExtractedPageData(
                    title = title.take(90),
                    summarySnippet = (metaDesc.ifBlank { bodyText }).take(250),
                    fullContext = context,
                    isVideo = isVideo
                )
            }
        } catch (e: Exception) {
            Log.w("VoltSage", "Could not fetch URL directly: ${e.message}")
            val fallbackTitle = inferTitleFromUrl(trimmed)
            ExtractedPageData(
                title = fallbackTitle,
                summarySnippet = "Study Link: $trimmed",
                fullContext = "Topic Link: $trimmed. Title: $fallbackTitle. Provide a comprehensive, high-yield academic study breakdown strictly on this subject.",
                isVideo = isVideo
            )
        }
    }

    private fun fetchYouTubeOEmbed(url: String): ExtractedPageData? {
        return try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", DESKTOP_UA)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val title = json.optString("title", "").trim()
                val author = json.optString("author_name", "").trim()
                if (title.isBlank()) return null

                val fullContext = buildString {
                    appendLine("Educational Video: $title")
                    if (author.isNotBlank()) appendLine("Instructor / Channel: $author")
                    appendLine("Source URL: $url")
                    appendLine("Platform: YouTube")
                    appendLine("Instruction: Generate comprehensive academic study notes, key takeaways, and an active recall quiz strictly covering the exact subject matter of '$title'. Ensure all concepts, terms, and explanations match this specific educational topic.")
                }

                ExtractedPageData(
                    title = title,
                    summarySnippet = if (author.isNotBlank()) "Video by $author on $title" else "Educational video: $title",
                    fullContext = fullContext,
                    isVideo = true
                )
            }
        } catch (e: Exception) {
            Log.w("VoltSage", "Error querying YouTube oEmbed: ${e.message}")
            null
        }
    }

    private fun fetchVimeoOEmbed(url: String): ExtractedPageData? {
        return try {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://vimeo.com/api/oembed.json?url=$encoded"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", DESKTOP_UA)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val title = json.optString("title", "").trim()
                val author = json.optString("author_name", "").trim()
                val desc = json.optString("description", "").trim()
                if (title.isBlank()) return null

                val fullContext = buildString {
                    appendLine("Video Title: $title")
                    if (author.isNotBlank()) appendLine("Instructor/Author: $author")
                    if (desc.isNotBlank()) appendLine("Description: $desc")
                    appendLine("Source URL: $url")
                    appendLine("Platform: Vimeo")
                }

                ExtractedPageData(
                    title = title,
                    summarySnippet = desc.ifBlank { "Video by $author: $title" },
                    fullContext = fullContext,
                    isVideo = true
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanTitle(raw: String, url: String): String {
        var t = raw.replace("\n", " ").replace("\r", " ").trim()
        t = t.replace(Regex("\\s+"), " ")
        // Remove common domain branding suffixes
        listOf(" - YouTube", "- YouTube", " - Wikipedia", " | Khan Academy", " | Coursera", " | edX").forEach { suffix ->
            if (t.endsWith(suffix, ignoreCase = true)) {
                t = t.substring(0, t.length - suffix.length).trim()
            }
        }
        if (t.equals("YouTube", ignoreCase = true) || t.isBlank()) {
            return inferTitleFromUrl(url)
        }
        return t
    }

    private fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be") ||
               lower.contains("vimeo.com") || lower.contains("tiktok.com") ||
               lower.contains("coursera.org/lecture") || lower.contains("edx.org/course")
    }

    private fun inferTitleFromUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val path = uri.path ?: ""
            val lastSegment = path.split("/").filter { it.isNotBlank() }.lastOrNull()
            if (!lastSegment.isNullOrBlank() && lastSegment.length > 3) {
                lastSegment.replace("-", " ")
                    .replace("_", " ")
                    .replace(".html", "")
                    .replace(".php", "")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            } else {
                uri.host ?: "Educational Study Topic"
            }
        } catch (e: Exception) {
            "Educational Study Topic"
        }
    }

    private fun extractTagContent(html: String, tagName: String): String? {
        val pattern = Pattern.compile("<$tagName[^>]*>(.*?)</$tagName>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        return if (matcher.find()) {
            matcher.group(1)?.replace(Regex("<.*?>"), "")?.trim()
        } else null
    }

    private fun extractMetaProperty(html: String, propertyName: String): String? {
        val pattern = Pattern.compile("<meta\\s+property=[\"']$propertyName[\"']\\s+content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)?.trim()

        val altPattern = Pattern.compile("<meta\\s+content=[\"'](.*?)[\"']\\s+property=[\"']$propertyName[\"']", Pattern.CASE_INSENSITIVE)
        val altMatcher = altPattern.matcher(html)
        return if (altMatcher.find()) altMatcher.group(1)?.trim() else null
    }

    private fun extractMetaDescription(html: String): String? {
        val pattern = Pattern.compile("<meta\\s+name=[\"']description[\"']\\s+content=[\"'](.*?)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)?.trim()

        val altPattern = Pattern.compile("<meta\\s+content=[\"'](.*?)[\"']\\s+name=[\"']description[\"']", Pattern.CASE_INSENSITIVE)
        val altMatcher = altPattern.matcher(html)
        return if (altMatcher.find()) altMatcher.group(1)?.trim() else null
    }

    private fun extractCleanText(html: String): String {
        // Strip scripts and styles
        var text = html.replace(Regex("(?s)<script.*?</script>"), "")
        text = text.replace(Regex("(?s)<style.*?</style>"), "")
        text = text.replace(Regex("(?s)<nav.*?</nav>"), "")
        text = text.replace(Regex("(?s)<footer.*?</footer>"), "")

        // Extract paragraph texts
        val paragraphs = mutableListOf<String>()
        val pPattern = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pPattern.matcher(text)
        while (matcher.find()) {
            val p = matcher.group(1)?.replace(Regex("<.*?>"), "")?.trim() ?: ""
            // Filter out cookie warnings, deprecation messages, bot interstitials
            if (p.length > 30 &&
                !p.contains("Your device is no longer supported", ignoreCase = true) &&
                !p.contains("Before you continue to", ignoreCase = true) &&
                !p.contains("Sign in to confirm you're not a bot", ignoreCase = true) &&
                !p.contains("cookie policy", ignoreCase = true)
            ) {
                paragraphs.add(p)
            }
        }
        return if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            text.replace(Regex("<.*?>"), " ").replace(Regex("\\s+"), " ").trim()
        }
    }
}
