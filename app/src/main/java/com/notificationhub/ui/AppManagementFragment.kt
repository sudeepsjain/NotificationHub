// File: app/src/main/java/com/smartnotify/ui/AppManagementFragment.kt
package com.notificationhub.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.notificationhub.SmartNotifyApplication
import com.notificationhub.databinding.FragmentAppManagementBinding
import com.notificationhub.ui.adapter.AppManagementAdapter
import com.notificationhub.data.entity.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppManagementFragment : Fragment() {
    private var _binding: FragmentAppManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AppManagementAdapter
    private val repository by lazy { SmartNotifyApplication.instance.repository }

    private var allApps: List<AppInfo> = emptyList()
    private var appPreferences: Map<String, AppPreference> = emptyMap()
    private var currentSearchQuery = ""

    companion object {
        private const val TAG = "AppManagement"
    }

    data class AppInfo(
        val name: String,
        val packageName: String,
        val isSystemApp: Boolean,
        val icon: android.graphics.drawable.Drawable?,
        val isLaunchable: Boolean = true
    )

    data class EnhancedAppInfo(
        val appInfo: AppInfo,
        val isImportant: Boolean
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupControls()
        loadAppPreferences()
        loadAllApps()
    }

    private fun setupRecyclerView() {
        adapter = AppManagementAdapter { packageName, appName, isImportant ->
            updateAppImportance(packageName, appName, isImportant)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AppManagementFragment.adapter
        }
    }

    private fun setupControls() {
        // Search functionality
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query != currentSearchQuery) {
                    currentSearchQuery = query
                    updateDisplayedApps()
                }
            }
        })

        // Refresh button
        binding.refreshButton.setOnClickListener {
            refreshData()
        }
    }

    private fun loadAppPreferences() {
        repository.getAllAppPreferences().observe(viewLifecycleOwner) { preferences ->
            appPreferences = preferences.associateBy { it.packageName }
            Log.d(TAG, "Loaded ${appPreferences.size} app preferences")
            updateDisplayedApps()
        }
    }

    private fun loadAllApps() {
        Log.d(TAG, "Loading all apps...")
        showLoadingState()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apps = withContext(Dispatchers.IO) {
                    loadAppsFromSystem()
                }

                if (isAdded) {
                    allApps = apps
                    Log.d(TAG, "Loaded ${allApps.size} total apps")
                    hideLoadingState()
                    updateDisplayedApps()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps", e)
                if (isAdded) {
                    hideLoadingState()
                    showErrorState("Error loading apps: ${e.message}")
                }
            }
        }
    }

    private fun loadAppsFromSystem(): List<AppInfo> {
        val packageManager = requireContext().packageManager

        val allInstalledApps = try {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps with metadata", e)
            try {
                packageManager.getInstalledApplications(0)
            } catch (e2: Exception) {
                Log.e(TAG, "Error getting installed apps", e2)
                emptyList()
            }
        }

        Log.d(TAG, "Found ${allInstalledApps.size} total applications")

        val tempApps = mutableListOf<AppInfo>()

        allInstalledApps.forEach { app ->
            try {
                // Skip our own app
                if (app.packageName == requireContext().packageName) {
                    return@forEach
                }

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appName = try {
                    packageManager.getApplicationLabel(app).toString()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get app name for ${app.packageName}")
                    app.packageName
                }

                val icon = try {
                    packageManager.getApplicationIcon(app)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get icon for ${app.packageName}")
                    null
                }

                val hasLauncherIntent = try {
                    packageManager.getLaunchIntentForPackage(app.packageName) != null
                } catch (e: Exception) {
                    false
                }

                // Include all apps but prioritize launchable ones
                val appInfo = AppInfo(
                    name = appName,
                    packageName = app.packageName,
                    isSystemApp = isSystem,
                    icon = icon,
                    isLaunchable = hasLauncherIntent
                )

                // Add all apps - both user and system
                tempApps.add(appInfo)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing app ${app.packageName}", e)
            }
        }

        // Sort: User apps first, then system apps, alphabetical within each group
        val sortedApps = tempApps.sortedWith(compareBy<AppInfo> { it.isSystemApp }.thenBy { it.name.lowercase() })

        Log.d(TAG, "Processed ${sortedApps.size} apps successfully")
        return sortedApps
    }

    private fun updateDisplayedApps() {
        val appsToShow = mutableListOf<EnhancedAppInfo>()

        // Convert all apps to enhanced app info with importance status
        allApps.forEach { app ->
            val preference = appPreferences[app.packageName]
            appsToShow.add(EnhancedAppInfo(app, preference?.isImportant ?: false))
        }

        val filteredList = if (currentSearchQuery.isEmpty()) {
            appsToShow
        } else {
            appsToShow.filter { enhancedApp ->
                enhancedApp.appInfo.name.contains(currentSearchQuery, ignoreCase = true) ||
                        enhancedApp.appInfo.packageName.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        adapter.submitList(filteredList)

        // Update count display
        val totalCount = allApps.size
        val userCount = allApps.count { !it.isSystemApp }
        val systemCount = allApps.count { it.isSystemApp }
        val importantCount = appPreferences.values.count { it.isImportant }

        binding.appCountText.text = if (currentSearchQuery.isEmpty()) {
            "$totalCount apps ($userCount user, $systemCount system) • $importantCount important"
        } else {
            "${filteredList.size} of $totalCount apps • $importantCount important"
        }

        // Show/hide empty state
        if (filteredList.isEmpty() && totalCount > 0) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun updateAppImportance(packageName: String, appName: String, isImportant: Boolean) {
        Log.d(TAG, "Toggled $appName importance: $isImportant")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val existing = repository.getAppPreference(packageName)
                if (existing != null) {
                    repository.updateAppPreference(existing.copy(isImportant = isImportant))
                } else {
                    repository.insertAppPreference(AppPreference(packageName, appName, isImportant))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating app preference", e)
            }
        }
    }

    private fun refreshData() {
        // Add refresh animation
        binding.refreshButton.animate()
            .rotationBy(360f)
            .setDuration(500)
            .start()

        loadAppPreferences()
        loadAllApps()
    }

    // UI State Management
    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.noResultsText.visibility = View.GONE
    }

    private fun hideLoadingState() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        binding.noResultsText.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE

        val message = if (currentSearchQuery.isNotEmpty()) {
            "No apps found for \"$currentSearchQuery\""
        } else {
            "No apps found"
        }
        binding.noResultsText.text = message
    }

    private fun hideEmptyState() {
        binding.noResultsText.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }

    private fun showErrorState(message: String) {
        binding.appCountText.text = message
        binding.noResultsText.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.noResultsText.text = "Error loading apps"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}