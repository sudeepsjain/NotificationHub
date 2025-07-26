// File: app/src/main/java/com/smartnotify/ui/viewmodel/NotificationLogViewModel.kt
package com.notificationhub.ui.viewmodel

import androidx.lifecycle.*
import com.notificationhub.SmartNotifyApplication
import com.notificationhub.data.entity.StoredNotification
import kotlinx.coroutines.launch

class NotificationLogViewModel : ViewModel() {
    private val repository = SmartNotifyApplication.instance.repository

    private val _filter = MutableLiveData(Filter.IMPORTANT) // Default to Important
    private val _searchQuery = MutableLiveData("")

    enum class Filter { IMPORTANT, OTHER, ALL }

    // Get unread important count for button state
    val unreadImportantCount: LiveData<Int> = repository.getUnreadImportantNotifications().map {
        it.size
    }

    val filteredNotifications: LiveData<List<StoredNotification>> =
        MediatorLiveData<List<StoredNotification>>().apply {
            val updateData = {
                val filter = _filter.value ?: Filter.IMPORTANT
                val query = _searchQuery.value ?: ""

                val source = when {
                    query.isNotEmpty() -> repository.searchNotifications(query)
                    filter == Filter.IMPORTANT -> repository.getImportantNotifications()
                    filter == Filter.ALL -> repository.getAllNotifications()
                    else -> repository.getAllNotifications() // OTHER case handled below
                }

                addSource(source) { notifications ->
                    value = when (filter) {
                        Filter.OTHER -> notifications.filter { !it.isImportant }
                        else -> notifications
                    }
                }
            }

            addSource(_filter) { updateData() }
            addSource(_searchQuery) { updateData() }
        }

    fun setFilter(filter: Filter) {
        _filter.value = filter
    }

    fun searchNotifications(query: String) {
        _searchQuery.value = query
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllImportantAsRead()
        }
    }
}