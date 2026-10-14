package com.exploreUBC.cpen321application.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.cpen321application.maps.GoogleMapsApiClient
import com.example.cpen321application.maps.ItineraryStop
import com.example.cpen321application.maps.PopularPlace
import com.example.cpen321application.maps.SampleMapData
import com.exploreUBC.cpen321application.BuildConfig
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CampusMapController(
    private val context: Context,
    private val binding: ActivityMapsBinding,
    private val scope: CoroutineScope,
) {
    private lateinit var map: GoogleMap
    private val popularMarkers = mutableListOf<Marker>()
    private val mapsApi = GoogleMapsApiClient(BuildConfig.MAPS_API_KEY)
    private var mapTilesLoaded = false
    private var showMyLocation = false

    fun start() {
        MapsInitializer.initialize(context.applicationContext)

        if (BuildConfig.MAPS_API_KEY.isBlank() || BuildConfig.MAPS_API_KEY == "YOUR_API_KEY") {
            binding.textMapStatus.text = "MAPS_API_KEY is missing from local.properties"
            Toast.makeText(context, "MAPS_API_KEY is missing from local.properties", Toast.LENGTH_LONG).show()
        } else {
            binding.textMapStatus.text = "Loading Google Map…"
        }

        binding.map.getMapAsync(::onMapReady)
    }

    fun setMyLocationEnabled(enabled: Boolean) {
        showMyLocation = enabled
        if (::map.isInitialized && enabled) {
            map.isMyLocationEnabled = true
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

        val stops = SampleMapData.sampleItinerary
        addActivityPins(stops)
        binding.textMapStatus.setText(R.string.map_loading_routes)

        map.setOnMapLoadedCallback {
            mapTilesLoaded = true
            fitCamera(stops.map { it.location })
            binding.textMapStatus.setText(R.string.map_routes_ready)
        }

        scope.launch {
            delay(4_000)
            if (!mapTilesLoaded) {
                showStaticMapFallback(stops)
            }
        }

        map.setOnInfoWindowClickListener { marker ->
            if (marker.tag is PopularPlace) {
                Toast.makeText(context, context.getString(R.string.map_add_popular_soon), Toast.LENGTH_SHORT).show()
            }
        }

        binding.checkboxPopular.setOnCheckedChangeListener { _, checked ->
            popularMarkers.forEach { it.isVisible = checked }
        }

        scope.launch {
            drawRoutes(stops)
            loadPopularPlaces(stops)
        }
    }

    private fun addActivityPins(stops: List<ItineraryStop>) {
        stops.forEachIndexed { index, stop ->
            map.addMarker(
                MarkerOptions()
                    .position(stop.location)
                    .title("${index + 1}. ${stop.name}")
                    .snippet("${stop.type} · ${stop.startTime}–${stop.endTime}\n${stop.address}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )?.tag = stop
        }
    }

    private suspend fun drawRoutes(stops: List<ItineraryStop>) {
        val routePoints = withContext(Dispatchers.IO) { mapsApi.fetchRoutePolyline(stops) }
        val points = routePoints ?: stops.map { it.location }

        map.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(ContextCompat.getColor(context, R.color.ubc_blue))
                .width(12f)
        )
    }

    private suspend fun loadPopularPlaces(stops: List<ItineraryStop>) {
        val center = stops.firstOrNull()?.location ?: SampleMapData.ubcCampus
        val remotePlaces = withContext(Dispatchers.IO) { mapsApi.fetchPopularPlaces(center) }
        val places = remotePlaces.takeUnless { it.isNullOrEmpty() } ?: SampleMapData.fallbackPopularPlaces
        val usedFallback = remotePlaces.isNullOrEmpty()

        places.forEach { place ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(place.location)
                    .title(place.name)
                    .snippet("${place.category} · ${place.address}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .visible(binding.checkboxPopular.isChecked)
            )
            marker?.tag = place
            if (marker != null) popularMarkers.add(marker)
        }

        fitCamera(stops.map { it.location } + places.map { it.location })
        binding.textMapStatus.setText(
            if (usedFallback) R.string.map_popular_fallback else R.string.map_popular_ready
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
