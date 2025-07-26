package com.notificationhub.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.notificationhub.utils.NotificationHelper
import com.notificationhub.utils.ReAlertScheduler

class ReAlertReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REALERT = "com.smartnotify.REALERT"
        private const val TAG = "ReAlertReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_REALERT -> {
                Log.d(TAG, "Re-alert triggered")
                NotificationHelper.updateSummaryNotification(context)
                ReAlertScheduler.scheduleReAlert(context)
            }
        }
    }
}
