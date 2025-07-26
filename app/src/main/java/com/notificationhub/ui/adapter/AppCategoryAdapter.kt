// Fixed AppCategoryAdapter.kt - Correct icon handling
// Location: app/src/main/java/com/smartnotify/ui/adapter/AppCategoryAdapter.kt

package com.notificationhub.ui.adapter

import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notificationhub.data.entity.AppPreference
import com.notificationhub.databinding.ItemAppCategoryBinding

class AppCategoryAdapter(
    private val onImportanceChanged: (AppPreference, Boolean) -> Unit
) : ListAdapter<AppPreference, AppCategoryAdapter.AppViewHolder>(DiffCallback()) {

    companion object {
        private const val TAG = "AppCategoryAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(private val binding: ItemAppCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appPreference: AppPreference) {
            binding.apply {
                appNameText.text = appPreference.appName
                packageNameText.text = appPreference.packageName

                // Set app icon - FIXED VERSION
                try {
                    val packageManager = root.context.packageManager
                    val appIconDrawable = packageManager.getApplicationIcon(appPreference.packageName)
                    appIcon.setImageDrawable(appIconDrawable)  // FIXED: Use appIcon (ImageView) not appIcon (Drawable)
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Icon not found for ${appPreference.packageName}")
                    // Set default icon
                    appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting icon for ${appPreference.packageName}", e)
                    appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
                }

                // Set switch state without triggering listener
                importantSwitch.setOnCheckedChangeListener(null)
                importantSwitch.isChecked = appPreference.isImportant

                // Set the listener after setting the state
                importantSwitch.setOnCheckedChangeListener { _, isChecked ->
                    Log.d(TAG, "Switch changed for ${appPreference.appName}: $isChecked")
                    onImportanceChanged(appPreference, isChecked)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AppPreference>() {
        override fun areItemsTheSame(oldItem: AppPreference, newItem: AppPreference): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppPreference, newItem: AppPreference): Boolean {
            return oldItem == newItem
        }
    }
}