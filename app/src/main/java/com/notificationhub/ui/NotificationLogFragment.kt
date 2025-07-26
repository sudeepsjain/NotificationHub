// File: app/src/main/java/com/smartnotify/ui/NotificationLogFragment.kt
package com.notificationhub.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.notificationhub.databinding.FragmentNotificationLogBinding
import com.notificationhub.ui.adapter.NotificationAdapter
import com.notificationhub.ui.viewmodel.NotificationLogViewModel
import com.notificationhub.data.entity.StoredNotification

class NotificationLogFragment : Fragment() {
    private var _binding: FragmentNotificationLogBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NotificationLogViewModel
    private lateinit var adapter: NotificationAdapter
    private var currentSearchQuery = ""

    companion object {
        private const val TAG = "NotificationLog"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[NotificationLogViewModel::class.java]

        setupRecyclerView()
        setupTabs()
        setupSearch()
        setupControls()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onMarkAsRead = { notification ->
                viewModel.markAsRead(notification.id)
            },
            onNotificationClick = { notification ->
                openNotificationApp(notification)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@NotificationLogFragment.adapter
        }
    }

    private fun setupTabs() {
        // Clear existing tabs and add in correct order
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Important"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Other"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))

        // Set default to Important tab
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        viewModel.setFilter(NotificationLogViewModel.Filter.IMPORTANT)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> viewModel.setFilter(NotificationLogViewModel.Filter.IMPORTANT)
                    1 -> viewModel.setFilter(NotificationLogViewModel.Filter.OTHER)
                    2 -> viewModel.setFilter(NotificationLogViewModel.Filter.ALL)
                }
                updateEmptyMessage()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query != currentSearchQuery) {
                    currentSearchQuery = query
                    viewModel.searchNotifications(query)
                }
            }
        })
    }

    private fun setupControls() {
        binding.markAllReadButton.setOnClickListener {
            viewModel.markAllAsRead()
        }
    }

    private fun setupObservers() {
        // Observe filtered notifications
        viewModel.filteredNotifications.observe(viewLifecycleOwner) { notifications ->
            Log.d(TAG, "Received ${notifications.size} notifications for display")

            // Remove duplicates by grouping by package, title, and timestamp (within 1 second)
            val deduplicatedNotifications = removeDuplicates(notifications)
            Log.d(TAG, "After deduplication: ${deduplicatedNotifications.size} notifications")

            adapter.submitList(deduplicatedNotifications)
            updateNotificationCount(deduplicatedNotifications)

            if (deduplicatedNotifications.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }

        // Observe unread count for button state
        viewModel.unreadImportantCount.observe(viewLifecycleOwner) { count ->
            binding.markAllReadButton.isEnabled = count > 0
            binding.markAllReadButton.text = if (count > 0) {
                "Mark All Read ($count)"
            } else {
                "Mark All Read"
            }
        }
    }

    private fun removeDuplicates(notifications: List<StoredNotification>): List<StoredNotification> {
        // Group by package name, title, and body, then take the latest within each group
        return notifications
            .groupBy { "${it.packageName}|${it.title}|${it.body}" }
            .mapValues { (_, group) ->
                // For each group, if notifications are within 5 seconds of each other, take the latest
                group.sortedByDescending { it.timestamp }
                    .fold(mutableListOf<StoredNotification>()) { acc, notification ->
                        if (acc.isEmpty() ||
                            acc.last().timestamp - notification.timestamp > 5000) {
                            acc.add(notification)
                        }
                        acc
                    }
            }
            .values
            .flatten()
            .sortedByDescending { it.timestamp }
    }

    private fun updateNotificationCount(notifications: List<StoredNotification>) {
        val totalCount = notifications.size
        val unreadCount = notifications.count { !it.isRead }
        val importantCount = notifications.count { it.isImportant }

        val currentTab = binding.tabLayout.selectedTabPosition
        val tabName = when (currentTab) {
            0 -> "important"
            1 -> "other"
            2 -> "all"
            else -> "all"
        }

        binding.notificationCountText.text = if (currentSearchQuery.isEmpty()) {
            "$totalCount $tabName notifications • $unreadCount unread"
        } else {
            "$totalCount of all notifications • $unreadCount unread"
        }
    }

    private fun updateEmptyMessage() {
        val currentTab = binding.tabLayout.selectedTabPosition
        val message = when (currentTab) {
            0 -> "No important notifications yet"
            1 -> "No other notifications"
            2 -> "No notifications received"
            else -> "No notifications found"
        }

        binding.emptyMessageText.text = if (currentSearchQuery.isNotEmpty()) {
            "No notifications found for \"$currentSearchQuery\""
        } else {
            message
        }
    }

    private fun openNotificationApp(notification: StoredNotification) {
        try {
            val packageManager = requireContext().packageManager

            // Try to get the launch intent for the app
            val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)

            if (launchIntent != null) {
                // Add flags to bring app to front or start new task
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // For some apps, we can try to be more specific
                when (notification.packageName) {
                    "com.google.android.gm" -> {
                        // Gmail - try to open to inbox
                        launchIntent.action = Intent.ACTION_MAIN
                        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    "com.whatsapp" -> {
                        // WhatsApp - open to main screen
                        launchIntent.action = Intent.ACTION_MAIN
                        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    "com.android.mms", "com.google.android.apps.messaging" -> {
                        // SMS apps - open to main screen
                        launchIntent.action = Intent.ACTION_MAIN
                        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                }

                startActivity(launchIntent)
                Log.d(TAG, "Opened app: ${notification.appName}")

                // Mark notification as read when opened
                viewModel.markAsRead(notification.id)

            } else {
                Log.w(TAG, "No launch intent found for ${notification.packageName}")
                // Could show a toast here
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error opening app for notification", e)
            // Could show a toast with error message
        }
    }

    private fun showEmptyState() {
        binding.emptyView.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        updateEmptyMessage()
    }

    private fun hideEmptyState() {
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}