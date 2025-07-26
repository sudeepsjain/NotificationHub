// File: app/src/main/java/com/smartnotify/ui/adapter/HomeNotificationAdapter.kt
package com.notificationhub.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notificationhub.data.entity.StoredNotification
import com.notificationhub.databinding.ItemHomeNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeNotificationAdapter(
    private val onNotificationClick: (StoredNotification) -> Unit
) : ListAdapter<StoredNotification, HomeNotificationAdapter.NotificationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemHomeNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemHomeNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: StoredNotification) {
            binding.apply {
                // App name and content
                appNameText.text = notification.appName
                titleText.text = notification.title.ifEmpty { "No title" }
                bodyText.text = notification.body.ifEmpty { "No content" }
                timeText.text = formatTimestamp(notification.timestamp)

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

                // Unread indicator
                unreadIndicator.visibility = if (!notification.isRead) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Click handler
                root.setOnClickListener {
                    onNotificationClick(notification)
                }
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "now"
                diff < 3600_000 -> "${diff / 60_000}m"
                diff < 86400_000 -> "${diff / 3600_000}h"
                diff < 604800_000 -> "${diff / 86400_000}d"
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