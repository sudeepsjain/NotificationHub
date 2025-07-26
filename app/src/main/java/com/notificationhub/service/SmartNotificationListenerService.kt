// SmartNotificationListenerService.kt - Fixed version
package com.notificationhub.service

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notificationhub.SmartNotifyApplication
import com.notificationhub.data.entity.StoredNotification
import com.notificationhub.utils.NotificationHelper
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class SmartNotificationListenerService : NotificationListenerService() {

    private val repository by lazy { SmartNotifyApplication.instance.repository }
    private val preferenceManager by lazy { SmartNotifyApplication.instance.preferenceManager }

    // Use SupervisorJob to prevent child failures from canceling the whole scope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SmartNotifyListener"
        private const val MAX_ICON_SIZE = 128 // Prevent memory issues with large icons
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { handleNotification(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification removed: ${sbn?.packageName}")
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification

        // Skip our own notifications
        if (packageName == applicationContext.packageName) return

        // Skip ongoing notifications (like music players, downloads)
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        // Skip group summary notifications
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        Log.d(TAG, "Processing notification from: $packageName")

        // Launch coroutine with exception handling
        serviceScope.launch {
            try {
                processNotification(sbn)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification from $packageName", e)
            }
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification

        // Check if app is marked as important
        val isImportant = repository.isAppImportant(packageName)

        // Get app info
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not get app name for $packageName")
            packageName
        }

        // Extract notification content
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val body = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Skip empty notifications
        if (title.isEmpty() && body.isEmpty()) {
            Log.d(TAG, "Skipping empty notification from $packageName")
            return
        }

        // Get app icon (with size limit)
        val iconData = try {
            val icon = packageManager.getApplicationIcon(packageName)
            drawableToByteArray(icon)
        } catch (e: Exception) {
            Log.w(TAG, "Could not get icon for $packageName")
            null
        }

        // Create stored notification
        val storedNotification = StoredNotification(
            appName = appName,
            packageName = packageName,
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            isImportant = isImportant,
            iconData = iconData
        )

        // Save to database
        repository.insertNotification(storedNotification)
        Log.d(TAG, "Stored notification: $appName - $title")

        // Update summary notification if important
        if (isImportant) {
            withContext(Dispatchers.Main) {
                NotificationHelper.updateSummaryNotification(applicationContext)
            }
        }
    }

    private fun drawableToByteArray(drawable: Drawable): ByteArray? {
        return try {
            // Limit icon size to prevent memory issues
            val width = minOf(drawable.intrinsicWidth.takeIf { it > 0 } ?: MAX_ICON_SIZE, MAX_ICON_SIZE)
            val height = minOf(drawable.intrinsicHeight.takeIf { it > 0 } ?: MAX_ICON_SIZE, MAX_ICON_SIZE)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream) // Reduced quality to save space

            // Cleanup
            bitmap.recycle()

            stream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error converting drawable to byte array", e)
            null
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListenerService connected")
        preferenceManager.notificationAccessGranted = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListenerService disconnected")
        preferenceManager.notificationAccessGranted = false

        // Cancel all pending operations
        serviceScope.coroutineContext.cancelChildren()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all coroutines when service is destroyed
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed, scope canceled")
    }
}