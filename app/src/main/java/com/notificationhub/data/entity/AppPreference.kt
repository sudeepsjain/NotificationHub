package com.notificationhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_preferences")
data class AppPreference(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isImportant: Boolean = false
)