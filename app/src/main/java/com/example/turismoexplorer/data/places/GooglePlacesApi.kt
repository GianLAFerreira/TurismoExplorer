package com.example.turismoexplorer.data.places

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesApi {
    @GET("place/textsearch/json")
    suspend fun textSearch(
        @Query("query") query: String,
        @Query("key") apiKey: String,
        @Query("language") language: String = "pt-BR",
        @Query("region") region: String = "br",
        @Query("type") type: String = "tourist_attraction"
    ): Response<PlacesTextSearchResponse>
}

