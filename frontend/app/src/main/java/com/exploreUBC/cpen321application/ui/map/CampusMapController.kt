package com.exploreUBC.cpen321application.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.cpen321application.maps.GoogleMapsApiClient
import com.example.cpen321application.maps.ItineraryStop
import com.example.cpen321application.maps.PopularPlace
import com.example.cpen321application.maps.SampleMapData
import com.exploreUBC.cpen321application.BuildConfig
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MapLayerMode {
    POPULAR_PLACES,
    TRIP_ROUTES,
}

class CampusMapController(
    private val context: Context,
    private val binding: ActivityMapsBinding,
    private val scope: CoroutineScope,
    private val mode: MapLayerMode,
) {
    private lateinit var map: GoogleMap
    private val popularMarkers = mutableListOf<Marker>()
    private val itineraryMarkers = mutableListOf<Marker>()
    private val itineraryPolylines = mutableListOf<Polyline>()
    private val mapsApi = GoogleMapsApiClient(
        mapsApiKey = BuildConfig.MAPS_API_KEY,
        geocodingApiKey = BuildConfig.GEOCODING_API_KEY,
        routesApiKey = BuildConfig.ROUTES_API_KEY,
        placesApiKey = BuildConfig.PLACES_API_KEY,
    )
    private var mapTilesLoaded = false
    private var showMyLocation = false
    private var pendingItineraries: List<Itinerary>? = null
    private var renderedStops: List<ItineraryStop> = emptyList()

    fun start() {
        MapsInitializer.initialize(context.applicationContext)

        binding.checkboxPopular.visibility = View.GONE
        if (mode == MapLayerMode.POPULAR_PLACES) {
            binding.textOverlayTitle.setText(R.string.map_overlay_popular_title)
            binding.textLegend.setText(R.string.map_legend_popular)
        }

        if (BuildConfig.MAPS_API_KEY.isBlank() || BuildConfig.MAPS_API_KEY == "YOUR_API_KEY") {
            binding.textMapStatus.text = "MAPS_API_KEY is missing from local.properties"
            Toast.makeText(context, "MAPS_API_KEY is missing from local.properties", Toast.LENGTH_LONG).show()
        } else {
            binding.textMapStatus.setText(
                if (mode == MapLayerMode.TRIP_ROUTES) {
                    R.string.map_loading_trips
                } else {
                    R.string.map_loading_popular
                },
            )
        }

        binding.map.getMapAsync(::onMapReady)
    }

    fun setMyLocationEnabled(enabled: Boolean) {
        showMyLocation = enabled
        if (::map.isInitialized && enabled) {
            map.isMyLocationEnabled = true
        }
    }

    fun showSavedItineraries(itineraries: List<Itinerary>) {
        if (mode != MapLayerMode.TRIP_ROUTES) return
        pendingItineraries = itineraries
        if (::map.isInitialized) {
            scope.launch { renderSavedItineraries(itineraries) }
        }
    }

    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SampleMapData.ubcCampus, 14.5f))
        if (showMyLocation || hasLocationPermission()) {
            map.isMyLocationEnabled = true
        }

        map.setOnMapLoadedCallback {
            mapTilesLoaded = true
            if (renderedStops.isNotEmpty()) {
                fitCamera(renderedStops.map { it.location })
            }
        }

        scope.launch {
            delay(4_000)
            if (!mapTilesLoaded) {
                showStaticMapFallback(renderedStops)
            }
        }

        map.setOnInfoWindowClickListener { marker ->
            when (val tag = marker.tag) {
                is ItineraryStop -> openDrivingDirections(tag)
                is PopularPlace -> Toast.makeText(
                    context,
                    context.getString(R.string.map_add_popular_soon),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        if (mode == MapLayerMode.POPULAR_PLACES) {
            scope.launch { loadPopularPlaces(emptyList()) }
            return
        }

        val itineraries = pendingItineraries
        if (itineraries != null) {
            scope.launch { renderSavedItineraries(itineraries) }
        }
    }

    private suspend fun renderSavedItineraries(itineraries: List<Itinerary>) {
        binding.textMapStatus.setText(R.string.map_loading_trips)
        val trips = withContext(Dispatchers.IO) { geocodeSavedItineraries(itineraries, mapsApi) }
        val stops = trips.flatMap { it.stops }
        renderedStops = stops

        itineraryMarkers.forEach { it.remove() }
        itineraryMarkers.clear()
        itineraryPolylines.forEach { it.remove() }
        itineraryPolylines.clear()

        if (itineraries.isEmpty()) {
            binding.textOverlayTitle.setText(R.string.map_overlay_title)
            binding.textLegend.setText(R.string.map_no_saved_trips)
            binding.textMapStatus.setText(R.string.map_no_saved_trips)
            return
        }

        binding.textOverlayTitle.text = trips.joinToString(" · ") { it.name }.ifBlank {
            context.getString(R.string.map_overlay_title)
        }
        binding.textLegend.setText(R.string.map_legend_activities)

        if (stops.isEmpty()) {
            binding.textMapStatus.setText(R.string.map_no_activities)
            return
        }

        trips.forEach { trip ->
            addActivityPins(trip.stops, BitmapDescriptorFactory.HUE_AZURE)
        }
        binding.textMapStatus.setText(R.string.map_loading_routes)
        fitCamera(stops.map { it.location })

        var usedDirectPath = false
        trips.forEach { trip ->
            if (trip.stops.size >= 2 && !drawDrivingRoute(trip.stops)) {
                usedDirectPath = true
            }
        }

        val unmapped = trips.sumOf { it.unmappedCount }
        binding.textMapStatus.setText(
            when {
                unmapped > 0 -> R.string.map_unmapped_stops
                usedDirectPath -> R.string.map_routes_fallback
                else -> R.string.map_routes_ready
            },
        )
    }

    private fun addActivityPins(stops: List<ItineraryStop>, hue: Float) {
        stops.forEachIndexed { index, stop ->
            val title = if (stop.itineraryName.isBlank()) {
                "${index + 1}. ${stop.name}"
            } else {
                "${stop.itineraryName}: ${index + 1}. ${stop.name}"
            }
            map.addMarker(
                MarkerOptions()
                    .position(stop.location)
                    .title(title)
                    .snippet(stopSnippet(stop))
                    .icon(BitmapDescriptorFactory.defaultMarker(hue))
            )?.also { marker ->
                marker.tag = stop
                itineraryMarkers.add(marker)
            }
        }
    }

    private fun stopSnippet(stop: ItineraryStop): String {
        val time = if (stop.startTime.isBlank() && stop.endTime.isBlank()) {
            stop.type
        } else {
            "${stop.type} · ${stop.startTime}–${stop.endTime}"
        }
        val driveHint = if (renderedStops.size >= 2) {
            "\nTap to drive to the next pin"
        } else {
            ""
        }
        return "$time\n${stop.address}$driveHint"
    }

    private suspend fun drawDrivingRoute(stops: List<ItineraryStop>): Boolean {
        val routePoints = withContext(Dispatchers.IO) { mapsApi.fetchDrivingRoute(stops) }
        val usedApiRoute = routePoints != null
        val points = routePoints ?: stops.map { it.location }
        val polyline = map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(ContextCompat.getColor(context, R.color.ubc_blue))
                .width(12f)
        )
        itineraryPolylines.add(polyline)
        return usedApiRoute
    }

    private fun openDrivingDirections(stop: ItineraryStop) {
        val index = renderedStops.indexOfFirst {
            it.address == stop.address && it.name == stop.name && it.itineraryName == stop.itineraryName
        }
        val next = renderedStops.getOrNull(index + 1)
        val uri = if (next != null) {
            Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                    "&origin=${stop.location.latitude},${stop.location.longitude}" +
                    "&destination=${next.location.latitude},${next.location.longitude}" +
                    "&travelmode=driving",
            )
        } else {
            Uri.parse("google.navigation:q=${stop.location.latitude},${stop.location.longitude}")
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure {
            Toast.makeText(context, "Could not open driving directions", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun loadPopularPlaces(stops: List<ItineraryStop>) {
        popularMarkers.forEach { it.remove() }
        popularMarkers.clear()

        val center = stops.firstOrNull()?.location ?: SampleMapData.ubcCampus
        val remotePlaces = withContext(Dispatchers.IO) { mapsApi.fetchPopularPlaces(center) }
        val places = remotePlaces.takeUnless { it.isNullOrEmpty() } ?: SampleMapData.fallbackPopularPlaces

        places.forEach { place ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(place.location)
                    .title(place.name)
                    .snippet("${place.category} · ${place.address}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
            marker?.tag = place
            if (marker != null) popularMarkers.add(marker)
        }

        fitCamera(places.map { it.location })
        binding.textMapStatus.setText(
            if (remotePlaces.isNullOrEmpty()) R.string.map_popular_fallback else R.string.map_popular_ready
        )
    }

    private fun fitCamera(points: List<LatLng>) {
        if (points.isEmpty()) return
        if (points.size == 1) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 15f))
            return
        }

        val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
        try {
            if (binding.map.width > 0 && binding.map.height > 0) {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160))
            } else {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 14f))
            }
        } catch (_: IllegalStateException) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 14f))
        }
    }

    private suspend fun showStaticMapFallback(stops: List<ItineraryStop>) {
        val key = BuildConfig.MAPS_API_KEY
        if (key.isBlank() || key == "YOUR_API_KEY") {
            binding.textMapStatus.text =
                "Map tiles did not load. Add MAPS_API_KEY and enable Maps SDK for Android."
            return
        }

        val markers = stops.take(5).joinToString("&") { stop ->
            "markers=color:0x0055B7|${stop.location.latitude},${stop.location.longitude}"
        }
        val url =
            "https://maps.googleapis.com/maps/api/staticmap?center=49.2606,-123.2460&zoom=14&size=600x900&scale=2&maptype=roadmap&$markers&key=$key"

        val bitmap = withContext(Dispatchers.IO) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }

        if (bitmap != null) {
            binding.mapFallback.setImageBitmap(bitmap)
            binding.mapFallback.visibility = View.VISIBLE
            binding.textMapStatus.text =
                "Showing a static campus map. Enable Maps SDK for Android on this API key for the interactive map."
        } else {
            binding.textMapStatus.text = "Map tiles did not load. Enable Maps SDK for Android."
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
