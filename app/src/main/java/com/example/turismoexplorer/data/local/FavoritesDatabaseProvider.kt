package com.example.turismoexplorer.data.local

import android.content.Context

object FavoritesDatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: AppDatabase.build(context).also { instance = it }
        }
}
