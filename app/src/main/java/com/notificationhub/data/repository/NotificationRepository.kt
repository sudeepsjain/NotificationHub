package com.notificationhub.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.notificationhub.data.dao.NotificationDao
import com.notificationhub.data.entity.StoredNotification
import com.notificationhub.data.entity.AppPreference
import android.util.Log
import kotlinx.coroutines.CancellationException

class NotificationRepository(private val notificationDao: NotificationDao) {

    companion object {
        private const val TAG = "NotificationRepository"
    }

    // Error handling
    private val _errors = MutableLiveData<String>()
    val errors: LiveData<String> get() = _errors

    fun getAllNotifications(): LiveData<List<StoredNotification>> = notificationDao.getAllNotifications()

    fun getImportantNotifications(): LiveData<List<StoredNotification>> = notificationDao.getImportantNotifications()

    fun getUnreadImportantNotifications(): LiveData<List<StoredNotification>> = notificationDao.getUnreadImportantNotifications()

    fun searchNotifications(query: String): LiveData<List<StoredNotification>> =
        notificationDao.searchNotifications("%$query%")

    fun getNotificationsByDateRange(startTime: Long, endTime: Long): LiveData<List<StoredNotification>> =
        notificationDao.getNotificationsByDateRange(startTime, endTime)

    suspend fun insertNotification(notification: StoredNotification): Result<Long> {
        return try {
            val id = notificationDao.insertNotification(notification)
            Log.d(TAG, "Notification inserted with ID: $id")
            Result.success(id)
        } catch (e: CancellationException) {
            throw e // Don't catch cancellation
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting notification", e)
            _errors.postValue("Failed to save notification: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateNotification(notification: StoredNotification): Result<Unit> {
        return try {
            notificationDao.updateNotification(notification)
            Log.d(TAG, "Notification updated: ${notification.id}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
            _errors.postValue("Failed to update notification: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notification: StoredNotification): Result<Unit> {
        return try {
            notificationDao.deleteNotification(notification)
            Log.d(TAG, "Notification deleted: ${notification.id}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification", e)
            _errors.postValue("Failed to delete notification: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteOldNotifications(cutoffTime: Long): Result<Int> {
        return try {
            val deletedCount = notificationDao.deleteOldNotifications(cutoffTime)
            Log.d(TAG, "Deleted $deletedCount old notifications")
            Result.success(deletedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting old notifications", e)
            _errors.postValue("Failed to cleanup old notifications: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun markAsRead(id: Long): Result<Unit> {
        return try {
            val updatedRows = notificationDao.markAsRead(id)
            if (updatedRows > 0) {
                Log.d(TAG, "Marked notification as read: $id")
                Result.success(Unit)
            } else {
                Log.w(TAG, "No notification found with ID: $id")
                Result.failure(Exception("Notification not found"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            _errors.postValue("Failed to mark notification as read: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun markAllImportantAsRead(): Result<Int> {
        return try {
            val updatedRows = notificationDao.markAllImportantAsRead()
            Log.d(TAG, "Marked $updatedRows important notifications as read")
            Result.success(updatedRows)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all important notifications as read", e)
            _errors.postValue("Failed to mark all as read: ${e.message}")
            Result.failure(e)
        }
    }

    // App Preferences
    fun getAllAppPreferences(): LiveData<List<AppPreference>> = notificationDao.getAllAppPreferences()

    suspend fun getAppPreference(packageName: String): AppPreference? {
        return try {
            notificationDao.getAppPreference(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app preference for $packageName", e)
            null
        }
    }

    suspend fun insertAppPreference(appPreference: AppPreference): Result<Unit> {
        return try {
            notificationDao.insertAppPreference(appPreference)
            Log.d(TAG, "App preference inserted: ${appPreference.packageName}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting app preference", e)
            _errors.postValue("Failed to save app preference: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateAppPreference(appPreference: AppPreference): Result<Unit> {
        return try {
            notificationDao.updateAppPreference(appPreference)
            Log.d(TAG, "App preference updated: ${appPreference.packageName}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app preference", e)
            _errors.postValue("Failed to update app preference: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun isAppImportant(packageName: String): Boolean {
        return try {
            notificationDao.isAppImportant(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is important: $packageName", e)
            false // Default to not important on error
        }
    }

    // Maintenance operations
    suspend fun performMaintenance(): Result<String> {
        return try {
            val notificationCount = notificationDao.getNotificationCount()
            val cleanedCount = if (notificationCount > 1000) {
                notificationDao.cleanupOldNotifications()
            } else {
                0
            }

            val message = "Maintenance completed. Total notifications: $notificationCount, Cleaned: $cleanedCount"
            Log.d(TAG, message)
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error during maintenance", e)
            Result.failure(e)
        }
    }
}