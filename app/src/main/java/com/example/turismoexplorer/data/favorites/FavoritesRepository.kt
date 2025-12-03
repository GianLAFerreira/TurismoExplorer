package com.example.turismoexplorer.data.favorites

import android.content.Context
import com.example.turismoexplorer.data.local.FavoritePlaceEntity
import com.example.turismoexplorer.data.local.FavoritesDatabaseProvider
import com.example.turismoexplorer.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(context: Context) {
    private val dao = FavoritesDatabaseProvider.get(context).favoritesDao()

    fun favorites(): Flow<List<Place>> =
        dao.getAll().map { list ->
            list.map { e ->
                Place(
                    id = e.id,
                    name = e.name,
                    address = e.address,
                    rating = e.rating,
                    lat = e.lat,
                    lng = e.lng
                )
            }
        }

    fun favoriteIds(): Flow<Set<String>> =
        dao.getAllIds().map { it.toSet() }

    // Nova: busca por nome ou cidade (via address)
    fun searchFavorites(query: String): Flow<List<Place>> {
        val pattern = "%${query.trim()}%"
        return dao.search(pattern).map { list ->
            list.map { e ->
                Place(
                    id = e.id,
                    name = e.name,
                    address = e.address,
                    rating = e.rating,
                    lat = e.lat,
                    lng = e.lng
                )
            }
        }
    }

    suspend fun isFavorite(id: String) = dao.isFavorite(id)

    suspend fun add(place: Place) {
        val e = FavoritePlaceEntity(
            id = place.id,
            name = place.name,
            address = place.address,
            lat = place.lat,
            lng = place.lng,
            rating = place.rating
        )
        dao.insert(e)
    }

    suspend fun removeById(id: String) = dao.deleteById(id)

    suspend fun toggle(place: Place) {
        if (isFavorite(place.id)) removeById(place.id) else add(place)
    }
}