package com.example.cpen321application.maps

import com.google.android.gms.maps.model.LatLng

data class ItineraryStop(
    val name: String,
    val type: String,
    val startTime: String,
    val endTime: String,
    val address: String,
    val location: LatLng
)

data class PopularPlace(
    val name: String,
    val category: String,
    val address: String,
    val location: LatLng
)
