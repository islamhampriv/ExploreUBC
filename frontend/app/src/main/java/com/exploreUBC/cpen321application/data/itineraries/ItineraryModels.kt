package com.exploreUBC.cpen321application.data.itineraries

data class Itinerary(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String?,
    val startingLocation: String?,
    val items: List<ItineraryItem>,
    val createdAt: String,
    val updatedAt: String,
    val saved: Boolean,
    val inRecommendation: Boolean,
    val score: Map<String, Double>,
)

sealed interface ItineraryItem {
    val id: String
    val startTime: String
    val endTime: String
}

data class ActivityItem(
    override val id: String,
    val name: String,
    val image: String,
    val description: String?,
    val category: String?,
    val location: String,
    override val startTime: String,
    override val endTime: String,
) : ItineraryItem

data class CreateActivityInput(
    val name: String,
    val image: String,
    val description: String? = null,
    val category: String? = null,
    val location: String,
    val startTime: String,
    val endTime: String,
)

typealias ActivityUpdate = CreateActivityInput

data class TransportationItem(
    override val id: String,
    val mode: String,
    val startLocation: String,
    val endLocation: String,
    val distanceMeters: Double,
    override val startTime: String,
    override val endTime: String,
) : ItineraryItem

data class CreateTransportationInput(
    val mode: String,
    val startLocation: String,
    val endLocation: String,
    val distanceMeters: Double,
    val startTime: String,
    val endTime: String,
)

typealias TransportationSegmentUpdate = CreateTransportationInput

data class ItineraryDetails(
    val name: String,
    val description: String? = null,
    val startingLocation: String? = null,
)
