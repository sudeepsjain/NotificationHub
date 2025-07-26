package com.notificationhub.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.*
import com.notificationhub.SmartNotifyApplication
import com.notificationhub.data.entity.AppPreference
import kotlinx.coroutines.launch

class AppCategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmartNotifyApplication.instance.repository
    private val packageManager = application.packageManager

    private val _installedApps = MutableLiveData<List<ApplicationInfo>>()

    companion object {
        private const val TAG = "AppCategoryViewModel"
    }

    val appPreferences: LiveData<List<AppPreference>> =
        MediatorLiveData<List<AppPreference>>().apply {
            addSource(_installedApps) { apps ->
                Log.d(TAG, "Installed apps updated: ${apps.size} apps")
                addSource(repository.getAllAppPreferences()) { preferences ->
                    Log.d(TAG, "Preferences updated: ${preferences.size} preferences")
                    val appMap = preferences.associateBy { it.packageName }
                    val result = apps.map { app ->
                        val packageName = app.packageName
                        val appName = try {
                            packageManager.getApplicationLabel(app).toString()
                        } catch (e: Exception) {
                            packageName
                        }
                        appMap[packageName] ?: AppPreference(packageName, appName, false)
                    }
                    Log.d(TAG, "Mapped result: ${result.size} app preferences")
                    value = result
                }
            }
        }

    fun setInstalledApps(apps: List<ApplicationInfo>) {
        Log.d(TAG, "Setting installed apps: ${apps.size}")
        _installedApps.value = apps
    }

    fun updateAppImportance(packageName: String, isImportant: Boolean) {
        Log.d(TAG, "Updating importance for $packageName: $isImportant")
        viewModelScope.launch {
            val existing = repository.getAppPreference(packageName)
            if (existing != null) {
                repository.updateAppPreference(existing.copy(isImportant = isImportant))
                Log.d(TAG, "Updated existing preference for $packageName")
            } else {
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }
                repository.insertAppPreference(AppPreference(packageName, appName, isImportant))
                Log.d(TAG, "Created new preference for $packageName")
            }
        }
    }
}