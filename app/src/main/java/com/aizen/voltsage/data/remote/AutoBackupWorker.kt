package com.aizen.voltsage.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "AutoBackupWorker"
        const val WORK_NAME_PERIODIC = "voltsage_periodic_drive_backup"
        const val WORK_NAME_ONETIME = "voltsage_onetime_drive_backup"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting Google Drive automatic backup background task...")
        val driveManager = GoogleDriveBackupManager(applicationContext)

        val accountInfo = driveManager.getAccountInfo()
        if (!accountInfo.isSignedIn) {
            Log.d(TAG, "User not signed in with Google. Skipping auto backup.")
            return Result.success()
        }

        return try {
            val backupResult = driveManager.uploadBackupToDrive()
            if (backupResult.isSuccess) {
                Log.d(TAG, "Google Drive auto-backup succeeded: ${backupResult.getOrNull()}")
                Result.success()
            } else {
                Log.w(TAG, "Google Drive auto-backup failed: ${backupResult.exceptionOrNull()?.message}")
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during automatic Google Drive backup", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
