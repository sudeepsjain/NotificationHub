// AppDatabase.kt - Fixed with proper error handling and migration
package com.notificationhub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notificationhub.data.dao.NotificationDao
import com.notificationhub.data.entity.StoredNotification
import com.notificationhub.data.entity.AppPreference
import android.util.Log

@Database(
    entities = [StoredNotification::class, AppPreference::class],
    version = 2, // Increment version for any schema changes
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2 (example)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Migrating database from version 1 to 2")
                // Add any schema changes here
                // Example: database.execSQL("ALTER TABLE notifications ADD COLUMN new_column TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "smartnotify_database"
                    )
                        .addMigrations(MIGRATION_1_2)
                        .fallbackToDestructiveMigration() // Only for development
                        .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // Better for small apps
                        .build()
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating database", e)
                    throw e
                }

                INSTANCE = instance
                Log.d(TAG, "Database created successfully")
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
