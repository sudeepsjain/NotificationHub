// File: app/src/main/java/com/smartnotify/ui/HomeFragment.kt
package com.notificationhub.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.notificationhub.R
import com.notificationhub.SmartNotifyApplication
import com.notificationhub.databinding.FragmentHomeBinding
import com.notificationhub.ui.adapter.HomeNotificationAdapter
import com.notificationhub.ui.viewmodel.HomeViewModel
import com.notificationhub.utils.PermissionHelper
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var recentAdapter: HomeNotificationAdapter
    private val repository by lazy { SmartNotifyApplication.instance.repository }

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupWelcomeMessage()
        setupRecentNotifications()
        setupClickListeners()
        setupObservers()
        updateAppStatus()
    }

    private fun setupWelcomeMessage() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
        binding.welcomeTimeText.text = greeting
    }

    private fun setupRecentNotifications() {
        recentAdapter = HomeNotificationAdapter { notification ->
            // Open the app when notification is clicked
            openNotificationApp(notification.packageName, notification.appName)
            // Mark as read
            viewModel.markAsRead(notification.id)
        }

        binding.recentNotificationsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.permissionButton.setOnClickListener {
            showNotificationPermissionGuidance()
        }

        binding.markAllReadButton.setOnClickListener {
            viewModel.markAllAsRead()
        }

        binding.manageAppsButton.setOnClickListener {
            // Navigate to app management
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AppManagementFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.viewAllButton.setOnClickListener {
            // Navigate to notification log
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NotificationLogFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupObservers() {
        // Observe unread important notifications
        viewModel.unreadCount.observe(viewLifecycleOwner) { notifications ->
            val count = notifications.size
            binding.unreadCountNumber.text = count.toString()

            // Update mark all read button
            binding.markAllReadButton.isEnabled = count > 0
            binding.markAllReadButton.text = if (count > 0) {
                "Mark All Read ($count)"
            } else {
                "Mark All Read"
            }
        }

        // Observe recent important notifications (limit to 3)
        viewModel.recentNotifications.observe(viewLifecycleOwner) { notifications ->
            val recentNotifications = notifications.take(3)

            if (recentNotifications.isNotEmpty()) {
                binding.recentNotificationsRecycler.visibility = View.VISIBLE
                binding.emptyRecentLayout.visibility = View.GONE
                recentAdapter.submitList(recentNotifications)
            } else {
                binding.recentNotificationsRecycler.visibility = View.GONE
                binding.emptyRecentLayout.visibility = View.VISIBLE
            }
        }

        // Observe important apps count
        repository.getAllAppPreferences().observe(viewLifecycleOwner) { preferences ->
            val importantCount = preferences.count { it.isImportant }
            binding.importantAppsCount.text = importantCount.toString()
        }

        // Observe latest notification for status
        repository.getAllNotifications().observe(viewLifecycleOwner) { notifications ->
            updateLastNotificationTime(notifications.firstOrNull()?.timestamp)
        }
    }

    private fun updateAppStatus() {
        val permissionStatus = PermissionHelper.checkAllPermissions(requireContext())

        if (permissionStatus.allPermissionsGranted) {
            // Hide permission card
            binding.permissionStatusCard.visibility = View.GONE

            // Update status indicator
            binding.statusIndicator.setBackgroundColor(requireContext().getColor(R.color.important_color))
            binding.statusText.text = "Active"
            binding.statusText.setTextColor(requireContext().getColor(R.color.important_color))

            // Update monitoring status
            binding.monitoringIndicator.setBackgroundColor(requireContext().getColor(R.color.important_color))
            binding.monitoringStatusText.text = "Monitoring notifications"
            binding.monitoringStatusText.setTextColor(requireContext().getColor(R.color.secondary_text))

        } else {
            // Show permission card
            binding.permissionStatusCard.visibility = View.VISIBLE

            // Update status indicator
            binding.statusIndicator.setBackgroundColor(requireContext().getColor(R.color.orange_light))
            binding.statusText.text = "Setup needed"
            binding.statusText.setTextColor(requireContext().getColor(R.color.orange_light))

            // Update monitoring status
            binding.monitoringIndicator.setBackgroundColor(0xFFFF5722.toInt())
            binding.monitoringStatusText.text = "Waiting for permissions"
            binding.monitoringStatusText.setTextColor(0xFFFF5722.toInt())
        }
    }

    private fun updateLastNotificationTime(timestamp: Long?) {
        if (timestamp != null) {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val timeText = when {
                diff < 60_000 -> "Last notification: just now"
                diff < 3600_000 -> "Last notification: ${diff / 60_000}m ago"
                diff < 86400_000 -> "Last notification: ${diff / 3600_000}h ago"
                diff < 604800_000 -> "Last notification: ${diff / 86400_000}d ago"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    "Last notification: ${dateFormat.format(Date(timestamp))}"
                }
            }

            binding.lastNotificationText.text = timeText
            binding.lastNotificationText.setTextColor(requireContext().getColor(R.color.secondary_text))
        } else {
            binding.lastNotificationText.text = "No notifications received yet"
            binding.lastNotificationText.setTextColor(requireContext().getColor(R.color.tertiary_text))
        }
    }

    private fun showNotificationPermissionGuidance() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
            updateAppStatus()
        }, 1000)
    }

    private fun openNotificationApp(packageName: String, appName: String) {
        try {
            val packageManager = requireContext().packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(launchIntent)
                Log.d(TAG, "Opened app: $appName")
            } else {
                Log.w(TAG, "No launch intent found for $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app", e)
        }
    }

    override fun onResume() {
        super.onResume()
        updateAppStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}