package com.example.cpen321application.maps

import com.google.android.gms.maps.model.LatLng

object SampleMapData {
    val ubcCampus = LatLng(49.2606, -123.2460)

    val sampleItinerary: List<ItineraryStop> = listOf(
        ItineraryStop(
            name = "UBC Life Building / Nest",
            type = "Food",
            startTime = "9:00 AM",
            endTime = "10:00 AM",
            address = "6138 Student Union Blvd, Vancouver",
            location = LatLng(49.2665, -123.2494)
        ),
        ItineraryStop(
            name = "Irving K. Barber Learning Centre",
            type = "Study",
            startTime = "10:15 AM",
            endTime = "12:00 PM",
            address = "1961 East Mall, Vancouver",
            location = LatLng(49.2675, -123.2528)
        ),
        ItineraryStop(
            name = "Museum of Anthropology",
            type = "Culture",
            startTime = "12:30 PM",
            endTime = "2:00 PM",
            address = "6393 NW Marine Dr, Vancouver",
            location = LatLng(49.2694, -123.2595)
        ),
        ItineraryStop(
            name = "Nitobe Memorial Garden",
            type = "Outdoors",
            startTime = "2:15 PM",
            endTime = "3:00 PM",
            address = "1895 Lower Mall, Vancouver",
            location = LatLng(49.2663, -123.2598)
        ),
        ItineraryStop(
            name = "Wesbrook Village",
            type = "Shopping",
            startTime = "3:30 PM",
            endTime = "5:00 PM",
            address = "3338 Wesbrook Mall, Vancouver",
            location = LatLng(49.2543, -123.2367)
        )
    )

    val fallbackPopularPlaces: List<PopularPlace> = listOf(
        PopularPlace("UBC Rose Garden", "Park", "6103 Memorial Rd, Vancouver", LatLng(49.2697, -123.2566)),
        PopularPlace("UBC Bookstore", "Shopping", "6200 University Blvd, Vancouver", LatLng(49.2662, -123.2498)),
        PopularPlace("Thunderbird Stadium", "Sports", "6288 Stadium Rd, Vancouver", LatLng(49.2550, -123.2456)),
        PopularPlace("Pacific Spirit Regional Park", "Park", "16th Ave & Blanca St, Vancouver", LatLng(49.2560, -123.2290)),
        PopularPlace("Wreck Beach trail", "Outdoors", "NW Marine Dr, Vancouver", LatLng(49.2620, -123.2610)),
        PopularPlace("Kitsilano Beach", "Beach", "1499 Arbutus St, Vancouver", LatLng(49.2740, -123.1540)),
        PopularPlace("Granville Island", "Market", "1669 Johnston St, Vancouver", LatLng(49.2710, -123.1340)),
        PopularPlace("Stanley Park", "Park", "2099 Beach Ave, Vancouver", LatLng(49.3017, -123.1417))
    )
}
