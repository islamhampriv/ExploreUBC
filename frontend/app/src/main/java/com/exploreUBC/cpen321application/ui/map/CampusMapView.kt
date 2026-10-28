package com.exploreUBC.cpen321application.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.MapView
import com.exploreUBC.cpen321application.data.itineraries.ItineraryMap
import com.exploreUBC.cpen321application.databinding.ActivityMapsBinding

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun CampusMapView(
    mode: MapLayerMode,
    modifier: Modifier = Modifier,
    itineraryMap: ItineraryMap? = null,
    overlayTitle: String? = null,
    travelMode: TripTravelMode = TripTravelMode.DRIVING,
    controlsBottomInsetPx: Int = 0,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val binding = remember(mode) {
        ActivityMapsBinding.inflate(LayoutInflater.from(context))
    }
    val controller = remember(binding, mode) {
        CampusMapController(context, binding, scope, mode)
    }

    BindCampusMapLifecycle(lifecycleOwner, binding, controller)
    RequestCampusMapLocation(controller)
    ShowItineraryOnMap(
        mode,
        itineraryMap,
        overlayTitle,
        travelMode,
        controlsBottomInsetPx,
        controller,
    )
    AndroidView(
        factory = { binding.root },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun BindCampusMapLifecycle(
    lifecycleOwner: LifecycleOwner,
    binding: ActivityMapsBinding,
    controller: CampusMapController,
) {
    DisposableEffect(lifecycleOwner, binding) {
        val mapView = binding.map
        mapView.onCreate(null)
        controller.start()
        val observer = mapLifecycleObserver(mapView)
        lifecycleOwner.lifecycle.addObserver(observer)
        syncMapViewToLifecycle(mapView, lifecycleOwner.lifecycle.currentState)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
}

private fun mapLifecycleObserver(
    mapView: MapView,
): LifecycleEventObserver = LifecycleEventObserver { _, event ->
    when (event) {
        Lifecycle.Event.ON_START -> mapView.onStart()
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> mapView.onStop()
        Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
        else -> Unit
    }
}

private fun syncMapViewToLifecycle(
    mapView: MapView,
    state: Lifecycle.State,
) {
    if (state.isAtLeast(Lifecycle.State.STARTED)) {
        mapView.onStart()
    }
    if (state.isAtLeast(Lifecycle.State.RESUMED)) {
        mapView.onResume()
    }
}

@Composable
private fun RequestCampusMapLocation(controller: CampusMapController) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        controller.setMyLocationEnabled(
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
        )
    }
    LaunchedEffect(Unit) {
        val granted = locationPermissions.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            controller.setMyLocationEnabled(true)
        } else {
            permissionLauncher.launch(locationPermissions)
        }
    }
}

@Composable
private fun ShowItineraryOnMap(
    mode: MapLayerMode,
    itineraryMap: ItineraryMap?,
    overlayTitle: String?,
    travelMode: TripTravelMode,
    controlsBottomInsetPx: Int,
    controller: CampusMapController,
) {
    LaunchedEffect(controlsBottomInsetPx) {
        controller.setControlBottomInset(controlsBottomInsetPx)
    }
    LaunchedEffect(mode, itineraryMap, overlayTitle) {
        if (mode == MapLayerMode.TRIP_ROUTES && itineraryMap != null) {
            controller.showItineraryMap(itineraryMap, overlayTitle.orEmpty())
        }
    }
    LaunchedEffect(travelMode) {
        if (mode == MapLayerMode.TRIP_ROUTES) {
            controller.setTravelMode(travelMode)
        }
    }
}
