package com.notificationhub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "notifications")
@Parcelize
data class StoredNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,
    val packageName: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isImportant: Boolean = false,
    val iconData: ByteArray? = null
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StoredNotification

        if (id != other.id) return false
        if (appName != other.appName) return false
        if (packageName != other.packageName) return false
        if (title != other.title) return false
        if (body != other.body) return false
        if (timestamp != other.timestamp) return false
        if (isRead != other.isRead) return false
        if (isImportant != other.isImportant) return false
        if (iconData != null) {
            if (other.iconData == null) return false
            if (!iconData.contentEquals(other.iconData)) return false
        } else if (other.iconData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + appName.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isRead.hashCode()
        result = 31 * result + isImportant.hashCode()
        result = 31 * result + (iconData?.contentHashCode() ?: 0)
        return result
    }
}