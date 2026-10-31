package com.example.cpen321application.maps

import com.google.android.gms.maps.model.LatLng

data class ItineraryStop(
    val name: String,
    val type: String,
    val startTime: String,
    val endTime: String,
    val address: String,
    val location: LatLng,
    val itineraryName: String = "",
)

data class PopularPlace(
    val name: String,
    val category: String,
    val address: String,
    val location: LatLng
)

data class AddressSuggestion(
    val primaryText: String,
    val fullAddress: String,
    val placeId: String,
)

data class AddressAutocompleteResult(
    val suggestions: List<AddressSuggestion>,
    val errorMessage: String? = null,
)
