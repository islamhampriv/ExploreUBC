package com.exploreUBC.cpen321application.data.itineraries

import java.io.IOException

data class MapCoordinates(
    val latitude: Double,
    val longitude: Double,
)

data class MapMarker(
    val activityId: String,
    val title: String,
    val address: String,
    val placeId: String,
    val coordinates: MapCoordinates,
    val isApproximate: Boolean,
)

data class MapRoute(
    val fromActivityId: String,
    val toActivityId: String,
    val transportationMode: String,
    val encodedPolyline: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
)

data class UnmappedActivity(
    val activityId: String,
    val title: String,
    val address: String,
)

data class ItineraryMap(
    val itineraryId: String,
    val ownerId: String,
    val itineraryUpdatedAt: String,
    val markers: List<MapMarker>,
    val routes: List<MapRoute>,
    val unmappedActivities: List<UnmappedActivity>,
    val generatedAt: String,
)

class ItineraryHttpException(
    val statusCode: Int,
    message: String,
) : IOException(message)
