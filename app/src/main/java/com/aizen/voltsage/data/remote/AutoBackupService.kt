package com.aizen.voltsage.data.remote

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Service to manage and schedule automatic Google Drive backups for VoltSage.
 * Encapsulates background periodic scheduling, one-time execution, and pruning to retain
 * the 5 most recent backups.
 */
object AutoBackupService {
    private const val TAG = "AutoBackupService"

    /**
     * Schedules a periodic background auto-backup with WorkManager.
     * Requires an active network connection and runs every [repeatIntervalHours] hours.
     */
    fun schedulePeriodicBackup(
        context: Context,
        repeatIntervalHours: Long = 24L
    ) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                repeatIntervalHours,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .addTag("voltsage_drive_backup")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AutoBackupWorker.WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Enqueued periodic Google Drive backup worker (every $repeatIntervalHours hours).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic backup", e)
        }
    }

    /**
     * Triggers an immediate one-time background backup to Google Drive.
     */
    fun triggerImmediateBackup(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    5,
                    TimeUnit.MINUTES
                )
                .addTag("voltsage_drive_backup")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                AutoBackupWorker.WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Enqueued immediate Google Drive auto-backup.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger immediate backup", e)
        }
    }

    /**
     * Cancels all scheduled Google Drive auto-backup tasks.
     */
    fun cancelAutoBackup(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(AutoBackupWorker.WORK_NAME_PERIODIC)
            WorkManager.getInstance(context).cancelUniqueWork(AutoBackupWorker.WORK_NAME_ONETIME)
            Log.d(TAG, "Cancelled Google Drive auto-backup tasks.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel auto-backup tasks", e)
        }
    }
}
