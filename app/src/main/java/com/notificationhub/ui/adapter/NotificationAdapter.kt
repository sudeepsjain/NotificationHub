// File: app/src/main/java/com/smartnotify/ui/adapter/NotificationAdapter.kt
package com.notificationhub.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notificationhub.data.entity.StoredNotification
import com.notificationhub.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onMarkAsRead: (StoredNotification) -> Unit,
    private val onNotificationClick: (StoredNotification) -> Unit
) : ListAdapter<StoredNotification, NotificationAdapter.NotificationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: StoredNotification) {
            binding.apply {
                // App name and timestamp
                appNameText.text = notification.appName
                timestampText.text = formatTimestamp(notification.timestamp)

                // Notification content
                titleText.text = notification.title.ifEmpty { "No title" }
                bodyText.text = notification.body.ifEmpty { "No content" }

                // App icon
                if (notification.iconData != null) {
                    try {
                        val bitmap = BitmapFactory.decodeByteArray(notification.iconData, 0, notification.iconData.size)
                        appIcon.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                    }
                } else {
                    appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                }

                // Important indicator
                importanceIndicator.visibility = if (notification.isImportant) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Read status
                updateReadStatus(notification.isRead)

                // Visual feedback for read/unread
                root.alpha = if (notification.isRead) 0.7f else 1.0f

                // Click handlers
                root.setOnClickListener {
                    onNotificationClick(notification)
                }

                markReadButton.setOnClickListener {
                    onMarkAsRead(notification)
                }

                // Hide mark read button if already read
                markReadButton.visibility = if (notification.isRead) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
            }
        }

        private fun updateReadStatus(isRead: Boolean) {
            binding.apply {
                if (isRead) {
                    readStatusIndicator.setBackgroundColor(0xFF777777.toInt())
                    readStatusText.text = "Read"
                    readStatusText.setTextColor(0xFF777777.toInt())
                } else {
                    readStatusIndicator.setBackgroundColor(0xFF03DAC5.toInt())
                    readStatusText.text = "Unread"
                    readStatusText.setTextColor(0xFF03DAC5.toInt())
                }
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 604800_000 -> "${diff / 86400_000}d ago"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StoredNotification>() {
        override fun areItemsTheSame(oldItem: StoredNotification, newItem: StoredNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StoredNotification, newItem: StoredNotification): Boolean {
            return oldItem == newItem
        }
    }
}