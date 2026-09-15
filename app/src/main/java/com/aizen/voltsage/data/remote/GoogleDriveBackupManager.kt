package com.aizen.voltsage.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.aizen.voltsage.data.local.AppDatabase
import com.aizen.voltsage.data.local.ReviewScheduleEntity
import com.aizen.voltsage.data.local.StudyGroupEntity
import com.aizen.voltsage.data.local.StudyPackEntity
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DrivePermissionException(message: String) : Exception(message)

data class DriveAccountInfo(
    val isSignedIn: Boolean,
    val email: String?,
    val displayName: String?,
    val lastBackupTimestamp: Long = 0L,
    val hasDrivePermission: Boolean = false
)

class GoogleDriveBackupManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("voltsage_drive_backup", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val driveScope = Scope("https://www.googleapis.com/auth/drive.file")
    private val oauthScopeString = "oauth2:https://www.googleapis.com/auth/drive.file"

    companion object {
        private const val BACKUP_FILENAME_PREFIX = "voltsage_study_backup_"
        private const val BACKUP_FOLDER_NAME = "VoltSage Backups"
        private const val KEY_LAST_BACKUP = "last_backup_time"
        private const val TAG = "DriveBackup"
        private const val MAX_BACKUPS = 5
    }

    /**
     * Creates GoogleSignInClient. When [requestDriveScope] is false, uses standard
     * DEFAULT_SIGN_IN + requestEmail which succeeds without requiring verified OAuth Drive scopes.
     */
    fun getGoogleSignInClient(requestDriveScope: Boolean = false): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (requestDriveScope) {
            builder.requestScopes(driveScope)
        }
        return GoogleSignIn.getClient(context, builder.build())
    }

    /**
     * Returns the currently signed-in Google account, regardless of whether Drive scopes
     * are granted yet.
     */
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Checks if the user has explicitly granted Google Drive file permissions.
     */
    fun hasDrivePermission(account: GoogleSignInAccount? = getLastSignedInAccount()): Boolean {
        return account != null && GoogleSignIn.hasPermissions(account, driveScope)
    }

    fun getAccountInfo(): DriveAccountInfo {
        val account = getLastSignedInAccount()
        val lastTime = prefs.getLong(KEY_LAST_BACKUP, 0L)
        return DriveAccountInfo(
            isSignedIn = account != null,
            email = account?.email,
            displayName = account?.displayName ?: account?.email,
            lastBackupTimestamp = lastTime,
            hasDrivePermission = hasDrivePermission(account)
        )
    }

    fun clearCachedToken(token: String) {
        try {
            GoogleAuthUtil.clearToken(context, token)
            Log.d(TAG, "Cleared cached Google OAuth token")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear token", e)
        }
    }

    private fun parseDriveErrorMessage(code: Int, body: String): String {
        try {
            val root = JSONObject(body)
            val errorObj = root.optJSONObject("error")
            if (errorObj != null) {
                val message = errorObj.optString("message")
                if (message.contains("insufficient", ignoreCase = true) || message.contains("scope", ignoreCase = true)) {
                    return "Google Drive permission required. Please tap 'Authorize' to grant Drive file access."
                }
                if (message.contains("disabled", ignoreCase = true) || message.contains("not been used", ignoreCase = true)) {
                    return "Google Drive API is disabled in Cloud Console for this project. Please enable Google Drive API."
                }
                if (message.isNotBlank()) {
                    return "Google Drive error ($code): $message"
                }
            }
        } catch (_: Exception) {}
        return "Google Drive error ($code): ${body.take(150)}"
    }

    private fun encodeQueryParam(value: String): String {
        return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    private suspend fun getAuthToken(): String? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val gAccount = account.account ?: account.email?.let { android.accounts.Account(it, "com.google") }
            ?: return@withContext null
        try {
            GoogleAuthUtil.getToken(context, gAccount, oauthScopeString)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Google Drive OAuth token: ${e.message}", e)
            null
        }
    }

    /**
     * Serializes all user study packs, reviews, and custom groups into a structured JSON string.
     */
    suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val packs = db.studyPackDao().getAllPacks().firstOrNull() ?: emptyList()
        val reviews = db.reviewScheduleDao().getAllReviews().firstOrNull() ?: emptyList()
        val groups = db.studyGroupDao().getAllGroups().firstOrNull() ?: emptyList()

        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appName", "VoltSage")

        // Packs
        val packsArr = JSONArray()
        packs.forEach { p ->
            val obj = JSONObject().apply {
                put("title", p.title)
                put("sourceUrl", p.sourceUrl)
                put("sourceType", p.sourceType)
                put("subject", p.subject)
                put("summary", p.summary)
                put("detailedNotes", p.detailedNotes)
                put("keyTakeawaysJson", p.keyTakeawaysJson)
                put("quizQuestionsJson", p.quizQuestionsJson)
                put("createdAt", p.createdAt)
                put("lastReviewedAt", p.lastReviewedAt)
                put("nextReviewDate", p.nextReviewDate)
                put("masteryScore", p.masteryScore)
                put("isFavorite", p.isFavorite)
            }
            packsArr.put(obj)
        }
        root.put("studyPacks", packsArr)

        // Reviews
        val reviewsArr = JSONArray()
        reviews.forEach { r ->
            val obj = JSONObject().apply {
                put("studyPackTitle", r.studyPackTitle)
                put("subject", r.subject)
                put("scheduledDate", r.scheduledDate)
                put("completed", r.completed)
                put("reviewType", r.reviewType)
            }
            reviewsArr.put(obj)
        }
        root.put("reviews", reviewsArr)

        // Groups
        val groupsArr = JSONArray()
        groups.forEach { g ->
            val obj = JSONObject().apply {
                put("name", g.name)
                put("subject", g.subject)
                put("description", g.description)
                put("code", g.code)
                put("createdTimestamp", g.createdTimestamp)
                put("activeChallenge", g.activeChallenge)
            }
            groupsArr.put(obj)
        }
        root.put("groups", groupsArr)

        root.toString(2)
    }

    /**
     * Uploads a new backup file into the user's personal Google Drive under "VoltSage Backups"
     * (or root Drive if folder is unavailable), maintaining only the most recent MAX_BACKUPS.
     */
    suspend fun uploadBackupToDrive(): Result<String> = withContext(Dispatchers.IO) {
        val account = getLastSignedInAccount()
            ?: return@withContext Result.failure(Exception("Google Sign-In required. Please connect your Google account."))

        if (!hasDrivePermission(account)) {
            return@withContext Result.failure(DrivePermissionException("Google Drive permission required. Please authorize Google Drive access to enable backups."))
        }

        val token = getAuthToken()
            ?: return@withContext Result.failure(DrivePermissionException("Unable to obtain Google Drive authorization. Please tap 'Authorize' to grant Drive permission."))

        try {
            val jsonContent = generateBackupJson()
            
            // 1. Ensure folder exists (fallback to root if folder cannot be created/found)
            val folderId = getOrCreateFolderId(token)

            // 2. Upload new backup
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "$BACKUP_FILENAME_PREFIX$timestamp.json"

            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("description", "VoltSage Academic Study & Quiz Backup")
                put("mimeType", "application/json")
                if (folderId != null) {
                    put("parents", JSONArray().put(folderId))
                }
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("metadata", null, metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addFormDataPart("file", fileName, jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .build()

            val createRequest = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $token")
                .post(multipartBody)
                .build()

            val resp = client.newCall(createRequest).execute()
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: resp.message
                if (resp.code == 401 || resp.code == 403) {
                    clearCachedToken(token)
                }
                val friendlyErr = parseDriveErrorMessage(resp.code, err)
                return@withContext Result.failure(IOException(friendlyErr))
            }

            // 3. Cleanup old backups (only keep the 5 most recent)
            cleanupOldBackups(token, folderId)

            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_BACKUP, now).apply()
            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(now))
            Result.success("Backup uploaded to Google Drive successfully ($dateStr)")
        } catch (e: Exception) {
            Log.e(TAG, "Drive upload error", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the backup file from Google Drive and restores packs, schedules, and groups.
     */
    suspend fun restoreBackupFromDrive(): Result<String> = withContext(Dispatchers.IO) {
        val account = getLastSignedInAccount()
            ?: return@withContext Result.failure(Exception("Google Sign-In required. Please connect your Google account."))

        if (!hasDrivePermission(account)) {
            return@withContext Result.failure(DrivePermissionException("Google Drive permission required. Please authorize Google Drive access to restore backups."))
        }

        val token = getAuthToken()
            ?: return@withContext Result.failure(DrivePermissionException("Unable to obtain Google Drive authorization. Please tap 'Authorize' to grant Drive permission."))

        try {
            val fileId = findLatestBackupFileId(token)
                ?: return@withContext Result.failure(Exception("No VoltSage backup file found in Google Drive."))

            val downloadRequest = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val resp = client.newCall(downloadRequest).execute()
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: resp.message
                if (resp.code == 401 || resp.code == 403) {
                    clearCachedToken(token)
                }
                val friendlyErr = parseDriveErrorMessage(resp.code, err)
                return@withContext Result.failure(IOException(friendlyErr))
            }

            val body = resp.body?.string() ?: return@withContext Result.failure(IOException("Backup file was empty."))
            val count = applyRestoredData(body)
            Result.success("Successfully restored $count items from Google Drive!")
        } catch (e: Exception) {
            Log.e(TAG, "Drive restore error", e)
            Result.failure(e)
        }
    }

    suspend fun applyRestoredData(jsonString: String): Int = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val root = JSONObject(jsonString)
        var restoredCount = 0

        // Restore Study Packs
        if (root.has("studyPacks")) {
            val arr = root.getJSONArray("studyPacks")
            val existingPacks = db.studyPackDao().getAllPacks().firstOrNull() ?: emptyList()
            val existingTitles = existingPacks.map { it.title.lowercase() }.toSet()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val title = obj.getString("title")
                if (title.lowercase() !in existingTitles) {
                    val pack = StudyPackEntity(
                        title = title,
                        sourceUrl = obj.optString("sourceUrl", ""),
                        sourceType = obj.optString("sourceType", "ARTICLE"),
                        subject = obj.optString("subject", "General"),
                        summary = obj.optString("summary", ""),
                        detailedNotes = obj.optString("detailedNotes", ""),
                        keyTakeawaysJson = obj.optString("keyTakeawaysJson", "[]"),
                        quizQuestionsJson = obj.optString("quizQuestionsJson", "[]"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastReviewedAt = obj.optLong("lastReviewedAt", 0L),
                        nextReviewDate = obj.optString("nextReviewDate", ""),
                        masteryScore = obj.optInt("masteryScore", 0),
                        isFavorite = obj.optBoolean("isFavorite", false)
                    )
                    db.studyPackDao().insertPack(pack)
                    restoredCount++
                }
            }
        }

        // Restore Groups
        if (root.has("groups")) {
            val arr = root.getJSONArray("groups")
            val existingGroups = db.studyGroupDao().getAllGroups().firstOrNull() ?: emptyList()
            val existingCodes = existingGroups.map { it.code.uppercase() }.toSet()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val code = obj.getString("code").uppercase()
                if (code !in existingCodes) {
                    val group = StudyGroupEntity(
                        name = obj.getString("name"),
                        subject = obj.optString("subject", "General"),
                        description = obj.optString("description", ""),
                        code = code,
                        createdTimestamp = obj.optLong("createdTimestamp", System.currentTimeMillis()),
                        activeChallenge = obj.optString("activeChallenge", "Conquer weekly study goals")
                    )
                    db.studyGroupDao().insertGroup(group)
                    restoredCount++
                }
            }
        }

        restoredCount
    }

    /**
     * Directly exports study packs, reviews, and groups into any user-selected URI (SAF),
     * enabling instant backup to personal Google Drive (via Files app) or device storage.
     */
    suspend fun exportBackupToUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonContent = generateBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(jsonContent.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: return@withContext Result.failure(IOException("Could not open output destination for export."))

            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LAST_BACKUP, now).apply()
            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(now))
            Result.success("Backup successfully saved ($dateStr)")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting backup to URI", e)
            Result.failure(e)
        }
    }

    /**
     * Directly restores study packs and squads from any selected JSON file URI (SAF).
     */
    suspend fun restoreBackupFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext Result.failure(IOException("Could not open backup file for import."))

            val count = applyRestoredData(jsonString)
            Result.success("Restored $count items from selected backup file!")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup from URI", e)
            Result.failure(e)
        }
    }

    private suspend fun getOrCreateFolderId(token: String): String? = withContext(Dispatchers.IO) {
        val query = "name = '$BACKUP_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=${encodeQueryParam(query)}&fields=files(id,name)"
        
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
            
        try {
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrEmpty()) {
                val json = JSONObject(body)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val existingId = files.getJSONObject(0).getString("id")
                    Log.d(TAG, "Found existing backup folder in Drive: $existingId")
                    return@withContext existingId
                }
            } else {
                Log.w(TAG, "Search backup folder in Drive failed: HTTP ${resp.code}: $body")
                if (resp.code == 401 || resp.code == 403) {
                    clearCachedToken(token)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed finding backup folder in Drive", e)
        }
        
        // Create folder
        val metadataJson = JSONObject().apply {
            put("name", BACKUP_FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }.toString()
        
        val createReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files")
            .addHeader("Authorization", "Bearer $token")
            .post(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()
            
        try {
            val createResp = client.newCall(createReq).execute()
            val body = createResp.body?.string()
            if (createResp.isSuccessful && !body.isNullOrEmpty()) {
                val newId = JSONObject(body).getString("id")
                Log.d(TAG, "Created backup folder in Drive: $newId")
                return@withContext newId
            } else {
                Log.w(TAG, "Failed creating backup folder in Drive: HTTP ${createResp.code}: $body")
                if (createResp.code == 401 || createResp.code == 403) {
                    clearCachedToken(token)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed creating backup folder in Drive", e)
        }
        null
    }

    private suspend fun cleanupOldBackups(token: String, folderId: String?) = withContext(Dispatchers.IO) {
        val query = if (folderId != null) {
            "'$folderId' in parents and name contains '$BACKUP_FILENAME_PREFIX' and trashed = false"
        } else {
            "name contains '$BACKUP_FILENAME_PREFIX' and trashed = false"
        }
        val encodedQuery = encodeQueryParam(query)
        val encodedOrder = encodeQueryParam("createdTime")
        val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&orderBy=$encodedOrder&fields=files(id,createdTime)"
        
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
            
        try {
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: return@withContext
                val files = JSONObject(body).optJSONArray("files") ?: return@withContext
                
                if (files.length() > MAX_BACKUPS) {
                    val numToDelete = files.length() - MAX_BACKUPS
                    for (i in 0 until numToDelete) {
                        val fileId = files.getJSONObject(i).getString("id")
                        val deleteReq = Request.Builder()
                            .url("https://www.googleapis.com/drive/v3/files/$fileId")
                            .addHeader("Authorization", "Bearer $token")
                            .delete()
                            .build()
                        client.newCall(deleteReq).execute()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed cleaning up old backups", e)
        }
    }

    private suspend fun findLatestBackupFileId(token: String): String? = withContext(Dispatchers.IO) {
        val folderId = getOrCreateFolderId(token)
        val queries = mutableListOf<String>()
        if (folderId != null) {
            queries.add("'$folderId' in parents and name contains '$BACKUP_FILENAME_PREFIX' and trashed = false")
        }
        queries.add("name contains '$BACKUP_FILENAME_PREFIX' and trashed = false")

        for (query in queries) {
            val encodedQuery = encodeQueryParam(query)
            val encodedOrder = encodeQueryParam("createdTime desc")
            val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&orderBy=$encodedOrder&fields=files(id,name)"
            
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
                
            try {
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: continue
                    val json = JSONObject(body)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        return@withContext files.getJSONObject(0).getString("id")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed finding backup file with query: $query", e)
            }
        }
        null
    }

    fun signOut(onComplete: () -> Unit) {
        getGoogleSignInClient().signOut().addOnCompleteListener {
            prefs.edit().remove(KEY_LAST_BACKUP).apply()
            onComplete()
        }
    }
}
