package com.example.turismoexplorer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inbox_notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val lat: Double?,
    val lng: Double?,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)