// File: app/src/main/java/com/smartnotify/ui/adapter/AppManagementAdapter.kt
package com.notificationhub.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notificationhub.databinding.ItemAppManagementBinding
import com.notificationhub.ui.AppManagementFragment

class AppManagementAdapter(
    private val onImportanceChanged: (String, String, Boolean) -> Unit
) : ListAdapter<AppManagementFragment.EnhancedAppInfo, AppManagementAdapter.AppViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppManagementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(private val binding: ItemAppManagementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(enhancedAppInfo: AppManagementFragment.EnhancedAppInfo) {
            val appInfo = enhancedAppInfo.appInfo

            binding.apply {
                // App name (corresponds to android:id="@+id/app_name_text")
                appNameText.text = appInfo.name

                // Package name (corresponds to android:id="@+id/package_name_text")
                packageNameText.text = appInfo.packageName

                // System badge (corresponds to android:id="@+id/system_badge")
                systemBadge.visibility = if (appInfo.isSystemApp) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // App icon (corresponds to android:id="@+id/app_icon")
                if (appInfo.icon != null) {
                    appIcon.setImageDrawable(appInfo.icon)
                } else {
                    appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                }

                // Importance switch (corresponds to android:id="@+id/important_switch")
                importantSwitch.setOnCheckedChangeListener(null)
                importantSwitch.isChecked = enhancedAppInfo.isImportant
                importantSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onImportanceChanged(appInfo.packageName, appInfo.name, isChecked)
                }

                // Visual feedback for non-launchable apps
                root.alpha = if (appInfo.isLaunchable) 1.0f else 0.6f

                // Make card clickable
                root.setOnClickListener {
                    importantSwitch.isChecked = !importantSwitch.isChecked
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AppManagementFragment.EnhancedAppInfo>() {
        override fun areItemsTheSame(
            oldItem: AppManagementFragment.EnhancedAppInfo,
            newItem: AppManagementFragment.EnhancedAppInfo
        ): Boolean {
            return oldItem.appInfo.packageName == newItem.appInfo.packageName
        }

        override fun areContentsTheSame(
            oldItem: AppManagementFragment.EnhancedAppInfo,
            newItem: AppManagementFragment.EnhancedAppInfo
        ): Boolean {
            return oldItem == newItem
        }
    }
}