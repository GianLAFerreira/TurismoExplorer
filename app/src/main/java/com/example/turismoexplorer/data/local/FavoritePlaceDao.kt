package com.example.turismoexplorer.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePlaceDao {
    @Query("SELECT * FROM favorite_places ORDER BY savedAt DESC")
    fun getAll(): Flow<List<FavoritePlaceEntity>>

    @Query("SELECT id FROM favorite_places")
    fun getAllIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_places WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoritePlaceEntity)

    @Delete
    suspend fun delete(entity: FavoritePlaceEntity)

    @Query("DELETE FROM favorite_places WHERE id = :id")
    suspend fun deleteById(id: String)

    // Busca por nome OU por endereço (cidade está contida no endereço)
    @Query("""
        SELECT * FROM favorite_places
        WHERE LOWER(name) LIKE LOWER(:pattern)
           OR LOWER(address) LIKE LOWER(:pattern)
        ORDER BY savedAt DESC
    """)
    fun search(pattern: String): Flow<List<FavoritePlaceEntity>>
}