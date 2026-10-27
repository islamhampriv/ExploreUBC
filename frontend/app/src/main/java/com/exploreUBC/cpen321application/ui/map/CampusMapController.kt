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
import com.example.cpen321application.maps.PolylineDecoder
import com.example.cpen321application.maps.PopularPlace
import com.example.cpen321application.maps.SampleMapData
import com.exploreUBC.cpen321application.BuildConfig
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.itineraries.ItineraryMap
import com.exploreUBC.cpen321application.data.itineraries.MapMarker
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
        placesApiKey = BuildConfig.PLACES_API_KEY,
    )
    private var mapTilesLoaded = false
    private var showMyLocation = false
    private var pendingItineraryMap: ItineraryMap? = null
    private var pendingOverlayTitle: String = ""
    private var renderedPoints: List<LatLng> = emptyList()
    private var renderedMarkers: List<MapMarker> = emptyList()

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

    fun showItineraryMap(itineraryMap: ItineraryMap, title: String) {
        if (mode != MapLayerMode.TRIP_ROUTES) return
        pendingItineraryMap = itineraryMap
        pendingOverlayTitle = title
        if (::map.isInitialized) {
            renderItineraryMap(itineraryMap, title)
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
            if (renderedPoints.isNotEmpty()) {
                fitCamera(renderedPoints)
            }
        }

        scope.launch {
            delay(4_000)
            if (!mapTilesLoaded) {
                showStaticMapFallback(renderedPoints)
            }
        }

        map.setOnInfoWindowClickListener { marker ->
            when (val tag = marker.tag) {
                is MapMarker -> openDirections(tag)
                is PopularPlace -> Toast.makeText(
                    context,
                    context.getString(R.string.map_add_popular_soon),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

        if (mode == MapLayerMode.POPULAR_PLACES) {
            scope.launch { loadPopularPlaces() }
            return
        }

        val itineraryMap = pendingItineraryMap
        if (itineraryMap != null) {
            renderItineraryMap(itineraryMap, pendingOverlayTitle)
        }
    }

    private fun renderItineraryMap(itineraryMap: ItineraryMap, title: String) {
        itineraryMarkers.forEach { it.remove() }
        itineraryMarkers.clear()
        itineraryPolylines.forEach { it.remove() }
        itineraryPolylines.clear()

        renderedMarkers = itineraryMap.markers
        renderedPoints = itineraryMap.markers.map { marker ->
            LatLng(marker.coordinates.latitude, marker.coordinates.longitude)
        }

        binding.textOverlayTitle.text = title.ifBlank {
            context.getString(R.string.map_overlay_title)
        }
        binding.textLegend.setText(R.string.map_legend_activities)
        binding.textMapStatus.setText(R.string.map_loading_routes)

        itineraryMap.markers.forEachIndexed { index, marker ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(marker.coordinates.latitude, marker.coordinates.longitude))
                    .title("${index + 1}. ${marker.title}")
                    .snippet(markerSnippet(marker))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
            )?.also { googleMarker ->
                googleMarker.tag = marker
                itineraryMarkers.add(googleMarker)
            }
        }

        itineraryMap.routes.forEach { route ->
            val points = runCatching {
                PolylineDecoder.decode(route.encodedPolyline)
            }.getOrDefault(emptyList())
            if (points.isEmpty()) return@forEach
            val polyline = map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(ContextCompat.getColor(context, R.color.ubc_blue))
                    .width(12f),
            )
            itineraryPolylines.add(polyline)
        }

        if (renderedPoints.isNotEmpty()) {
            fitCamera(renderedPoints)
        }
        binding.textMapStatus.text = statusMessage(itineraryMap)
    }

    private fun markerSnippet(marker: MapMarker): String = buildString {
        append(marker.address)
        if (marker.isApproximate) {
            append('\n')
            append(context.getString(R.string.map_marker_approximate))
        }
    }

    private fun statusMessage(itineraryMap: ItineraryMap): String {
        val parts = mutableListOf<String>()
        if (itineraryMap.markers.isEmpty()) {
            parts += context.getString(R.string.map_no_activities)
        } else if (itineraryMap.routes.isEmpty() && itineraryMap.markers.size >= 2) {
            parts += context.getString(R.string.map_routes_empty)
        } else if (itineraryMap.routes.isNotEmpty()) {
            parts += context.getString(R.string.map_routes_ready)
        }
        if (itineraryMap.unmappedActivities.isNotEmpty()) {
            val names = itineraryMap.unmappedActivities.joinToString { it.title }
            parts += context.getString(R.string.map_unmapped_stops, names)
        }
        if (itineraryMap.markers.any { it.isApproximate }) {
            parts += context.getString(R.string.map_approximate_locations)
        }
        return parts.joinToString(" ").ifBlank {
            context.getString(R.string.map_routes_ready)
        }
    }

    private fun openDirections(marker: MapMarker) {
        val index = renderedMarkers.indexOfFirst { it.activityId == marker.activityId }
        val next = renderedMarkers.getOrNull(index + 1)
        val origin = marker.coordinates
        val uri = if (next != null) {
            val destination = next.coordinates
            Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                    "&origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}",
            )
        } else {
            Uri.parse("google.navigation:q=${origin.latitude},${origin.longitude}")
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure {
            Toast.makeText(context, "Could not open directions", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun loadPopularPlaces() {
        popularMarkers.forEach { it.remove() }
        popularMarkers.clear()

        val center = SampleMapData.ubcCampus
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

    private suspend fun showStaticMapFallback(points: List<LatLng>) {
        val key = BuildConfig.MAPS_API_KEY
        if (key.isBlank() || key == "YOUR_API_KEY") {
            binding.textMapStatus.text =
                "Map tiles did not load. Add MAPS_API_KEY and enable Maps SDK for Android."
            return
        }

        val markers = points.take(5).joinToString("&") { point ->
            "markers=color:0x0055B7|${point.latitude},${point.longitude}"
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
