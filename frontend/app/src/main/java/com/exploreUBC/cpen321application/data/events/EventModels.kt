package com.exploreUBC.cpen321application.data.events

data class Event(
    val id: String,
    val title: String,
    val imageUrl: String,
    val startDateTime: String,
    val venueName: String,
    val city: String,
    val category: String,
    val ticketUrl: String,
    val priceMin: Double?,
    val priceMax: Double?,
    val currency: String?,
    val distanceKm: Double
)
