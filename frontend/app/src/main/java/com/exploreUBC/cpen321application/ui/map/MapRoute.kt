package com.exploreUBC.cpen321application.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MapRoute(modifier: Modifier = Modifier) {
    CampusMapView(
        mode = MapLayerMode.POPULAR_PLACES,
        modifier = modifier,
    )
}
