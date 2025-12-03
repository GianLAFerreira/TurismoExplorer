package com.example.turismoexplorer.domain

data class Place(
    val id: String,
    val name: String,
    val address: String,
    val rating: Double?,
    val lat: Double?,
    val lng: Double?
)