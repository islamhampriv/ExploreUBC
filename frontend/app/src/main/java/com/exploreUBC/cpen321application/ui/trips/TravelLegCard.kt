package com.exploreUBC.cpen321application.ui.trips

import kotlin.math.roundToInt

fun formatTravelDuration(seconds: Double): String {
    val totalMinutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours} hr ${minutes} min"
    } else {
        "$totalMinutes min"
    }
}

fun formatTravelDistance(meters: Double): String {
    return if (meters >= 1000) {
        val tenths = (meters / 100.0).roundToInt()
        "${tenths / 10.0} km"
    } else {
        "${meters.roundToInt()} m"
    }
}
