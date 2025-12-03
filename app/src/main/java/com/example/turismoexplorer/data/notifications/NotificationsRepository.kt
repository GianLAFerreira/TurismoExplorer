package com.example.turismoexplorer.data.notifications

import android.content.Context
import com.example.turismoexplorer.data.local.FavoritesDatabaseProvider
import com.example.turismoexplorer.data.local.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationsRepository(context: Context) {
    private val dao = FavoritesDatabaseProvider.get(context).notificationsDao()

    fun all(): Flow<List<NotificationEntity>> = dao.getAll()

    suspend fun add(title: String, message: String, lat: Double?, lng: Double?) {
        dao.insert(
            NotificationEntity(
                title = title,
                message = message,
                lat = lat,
                lng = lng
            )
        )
    }

    suspend fun clearAll() = dao.clearAll()
}