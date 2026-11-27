package com.exploreUBC.cpen321application.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.cpen321application.maps.GoogleMapsApiClient
import com.example.cpen321application.maps.PolylineDecoder
import com.example.cpen321application.maps.PopularPlace
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
import kotlinx.coroutines.CancellationException
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
    private var mapTilesTimedOut = false
    private var popularPlacesFailed = false
    private var popularPlacesCount: Int? = null
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
    private val campusCenter = LatLng(49.2606, -123.2460)

    fun start() = onMainThread {
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

    fun setMyLocationEnabled(enabled: Boolean) = onMainThread {
        showMyLocation = enabled
        if (::map.isInitialized && enabled) {
            map.isMyLocationEnabled = true
        }
    }

    fun showItineraryMap(itineraryMap: ItineraryMap, title: String) = onMainThread {
        if (mode == MapLayerMode.TRIP_ROUTES) {
            pendingItineraryMap = itineraryMap
            pendingOverlayTitle = title
            if (::map.isInitialized) {
                renderItineraryMap(itineraryMap, title)
            }
        }
    }

    fun setPopularPlacesEnabled(enabled: Boolean) = onMainThread {
        if (popularPlacesEnabled != enabled) {
            popularPlacesEnabled = enabled
            if (::map.isInitialized) {
                if (enabled) {
                    binding.textMapStatus.setText(R.string.map_loading_popular)
                    binding.textMapStatus.visibility = View.VISIBLE
                    popularPlacesJob = scope.launch { loadPopularPlaces() }
                } else {
                    popularPlacesJob?.cancel()
                    popularPlacesFailed = false
                    popularMarkers.forEach { it.remove() }
                    popularMarkers.clear()
                    if (mode == MapLayerMode.POPULAR_PLACES) {
                        if (mapTilesLoaded) binding.map.visibility = View.VISIBLE
                        binding.textMapStatus.setText(R.string.map_popular_hidden)
                        binding.textMapStatus.setOnClickListener {
                            binding.textMapStatus.visibility = View.GONE
                        }
                    } else {
                        binding.textMapStatus.text = pendingItineraryMap?.let {
                            MapItineraryText.statusMessage(context, it)
                        } ?: context.getString(R.string.map_loading_trips)
                    }
                }
            }
        }
    }

    fun setControlBottomInset(bottomPx: Int) = onMainThread {
        controlBottomInsetPx = bottomPx.coerceAtLeast(0)
        if (::map.isInitialized) {
            map.setPadding(0, 0, 0, controlBottomInsetPx)
        }
    }

    fun setOnMapClickListener(listener: () -> Unit) = onMainThread {
        onMapClick = listener
        if (::map.isInitialized) {
            map.setOnMapClickListener { onMapClick() }
        }
    }

    fun setTravelMode(tripMode: TripTravelMode) = onMainThread {
        if (mode == MapLayerMode.TRIP_ROUTES) {
            val shouldRedraw = travelMode != tripMode && ::map.isInitialized
            travelMode = tripMode
            if (shouldRedraw) {
                redrawTravelPolylines()
                updateTravelLegend()
            }
        }
    }

    fun setSelectedRoute(routeKey: String?) = onMainThread {
        val shouldRedraw = selectedRouteKey != routeKey && ::map.isInitialized
        selectedRouteKey = routeKey
        if (shouldRedraw) {
            redrawTravelPolylines()
        }
    }

    private fun onMapReady(googleMap: GoogleMap) = onMainThread {
        map = googleMap
        map.setPadding(0, 0, 0, controlBottomInsetPx)
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 14.5f))
        if (showMyLocation || hasLocationPermission()) {
            map.isMyLocationEnabled = true
        }

        map.setOnMapLoadedCallback {
            mapTilesLoaded = true
            if (mapTilesTimedOut) {
                mapTilesTimedOut = false
                if (!popularPlacesFailed || mode == MapLayerMode.TRIP_ROUTES) {
                    binding.map.visibility = View.VISIBLE
                }
                if (mode == MapLayerMode.POPULAR_PLACES && !popularPlacesFailed) {
                    binding.textMapStatus.setText(
                        when (popularPlacesCount) {
                            null -> R.string.map_loading_popular
                            0 -> R.string.map_popular_empty
                            else -> R.string.map_popular_ready
                        },
                    )
                } else if (mode == MapLayerMode.TRIP_ROUTES && !popularPlacesFailed) {
                    binding.textMapStatus.text = pendingItineraryMap?.let {
                        MapItineraryText.statusMessage(context, it)
                    } ?: context.getString(R.string.map_loading_trips)
                }
            }
            if (renderedPoints.isNotEmpty()) {
                fitMapCamera(map, renderedPoints, binding.map)
            }
        }

        scope.launch {
            delay(4_000)
            if (!mapTilesLoaded) {
                mapTilesTimedOut = true
                if (!popularPlacesFailed || mode == MapLayerMode.TRIP_ROUTES) {
                    binding.textMapStatus.setText(R.string.map_tiles_failed)
                    binding.textMapStatus.visibility = View.VISIBLE
                    binding.textMapStatus.setOnClickListener { }
                }
                binding.map.visibility = View.INVISIBLE
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
        withContext(Dispatchers.Main.immediate) {
            popularMarkers.forEach { it.remove() }
            popularMarkers.clear()
        }

        val places = try {
            withContext(Dispatchers.IO) { mapsApi.fetchPopularPlaces(campusCenter) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        withContext(Dispatchers.Main.immediate) {
            if (places == null) {
                popularPlacesFailed = true
                popularPlacesCount = null
                if (mode == MapLayerMode.POPULAR_PLACES) {
                    binding.map.visibility = View.INVISIBLE
                }
                binding.textMapStatus.setText(R.string.map_popular_failed)
                binding.textMapStatus.visibility = View.VISIBLE
                binding.textMapStatus.setOnClickListener {
                    binding.textMapStatus.setText(R.string.map_loading_popular)
                    popularPlacesJob = scope.launch { loadPopularPlaces() }
                }
                return@withContext
            }
            popularPlacesFailed = false
            popularPlacesCount = places.size
            if (mapTilesLoaded) binding.map.visibility = View.VISIBLE
            if (mode == MapLayerMode.POPULAR_PLACES) {
                binding.textMapStatus.setOnClickListener {
                    binding.textMapStatus.visibility = View.GONE
                }
            }
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

            if (renderedPoints.isEmpty() && places.isNotEmpty()) {
                fitMapCamera(map, places.map { it.location }, binding.map)
            }
            if (mapTilesTimedOut) {
                binding.textMapStatus.setText(R.string.map_tiles_failed)
                binding.textMapStatus.visibility = View.VISIBLE
                binding.textMapStatus.setOnClickListener { }
            } else if (mode == MapLayerMode.POPULAR_PLACES) {
                binding.textMapStatus.setText(
                    if (places.isEmpty()) R.string.map_popular_empty else R.string.map_popular_ready,
                )
            } else if (mode == MapLayerMode.TRIP_ROUTES) {
                binding.textMapStatus.text = pendingItineraryMap?.let {
                    MapItineraryText.statusMessage(context, it)
                } ?: context.getString(R.string.map_loading_trips)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun onMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            binding.root.post(action)
        }
    }
}
