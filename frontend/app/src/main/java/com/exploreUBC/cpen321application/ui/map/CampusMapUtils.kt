package com.exploreUBC.cpen321application.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import androidx.core.content.ContextCompat
import com.example.cpen321application.maps.PolylineDecoder
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.itineraries.MapRoute
import com.exploreUBC.cpen321application.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

internal fun Context.hasMapLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

internal fun ActivityMapsBinding.updateTravelLegend(travelMode: TripTravelMode) {
    textLegend.setText(
        if (travelMode == TripTravelMode.WALKING) {
            R.string.map_legend_walking
        } else {
            R.string.map_legend_driving
        },
    )
}

internal fun routePolylinePoints(route: MapRoute, travelMode: TripTravelMode): List<LatLng> {
    val selectedRoute = when (travelMode) {
        TripTravelMode.WALKING -> route.walking
        TripTravelMode.DRIVING -> route.driving
    }
    val encoded = selectedRoute?.encodedPolyline?.takeIf { it.isNotBlank() } ?: route.encodedPolyline
    return runCatching { PolylineDecoder.decode(encoded) }.getOrDefault(emptyList())
}

internal fun routePolylineColor(
    selected: Boolean,
    travelMode: TripTravelMode,
    context: Context,
): Int {
    val colorRes = if (!selected) {
        R.color.text_secondary
    } else if (travelMode == TripTravelMode.WALKING) {
        R.color.ubc_gold
    } else {
        R.color.ubc_blue_mid
    }
    return ContextCompat.getColor(context, colorRes)
}

internal fun fitMapCamera(map: GoogleMap, points: List<LatLng>, hostView: View) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 15f))
        return
    }

    val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
    try {
        if (hostView.width > 0 && hostView.height > 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160))
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 14f))
        }
    } catch (_: IllegalStateException) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 14f))
    }
}
