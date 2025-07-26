// File: app/src/main/java/com/smartnotify/ui/viewmodel/HomeViewModel.kt
package com.notificationhub.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notificationhub.SmartNotifyApplication
import com.notificationhub.data.entity.StoredNotification
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repository = SmartNotifyApplication.instance.repository

    // Get unread important notifications
    val unreadCount: LiveData<List<StoredNotification>> = repository.getUnreadImportantNotifications()

    // Get recent important notifications (for display in home)
    val recentNotifications: LiveData<List<StoredNotification>> = repository.getImportantNotifications()

    // Get important apps count using MediatorLiveData
    val importantAppsCount: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(repository.getAllAppPreferences()) { preferences ->
            value = preferences.count { appPreference -> appPreference.isImportant }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllImportantAsRead()
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }
}