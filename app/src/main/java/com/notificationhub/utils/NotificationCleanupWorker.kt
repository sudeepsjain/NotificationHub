// NotificationCleanupWorker.kt - Safe version with proper error handling
package com.notificationhub.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.notificationhub.SmartNotifyApplication
import java.util.concurrent.TimeUnit

class NotificationCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "notification_cleanup"
        private const val TAG = "NotificationCleanup"

        fun scheduleCleanup(context: Context) {
            try {
                // Check if WorkManager is available
                val workManager = WorkManager.getInstance(context)

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresCharging(false)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val cleanupRequest = PeriodicWorkRequestBuilder<NotificationCleanupWorker>(
                    1, TimeUnit.DAYS,
                    6, TimeUnit.HOURS // Flex interval
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    cleanupRequest
                )

                Log.d(TAG, "Scheduled daily cleanup successfully")

            } catch (e: IllegalStateException) {
                Log.e(TAG, "WorkManager not initialized when scheduling cleanup", e)
                // Don't crash - app can work without cleanup
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling cleanup work", e)
            }
        }

        fun cancelCleanup(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Canceled cleanup work")
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling cleanup work", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting cleanup work")

            val repository = SmartNotifyApplication.instance.repository
            val preferenceManager = SmartNotifyApplication.instance.preferenceManager

            val retentionDays = preferenceManager.retentionDays
            val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

            // Delete old notifications
            repository.deleteOldNotifications(cutoffTime)

            Log.i(TAG, "Cleanup completed successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Cleanup work failed", e)

            // Retry for database errors, fail for others
            if (e is java.sql.SQLException || e is android.database.sqlite.SQLiteException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}