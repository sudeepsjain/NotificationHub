package com.notificationhub.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.notificationhub.service.ReAlertService
import com.notificationhub.utils.NotificationCleanupWorker
import com.notificationhub.utils.PermissionHelper

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Boot/restart received: ${intent.action}")

                // Only restart services if we have the necessary permissions
                if (PermissionHelper.isNotificationListenerEnabled(context)) {
                    startReAlertService(context)
                }

                // Reschedule cleanup work
                NotificationCleanupWorker.scheduleCleanup(context)

                Log.d(TAG, "Services restarted after boot/update")
            }
        }
    }

    private fun startReAlertService(context: Context) {
        try {
            val serviceIntent = Intent(context, ReAlertService::class.java)
            context.startForegroundService(serviceIntent)
            Log.d(TAG, "ReAlertService started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ReAlertService", e)
        }
    }
}
