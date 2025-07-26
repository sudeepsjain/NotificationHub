package com.notificationhub.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "smartnotify_prefs"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_REALERT_INTERVAL = "realert_interval"
        private const val KEY_DND_BEHAVIOR = "dnd_behavior"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NOTIFICATION_ACCESS_GRANTED = "notification_access_granted"
        private const val KEY_SILENT_NOTIFICATIONS = "silent_notifications"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    var retentionDays: Int
        get() = prefs.getInt(KEY_RETENTION_DAYS, 7)
        set(value) = prefs.edit().putInt(KEY_RETENTION_DAYS, value).apply()

    var reAlertInterval: Int
        get() = prefs.getInt(KEY_REALERT_INTERVAL, 15)
        set(value) = prefs.edit().putInt(KEY_REALERT_INTERVAL, value).apply()

    var dndBehavior: Boolean
        get() = prefs.getBoolean(KEY_DND_BEHAVIOR, false)
        set(value) = prefs.edit().putBoolean(KEY_DND_BEHAVIOR, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var notificationAccessGranted: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ACCESS_GRANTED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ACCESS_GRANTED, value).apply()

    var silentNotifications: Boolean
        get() = prefs.getBoolean(KEY_SILENT_NOTIFICATIONS, true) // Default to silent
        set(value) = prefs.edit().putBoolean(KEY_SILENT_NOTIFICATIONS, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true) // Default to true for first launch
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
}