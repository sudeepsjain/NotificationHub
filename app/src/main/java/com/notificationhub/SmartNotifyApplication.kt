// 1. SmartNotifyApplication.kt - Add WorkManager initialization
package com.notificationhub

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.notificationhub.data.AppDatabase
import com.notificationhub.data.repository.NotificationRepository
import com.notificationhub.utils.PreferenceManager

class SmartNotifyApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "SmartNotifyApp"
        lateinit var instance: SmartNotifyApplication
            private set
    }

    val database by lazy {
        Log.d(TAG, "Initializing database")
        AppDatabase.getDatabase(this)
    }

    val repository by lazy {
        Log.d(TAG, "Initializing repository")
        NotificationRepository(database.notificationDao())
    }

    val preferenceManager by lazy {
        Log.d(TAG, "Initializing preference manager")
        PreferenceManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "NotificationHub application created")

        // Initialize crash handling
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", exception)
        }
    }

    // WorkManager Configuration
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "Memory trim requested: $level")

        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                System.gc()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning")
        System.gc()
    }
}