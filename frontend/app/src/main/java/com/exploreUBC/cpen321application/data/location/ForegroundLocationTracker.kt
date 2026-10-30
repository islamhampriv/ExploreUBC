package com.exploreUBC.cpen321application.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

private const val LOCATION_UPDATE_INTERVAL_MS = 60_000L
private const val MINIMUM_LOCATION_CHANGE_METERS = 10_000F

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        val UBC = UserLocation(49.2606, -123.2460)
    }
}

/** Reports foreground locations after the user moves at least 10 kilometres. */
class ForegroundLocationTracker(
    context: Context,
    private val onLocationChanged: (UserLocation) -> Unit,
) {
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastReportedLocation: Location? = null
    private var isTracking = false

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        LOCATION_UPDATE_INTERVAL_MS,
    )
        .setMinUpdateDistanceMeters(MINIMUM_LOCATION_CHANGE_METERS)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::reportLocationIfFarEnough)
        }
    }

    /** Starts foreground location updates after permission has been granted. */
    @SuppressLint("MissingPermission")
    fun start() {
        if (isTracking) return
        isTracking = true
        locationClient.lastLocation.addOnSuccessListener { location ->
            location?.let(::reportLocationIfFarEnough)
        }
        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper(),
        )
    }

    /** Stops foreground location updates. */
    fun stop() {
        if (!isTracking) return
        locationClient.removeLocationUpdates(locationCallback)
        isTracking = false
    }

    private fun reportLocationIfFarEnough(location: Location) {
        val previousLocation = lastReportedLocation
        if (
            previousLocation == null ||
            previousLocation.distanceTo(location) >= MINIMUM_LOCATION_CHANGE_METERS
        ) {
            lastReportedLocation = location
            onLocationChanged(UserLocation(location.latitude, location.longitude))
        }
    }
}
