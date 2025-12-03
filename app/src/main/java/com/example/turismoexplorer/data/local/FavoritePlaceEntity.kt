package com.example.turismoexplorer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_places")
data class FavoritePlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val lat: Double?,
    val lng: Double?,
    val rating: Double?,
    val savedAt: Long = System.currentTimeMillis()
)