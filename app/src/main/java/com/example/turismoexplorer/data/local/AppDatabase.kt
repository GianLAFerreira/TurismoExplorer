package com.example.turismoexplorer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoritePlaceEntity::class, NotificationEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritePlaceDao
    abstract fun notificationsDao(): NotificationDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "turismo_explorer.db"
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}