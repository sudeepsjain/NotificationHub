package com.notificationhub.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import com.notificationhub.MainActivity
import com.notificationhub.R
import com.notificationhub.SmartNotifyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {

    private const val CHANNEL_ID = "smartnotify_summary"
    private const val SUMMARY_NOTIFICATION_ID = 1000
    private const val TAG = "NotificationHelper"

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val preferenceManager = SmartNotifyApplication.instance.preferenceManager

        val importance = if (preferenceManager.silentNotifications) {
            NotificationManager.IMPORTANCE_LOW
        } else {
            NotificationManager.IMPORTANCE_DEFAULT
        }

        val summaryChannel = NotificationChannel(
            CHANNEL_ID,
            "Smart Notifications",
            importance
        ).apply {
            description = "Summary of important notifications"
            if (preferenceManager.silentNotifications) {
                setSound(null, null)
                enableVibration(false)
            }
        }

        notificationManager.createNotificationChannel(summaryChannel)
        Log.d(TAG, "Notification channels created with silent: ${preferenceManager.silentNotifications}")
    }

    fun updateSummaryNotification(context: Context) {
        val repository = SmartNotifyApplication.instance.repository
        val preferenceManager = SmartNotifyApplication.instance.preferenceManager

        CoroutineScope(Dispatchers.Main).launch {
            repository.getUnreadImportantNotifications().observeForever(object : Observer<List<com.notificationhub.data.entity.StoredNotification>> {
                override fun onChanged(notifications: List<com.notificationhub.data.entity.StoredNotification>) {
                    repository.getUnreadImportantNotifications().removeObserver(this)

                    if (notifications.isEmpty()) {
                        cancelSummaryNotification(context)
                        return
                    }

                    try {
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        val intent = Intent(context, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(
                            context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val title = if (notifications.size == 1) {
                            "1 important notification"
                        } else {
                            "${notifications.size} important notifications"
                        }

                        val content = if (notifications.isNotEmpty()) {
                            "${notifications[0].appName}: ${notifications[0].title}"
                        } else {
                            "Tap to view notifications"
                        }

                        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle(title)
                            .setContentText(content)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(false)
                            .setOngoing(true)

                        if (preferenceManager.silentNotifications) {
                            notificationBuilder.setPriority(NotificationCompat.PRIORITY_LOW)
                                .setSound(null)
                                .setVibrate(null)
                        } else {
                            notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        }

                        val notification = notificationBuilder.build()

                        notificationManager.notify(SUMMARY_NOTIFICATION_ID, notification)
                        Log.d(TAG, "Summary notification updated: $title")

                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating summary notification", e)
                    }
                }
            })
        }
    }

    private fun cancelSummaryNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
        Log.d(TAG, "Summary notification cancelled")
    }
}