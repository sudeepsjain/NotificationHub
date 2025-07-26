package com.notificationhub.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.notificationhub.SmartNotifyApplication
import com.notificationhub.receiver.ReAlertReceiver

object ReAlertScheduler {

    private const val TAG = "ReAlertScheduler"
    private const val REQUEST_CODE = 1001

    fun scheduleReAlert(context: Context) {
        val preferenceManager = SmartNotifyApplication.instance.preferenceManager
        val interval = preferenceManager.reAlertInterval * 60 * 1000L // Convert to milliseconds

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReAlertReceiver::class.java).apply {
            action = ReAlertReceiver.ACTION_REALERT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + interval

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(TAG, "Re-alert scheduled for ${interval / 60000} minutes")
        } catch (e: SecurityException) {
            // Fallback to inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.d(TAG, "Using inexact alarm due to permission restriction")
        }
    }

    fun cancelReAlert(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReAlertReceiver::class.java).apply {
            action = ReAlertReceiver.ACTION_REALERT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Re-alert cancelled")
    }
}
