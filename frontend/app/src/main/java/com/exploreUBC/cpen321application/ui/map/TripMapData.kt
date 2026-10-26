package com.exploreUBC.cpen321application.ui.map

import com.example.cpen321application.maps.GoogleMapsApiClient
import com.example.cpen321application.maps.ItineraryStop
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.google.android.gms.maps.model.LatLng

data class MappedTrip(
    val name: String,
    val stops: List<ItineraryStop>,
    val unmappedCount: Int,
)

fun geocodeSavedItineraries(
    itineraries: List<Itinerary>,
    mapsApi: GoogleMapsApiClient,
): List<MappedTrip> {
    val cache = mutableMapOf<String, LatLng?>()
    return itineraries.map { itinerary ->
        var unmapped = 0
        val stops = itinerary.items.filterIsInstance<ActivityItem>().mapNotNull { activity ->
            val address = activity.location.trim()
            if (address.isEmpty()) {
                unmapped += 1
                return@mapNotNull null
            }
            val location = cache.getOrPut(address.lowercase()) { mapsApi.geocode(address) }
            if (location == null) {
                unmapped += 1
                return@mapNotNull null
            }
            ItineraryStop(
                name = activity.name,
                type = activity.category?.replace('_', ' ')?.ifBlank { "Activity" } ?: "Activity",
                startTime = activity.startTime,
                endTime = activity.endTime,
                address = address,
                location = location,
                itineraryName = itinerary.name,
            )
        }
        MappedTrip(
            name = itinerary.name,
            stops = stops,
            unmappedCount = unmapped,
        )
    }
}
