package com.example.turismoexplorer.data.places

import android.util.Log
import com.example.turismoexplorer.domain.Place

class PlacesRepository(
    private val api: GooglePlacesApi,
    private val apiKey: String
) {
    suspend fun popularByCity(city: String, limit: Int = 20): List<Place> {
        val query = "tourist attractions in $city, Brazil"
        val response = api.textSearch(query = query, apiKey = apiKey)

        if (!response.isSuccessful) {
            Log.e("PlacesRepository", "HTTP ${response.code()} para city=$city")
            throw IllegalStateException("HTTP ${response.code()} ao consultar Places")
        }

        val body = response.body() ?: run {
            Log.e("PlacesRepository", "Resposta vazia para city=$city")
            throw IllegalStateException("Resposta vazia do Places")
        }
        val status = body.status ?: "UNKNOWN"
        val errorMessage = body.error_message
        val count = body.results?.size ?: 0

        Log.d("PlacesRepository", "status=$status, error=$errorMessage, results=$count, city=$city")

        if (status != "OK") {
            val msg = when (status) {
                "REQUEST_DENIED" -> "Acesso negado: ${errorMessage ?: "verifique restrições e billing"}"
                "ZERO_RESULTS" -> "Nenhum resultado para “$city”."
                "OVER_QUERY_LIMIT" -> "Cota excedida."
                "INVALID_REQUEST" -> "Requisição inválida."
                else -> "Erro do Places ($status): ${errorMessage ?: "sem detalhes"}"
            }
            Log.e("PlacesRepository", "Falha: $msg")
            throw IllegalStateException(msg)
        }

        val list = body.results.orEmpty().mapNotNull { r ->
            val id = r.place_id ?: return@mapNotNull null
            Place(
                id = id,
                name = r.name.orEmpty(),
                address = r.formatted_address.orEmpty(),
                rating = r.rating,
                lat = r.geometry?.location?.lat,
                lng = r.geometry?.location?.lng
            )
        }
        Log.d("PlacesRepository", "Mapeados ${list.size} lugares")
        return list.take(limit)
    }
}