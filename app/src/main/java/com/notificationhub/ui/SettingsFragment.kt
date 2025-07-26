// File: app/src/main/java/com/smartnotify/ui/SettingsFragment.kt
package com.notificationhub.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.notificationhub.PrivacyPolicyActivity
import com.notificationhub.R
import com.notificationhub.SmartNotifyApplication
import com.notificationhub.databinding.FragmentSettingsBinding
import com.notificationhub.utils.PermissionHelper
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val preferenceManager by lazy { SmartNotifyApplication.instance.preferenceManager }
    private val repository by lazy { SmartNotifyApplication.instance.repository }

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        updatePermissionStatus()
        updateStorageInfo()
        updateAppVersion()
    }

    private fun setupViews() {
        // Re-alert interval setup
        binding.reAlertSeekBar.progress = (preferenceManager.reAlertInterval - 1).coerceIn(0, 59)
        updateReAlertText(preferenceManager.reAlertInterval)

        // Retention period setup
        binding.retentionSeekBar.progress = (preferenceManager.retentionDays - 1).coerceIn(0, 29)
        updateRetentionText(preferenceManager.retentionDays)

        // DND settings
        binding.dndSwitch.isChecked = preferenceManager.dndBehavior

        // Silent notification settings
        binding.silentNotificationsSwitch.isChecked = preferenceManager.silentNotifications

        // Update notification management status
        updateNotificationManagementStatus()

        // SeekBar listeners
        binding.reAlertSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = progress + 1
                updateReAlertText(minutes)
                if (fromUser) {
                    preferenceManager.reAlertInterval = minutes
                    showToast("Re-alert interval updated to $minutes minutes")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.retentionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val days = progress + 1
                updateRetentionText(days)
                if (fromUser) {
                    preferenceManager.retentionDays = days
                    showToast("Retention period updated to $days days")
                    updateStorageInfo() // Refresh storage info
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.dndSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.dndBehavior = isChecked
            showToast("DND behavior ${if (isChecked) "enabled" else "disabled"}")
        }

        binding.silentNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.silentNotifications = isChecked
            showToast("Silent notifications ${if (isChecked) "enabled" else "disabled"}")
        }
    }

    private fun setupClickListeners() {
        // Permission buttons
        binding.notificationPermissionButton.setOnClickListener {
            showNotificationPermissionGuidance()
        }

        binding.batteryOptimizationButton.setOnClickListener {
            PermissionHelper.requestIgnoreBatteryOptimizations(requireContext())
        }

        // Notification management
        binding.manageNotificationsButton.setOnClickListener {
            PermissionHelper.openAppSettings(requireContext())
        }

        // Data management
        binding.clearDataButton.setOnClickListener {
            showClearDataDialog()
        }

        // About
        binding.aboutButton.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun updateReAlertText(minutes: Int) {
        binding.reAlertIntervalText.text = "Re-alert every $minutes minutes"
    }

    private fun updateRetentionText(days: Int) {
        binding.retentionDaysText.text = "Keep notifications for $days days"
    }

    private fun updatePermissionStatus() {
        val permissionStatus = PermissionHelper.checkAllPermissions(requireContext())

        if (permissionStatus.allPermissionsGranted) {
            // Hide permission card and update status
            binding.permissionStatusCard.visibility = View.GONE
            binding.appStatusIndicator.setBackgroundColor(requireContext().getColor(R.color.success_color))
            binding.appStatusText.text = "Active"
            binding.appStatusText.setTextColor(requireContext().getColor(R.color.success_color))
        } else {
            // Show permission card and update indicators
            binding.permissionStatusCard.visibility = View.VISIBLE
            binding.appStatusIndicator.setBackgroundColor(requireContext().getColor(R.color.warning_color))
            binding.appStatusText.text = "Setup needed"
            binding.appStatusText.setTextColor(requireContext().getColor(R.color.warning_color))

            // Update individual permission indicators
            updatePermissionIndicator(
                binding.notificationPermissionIndicator,
                binding.notificationPermissionButton,
                permissionStatus.hasNotificationListenerAccess,
                "Granted", "Grant"
            )

            updatePermissionIndicator(
                binding.batteryPermissionIndicator,
                binding.batteryOptimizationButton,
                permissionStatus.isIgnoringBatteryOptimizations,
                "Disabled", "Disable"
            )
        }
    }

    private fun updateNotificationManagementStatus() {
        val permissionStatus = PermissionHelper.checkAllPermissions(requireContext())
        
        // Update notification status text and indicator
        if (permissionStatus.canPostNotifications) {
            binding.notificationStatusText.text = getString(R.string.notifications_enabled)
            binding.notificationStatusText.setTextColor(requireContext().getColor(R.color.success_color))
            binding.notificationStatusIndicator.setBackgroundColor(requireContext().getColor(R.color.success_color))
        } else {
            binding.notificationStatusText.text = getString(R.string.notifications_disabled)
            binding.notificationStatusText.setTextColor(requireContext().getColor(R.color.warning_color))
            binding.notificationStatusIndicator.setBackgroundColor(requireContext().getColor(R.color.warning_color))
        }
    }

    private fun updatePermissionIndicator(
        indicator: View,
        button: android.widget.Button,
        isGranted: Boolean,
        grantedText: String,
        notGrantedText: String
    ) {
        if (isGranted) {
            indicator.setBackgroundColor(requireContext().getColor(R.color.success_color))
            button.text = grantedText
            button.isEnabled = false
        } else {
            indicator.setBackgroundColor(requireContext().getColor(R.color.warning_color))
            button.text = notGrantedText
            button.isEnabled = true
        }
    }

    private fun updateStorageInfo() {
        lifecycleScope.launch {
            try {
                // Get notification count and estimate storage
                repository.getAllNotifications().observe(viewLifecycleOwner) { notifications ->
                    val count = notifications.size
                    val estimatedSizeMB = (count * 0.5).toInt() // Rough estimate: 0.5KB per notification

                    binding.storageInfoText.text = if (estimatedSizeMB > 0) {
                        "$count notifications (~${estimatedSizeMB}KB)"
                    } else {
                        "$count notifications"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating storage info", e)
                binding.storageInfoText.text = "Unable to calculate"
            }
        }
    }

    private fun updateAppVersion() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.versionText.text = packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.versionText.text = "Unknown"
        }
    }


    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all notifications and app preferences. This action cannot be undone.\n\nAre you sure you want to continue?")
            .setPositiveButton("Clear All") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun clearAllData() {
        lifecycleScope.launch {
            try {
                // Delete all notifications
                repository.getAllNotifications().observe(viewLifecycleOwner) { notifications ->
                    lifecycleScope.launch {
                        notifications.forEach { notification ->
                            repository.deleteNotification(notification)
                        }

                        // Clear app preferences
                        repository.getAllAppPreferences().observe(viewLifecycleOwner) { preferences ->
                            lifecycleScope.launch {
                                preferences.forEach { preference ->
                                    // Reset to non-important but keep the record
                                    repository.updateAppPreference(preference.copy(isImportant = false))
                                }

                                showToast("All data cleared successfully")
                                updateStorageInfo()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing data", e)
                showToast("Failed to clear data: ${e.message}")
            }
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About NotificationHub")
            .setMessage("""
                NotificationHub v1.0
                Smart notification manager with re-alerting and prioritization.
                
                Features:
                • Smart notification filtering and categorization
                • Re-alerting for important notifications you might miss
                • App-specific importance settings and customization
                • Local data storage with complete privacy protection
                • Battery-friendly background monitoring
                • Advanced notification management and organization
                
                Privacy & Security:
                All notification data is stored locally on your device and never transmitted to external servers or third parties. Your privacy is completely protected.
                
                Technical Details:
                • Built with modern Android architecture components
                • Uses Room database for efficient local storage
                • Implements Android notification listener best practices
                • Optimized for battery life and performance
                
                Support:
                If you encounter any issues or have suggestions, please check the app settings for diagnostic tools or contact support through the app store.
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Privacy Details") { _, _ ->
                showPrivacyDetails()
            }
            .setNegativeButton("Rate App") { _, _ ->
                openAppStore()
            }
            .show()
    }

    private fun showNotificationPermissionGuidance() {
        AlertDialog.Builder(requireContext())
            .setTitle("Enable Notification Access")
            .setIcon(R.drawable.ic_notification)
            .setMessage("""
                To enable NotificationHub, please follow these steps:
                
                1️⃣ In the next screen, look for "NotificationHub" in the list
                
                2️⃣ Tap on "NotificationHub" to select it
                
                3️⃣ Enable the toggle switch next to NotificationHub
                
                4️⃣ Confirm by tapping "Allow" when prompted
                
                5️⃣ Return to NotificationHub to complete setup
                
                This allows NotificationHub to read your notifications and provide smart filtering.
            """.trimIndent())
            .setPositiveButton("Open Settings") { _, _ ->
                PermissionHelper.openNotificationListenerSettingsWithGuidance(requireContext()) {
                    // Schedule a check after user returns
                    schedulePermissionRecheck()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun schedulePermissionRecheck() {
        // Check permission status after a short delay when user returns
        binding.root.postDelayed({
            updatePermissionStatus()
        }, 1000)
    }

    private fun showPrivacyDetails() {
        try {
            val intent = Intent(requireContext(), PrivacyPolicyActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to simple dialog if WebView activity fails
            showSimplePrivacyDialog()
        }
    }

    private fun showSimplePrivacyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Privacy Information")
            .setMessage("""
                NotificationHub Privacy Commitment:
                
                • All data stored locally on your device
                • No cloud sync or external data transmission
                • Complete control over data retention
                • Industry-standard local encryption
                • No analytics, tracking, or user profiling
                • No advertising or monetization of user data
                
                For complete privacy details, please check the full privacy policy in the app resources.
            """.trimIndent())
            .setPositiveButton("Understood") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}"))
                startActivity(intent)
            } catch (e2: Exception) {
                showToast("Unable to open app store")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateNotificationManagementStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}