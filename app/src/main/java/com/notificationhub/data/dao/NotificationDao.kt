package com.notificationhub.data.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.notificationhub.data.entity.StoredNotification
import com.notificationhub.data.entity.AppPreference

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): LiveData<List<StoredNotification>>

    @Query("SELECT * FROM notifications WHERE isImportant = 1 ORDER BY timestamp DESC")
    fun getImportantNotifications(): LiveData<List<StoredNotification>>

    @Query("SELECT * FROM notifications WHERE isRead = 0 AND isImportant = 1 ORDER BY timestamp DESC")
    fun getUnreadImportantNotifications(): LiveData<List<StoredNotification>>

    @Query("SELECT * FROM notifications WHERE packageName LIKE :query OR appName LIKE :query OR title LIKE :query OR body LIKE :query ORDER BY timestamp DESC")
    fun searchNotifications(query: String): LiveData<List<StoredNotification>>

    @Query("SELECT * FROM notifications WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getNotificationsByDateRange(startTime: Long, endTime: Long): LiveData<List<StoredNotification>>

    @Insert
    suspend fun insertNotification(notification: StoredNotification): Long

    @Update
    suspend fun updateNotification(notification: StoredNotification)

    @Delete
    suspend fun deleteNotification(notification: StoredNotification)

    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTime")
    suspend fun deleteOldNotifications(cutoffTime: Long): Int

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long): Int

    @Query("UPDATE notifications SET isRead = 1 WHERE isImportant = 1 AND isRead = 0")
    suspend fun markAllImportantAsRead(): Int

    // App Preferences
    @Query("SELECT * FROM app_preferences")
    fun getAllAppPreferences(): LiveData<List<AppPreference>>

    @Query("SELECT * FROM app_preferences WHERE packageName = :packageName")
    suspend fun getAppPreference(packageName: String): AppPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppPreference(appPreference: AppPreference)

    @Update
    suspend fun updateAppPreference(appPreference: AppPreference)

    @Query("SELECT COALESCE(isImportant, 0) FROM app_preferences WHERE packageName = :packageName")
    suspend fun isAppImportant(packageName: String): Boolean

    // Add cleanup queries
    @Query("DELETE FROM notifications WHERE id NOT IN (SELECT id FROM notifications ORDER BY timestamp DESC LIMIT 1000)")
    suspend fun cleanupOldNotifications(): Int

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getNotificationCount(): Int
}
