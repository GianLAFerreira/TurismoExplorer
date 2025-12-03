package com.example.turismoexplorer.data.places

data class PlacesTextSearchResponse(
    val results: List<PlaceResult>?,
    val status: String?,
    val error_message: String?
)