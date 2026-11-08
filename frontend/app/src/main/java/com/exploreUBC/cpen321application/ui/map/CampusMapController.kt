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
import com.exploreUBC.cpen321application.data.itineraries.MapRoute
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MapLayerMode {
    POPULAR_PLACES,
    TRIP_ROUTES,
}

enum class TripTravelMode {
    WALKING,
    DRIVING,
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
    private var renderedRoutes: List<MapRoute> = emptyList()
    private var travelMode: TripTravelMode = TripTravelMode.DRIVING
    private var selectedRouteKey: String? = null
    private var controlBottomInsetPx = 0
    private var popularPlacesEnabled = true
    private var popularPlacesJob: Job? = null
    private var onMapClick: () -> Unit = {}

    fun start() {
        MapsInitializer.initialize(context.applicationContext)

        binding.checkboxPopular.visibility = View.GONE
        binding.mapOverlayControls.visibility = View.GONE
        binding.textMapStatus.setOnClickListener {
            binding.textMapStatus.visibility = View.GONE
        }
        if (mode == MapLayerMode.TRIP_ROUTES) {
            binding.textMapStatus.visibility = View.GONE
        }
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

    fun setPopularPlacesEnabled(enabled: Boolean) {
        if (popularPlacesEnabled == enabled) return
        popularPlacesEnabled = enabled
        if (!::map.isInitialized) return
        if (enabled) {
            popularPlacesJob = scope.launch { loadPopularPlaces() }
        } else {
            popularPlacesJob?.cancel()
            popularMarkers.forEach { it.remove() }
            popularMarkers.clear()
            if (mode == MapLayerMode.POPULAR_PLACES) {
                binding.textMapStatus.setText(R.string.map_popular_hidden)
            }
        }
    }

    fun setControlBottomInset(bottomPx: Int) {
        controlBottomInsetPx = bottomPx.coerceAtLeast(0)
        if (::map.isInitialized) {
            map.setPadding(0, 0, 0, controlBottomInsetPx)
        }
    }

    fun setOnMapClickListener(listener: () -> Unit) {
        onMapClick = listener
        if (::map.isInitialized) {
            map.setOnMapClickListener { onMapClick() }
        }
    }

    fun setTravelMode(tripMode: TripTravelMode) {
        if (mode != MapLayerMode.TRIP_ROUTES) return
        val shouldRedraw = travelMode != tripMode && ::map.isInitialized
        travelMode = tripMode
        if (shouldRedraw) {
            redrawTravelPolylines()
            updateTravelLegend()
        }
    }

    fun setSelectedRoute(routeKey: String?) {
        val shouldRedraw = selectedRouteKey != routeKey && ::map.isInitialized
        selectedRouteKey = routeKey
        if (shouldRedraw) {
            redrawTravelPolylines()
        }
    }

    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setPadding(0, 0, 0, controlBottomInsetPx)
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
                fitMapCamera(map, renderedPoints, binding.map)
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
        map.setOnMapClickListener { onMapClick() }

        val itineraryMap = pendingItineraryMap
        if (itineraryMap != null) {
            renderItineraryMap(itineraryMap, pendingOverlayTitle)
        }
        if (popularPlacesEnabled) {
            popularPlacesJob = scope.launch { loadPopularPlaces() }
        }
    }

    private fun renderItineraryMap(itineraryMap: ItineraryMap, title: String) {
        itineraryMarkers.forEach { it.remove() }
        itineraryMarkers.clear()
        itineraryPolylines.forEach { it.remove() }
        itineraryPolylines.clear()

        renderedMarkers = itineraryMap.markers
        renderedRoutes = itineraryMap.routes
        renderedPoints = itineraryMap.markers.map { marker ->
            LatLng(marker.coordinates.latitude, marker.coordinates.longitude)
        }

        binding.textOverlayTitle.text = title.ifBlank {
            context.getString(R.string.map_overlay_title)
        }
        updateTravelLegend()
        binding.textMapStatus.setText(R.string.map_loading_routes)

        itineraryMap.markers.forEachIndexed { index, marker ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(marker.coordinates.latitude, marker.coordinates.longitude))
                    .title("${index + 1}. ${marker.title}")
                    .snippet(MapItineraryText.markerSnippet(context, marker, renderedRoutes))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
            )?.also { googleMarker ->
                googleMarker.tag = marker
                itineraryMarkers.add(googleMarker)
            }
        }

        redrawTravelPolylines()

        if (renderedPoints.isNotEmpty()) {
            fitMapCamera(map, renderedPoints, binding.map)
        }
        binding.textMapStatus.text = MapItineraryText.statusMessage(context, itineraryMap)
    }

    private fun redrawTravelPolylines() {
        itineraryPolylines.forEach { it.remove() }
        itineraryPolylines.clear()
        val activeRouteKey = selectedRouteKey ?: renderedRoutes.firstOrNull()?.routeKey()
        renderedRoutes.sortedBy { it.routeKey() == activeRouteKey }.forEach { route ->
            addRoutePolyline(route, activeRouteKey)
        }
    }

    private fun addRoutePolyline(route: MapRoute, activeRouteKey: String?) {
        val points = routePolylinePoints(route, travelMode)
        if (points.isNotEmpty()) {
            val selected = route.routeKey() == activeRouteKey
            itineraryPolylines.add(
                map.addPolyline(
                    PolylineOptions()
                        .addAll(points)
                        .color(routePolylineColor(selected, travelMode, context))
                        .width(if (selected) 14f else 8f)
                        .zIndex(if (selected) 2f else 1f),
                ),
            )
        }
    }

    private fun updateTravelLegend() {
        binding.textLegend.setText(
            if (travelMode == TripTravelMode.WALKING) {
                R.string.map_legend_walking
            } else {
                R.string.map_legend_driving
            },
        )
    }

    private fun openDirections(marker: MapMarker) {
        val index = renderedMarkers.indexOfFirst { it.activityId == marker.activityId }
        val next = renderedMarkers.getOrNull(index + 1)
        val origin = marker.coordinates
        val uri = if (next != null) {
            val destination = next.coordinates
            val mapsMode = if (travelMode == TripTravelMode.WALKING) "walking" else "driving"
            Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                    "&origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&travelmode=$mapsMode",
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
        withContext(Dispatchers.Main) {
            popularMarkers.forEach { it.remove() }
            popularMarkers.clear()
        }

        val center = SampleMapData.ubcCampus
        val remotePlaces = withContext(Dispatchers.IO) { mapsApi.fetchPopularPlaces(center) }
        val places = remotePlaces.takeUnless { it.isNullOrEmpty() }
            ?: SampleMapData.fallbackPopularPlaces

        withContext(Dispatchers.Main) {
            if (::map.isInitialized) {
                addPopularMarkers(places)
                if (renderedPoints.isEmpty()) {
                    fitMapCamera(map, places.map { it.location }, binding.map)
                }
                binding.textMapStatus.setText(R.string.map_popular_ready)
            }
        }
    }

    private fun addPopularMarkers(places: List<PopularPlace>) {
        places.forEach { place ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(place.location)
                    .title(place.name)
                    .snippet("${place.category} · ${place.address}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)),
            )
            marker?.tag = place
            if (marker != null) popularMarkers.add(marker)
        }
    }

    private suspend fun showStaticMapFallback(points: List<LatLng>) {
        val key = BuildConfig.MAPS_API_KEY
        if (key.isBlank() || key == "YOUR_API_KEY") {
            withContext(Dispatchers.Main) {
                binding.textMapStatus.text =
                    "Map tiles did not load. Add MAPS_API_KEY and enable Maps SDK for Android."
            }
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
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { BitmapFactory.decodeStream(it) }
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }

        withContext(Dispatchers.Main) {
            if (bitmap != null) {
                binding.mapFallback.setImageBitmap(bitmap)
                binding.mapFallback.visibility = View.VISIBLE
                binding.textMapStatus.text =
                    "Showing a static campus map. Enable Maps SDK for Android on this API key for the interactive map."
            } else {
                binding.textMapStatus.text = "Map tiles did not load. Enable Maps SDK for Android."
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
