package com.example.turismoexplorer.data.places

data class PlaceResult(
    val place_id: String?,
    val name: String?,
    val formatted_address: String?,
    val rating: Double?,
    val user_ratings_total: Int?,
    val geometry: Geometry?,
    val photos: List<Photo>?
)