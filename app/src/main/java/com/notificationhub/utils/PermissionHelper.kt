// PermissionHelper.kt - Enhanced version
package com.notificationhub.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.notificationhub.service.SmartNotificationListenerService

object PermissionHelper {
    private const val TAG = "PermissionHelper"

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val cn = ComponentName(context, SmartNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val enabled = flat != null && flat.contains(cn.flattenToString())
        Log.d(TAG, "Notification listener enabled: $enabled")
        return enabled
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification listener settings", e)
            // Fallback to general settings
            openAppSettings(context)
        }
    }

    fun openNotificationListenerSettingsWithGuidance(context: Context, onResult: (() -> Unit)? = null) {
        try {
            // Store callback for later use
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onResult?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification listener settings", e)
            openAppSettings(context)
        }
    }

    fun openAppNotificationSettings(context: Context) {
        try {
            // Try to open the specific notification settings for our app
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app notification settings", e)
            // Fallback to app settings
            openAppSettings(context)
        }
    }

    fun openNotificationAccessSettings(context: Context) {
        try {
            // Try to open notification access settings with our app pre-selected
            val intent = Intent().apply {
                action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Some OEMs support extras to pre-select the app
                putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification access settings", e)
            openNotificationListenerSettings(context)
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            permission == PackageManager.PERMISSION_GRANTED
        } else {
            // Before API 33, notification permission is granted by default
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification settings", e)
            openAppSettings(context)
        }
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app settings", e)
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Not applicable for older versions
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting battery optimization exemption", e)
                // Fallback to battery settings
                openBatteryOptimizationSettings(context)
            }
        }
    }

    private fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening battery optimization settings", e)
        }
    }

    fun getAllRequiredPermissions(): List<String> {
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add("notification_listener") // Custom identifier
            add("battery_optimization") // Custom identifier
        }
    }

    fun checkAllPermissions(context: Context): PermissionStatus {
        return PermissionStatus(
            canPostNotifications = canPostNotifications(context),
            hasNotificationListenerAccess = isNotificationListenerEnabled(context),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
        )
    }

    data class PermissionStatus(
        val canPostNotifications: Boolean,
        val hasNotificationListenerAccess: Boolean,
        val isIgnoringBatteryOptimizations: Boolean
    ) {
        val allPermissionsGranted: Boolean
            get() = canPostNotifications && hasNotificationListenerAccess

        val isFullyOptimized: Boolean
            get() = allPermissionsGranted && isIgnoringBatteryOptimizations
    }
}