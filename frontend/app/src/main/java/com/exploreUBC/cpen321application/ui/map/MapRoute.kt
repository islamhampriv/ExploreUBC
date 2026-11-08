package com.exploreUBC.cpen321application.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryHttpException
import com.exploreUBC.cpen321application.data.itineraries.ItineraryMap

private sealed interface SavedItinerariesState {
    data object Loading : SavedItinerariesState
    data class Ready(val itineraries: List<Itinerary>) : SavedItinerariesState
    data class Failed(val message: String) : SavedItinerariesState
}

private sealed interface SelectedMapState {
    data object Loading : SelectedMapState
    data class Ready(val map: ItineraryMap) : SelectedMapState
    data object Missing : SelectedMapState
    data class Failed(val message: String) : SelectedMapState
}

private class MapRouteUiState {
    var itinerariesState by mutableStateOf<SavedItinerariesState>(SavedItinerariesState.Loading)
    var selectedItinerary by mutableStateOf<Itinerary?>(null)
    var itineraryMapState by mutableStateOf<SelectedMapState>(SelectedMapState.Loading)
    var mapReloadToken by mutableIntStateOf(0)
    var searchText by mutableStateOf("")
    var popularPlacesEnabled by mutableStateOf(true)
    var travelMode by mutableStateOf(TripTravelMode.DRIVING)
    var selectedRouteKey by mutableStateOf<String?>(null)
    var overlayHeightPx by mutableIntStateOf(0)
    var cardsVisible by mutableStateOf(true)
}

@Composable
fun MapRoute(
    modifier: Modifier = Modifier,
    userId: String? = null,
    itinerariesRepository: ItinerariesRepository? = null,
    selectedItineraryId: String? = null,
    onItinerarySelected: (String?) -> Unit = {},
    onVisitItinerary: () -> Unit = {},
    showMap: Boolean = true,
) {
    val uiState = remember(userId, itinerariesRepository) { MapRouteUiState() }
    LoadSavedItineraries(userId, itinerariesRepository, uiState)
    SyncSelectedItinerary(selectedItineraryId, uiState)
    LoadSelectedItineraryMap(userId, selectedItineraryId, itinerariesRepository, uiState)
    ResetSelectedMapControls(selectedItineraryId, uiState)
    MapRouteContent(
        selectedItineraryId,
        uiState,
        onItinerarySelected,
        onVisitItinerary,
        showMap,
        modifier,
    )
}

@Composable
private fun LoadSavedItineraries(
    userId: String?,
    itinerariesRepository: ItinerariesRepository?,
    uiState: MapRouteUiState,
) {
    LaunchedEffect(userId, itinerariesRepository) {
        val repository = itinerariesRepository
        val ownerId = userId?.takeIf(String::isNotBlank)
        if (repository == null || ownerId == null) {
            uiState.itinerariesState = SavedItinerariesState.Failed(
                "A user ID is required to search saved itineraries.",
            )
        } else {
            uiState.itinerariesState = SavedItinerariesState.Loading
            runCatching { repository.getItineraries(ownerId, saved = true) }
                .onSuccess { uiState.itinerariesState = SavedItinerariesState.Ready(it) }
                .onFailure {
                    uiState.itinerariesState = SavedItinerariesState.Failed(
                        it.message ?: "Could not load saved itineraries.",
                    )
                }
        }
    }
}

@Composable
private fun SyncSelectedItinerary(selectedItineraryId: String?, uiState: MapRouteUiState) {
    LaunchedEffect(selectedItineraryId, uiState.itinerariesState) {
        uiState.selectedItinerary = when (val state = uiState.itinerariesState) {
            is SavedItinerariesState.Ready ->
                state.itineraries.firstOrNull { it.id == selectedItineraryId }
            else -> null
        }
    }
}

@Composable
private fun LoadSelectedItineraryMap(
    userId: String?,
    selectedItineraryId: String?,
    itinerariesRepository: ItinerariesRepository?,
    uiState: MapRouteUiState,
) {
    LaunchedEffect(
        userId,
        selectedItineraryId,
        itinerariesRepository,
        uiState.mapReloadToken,
    ) {
        val repository = itinerariesRepository
        val ownerId = userId?.takeIf(String::isNotBlank)
        if (selectedItineraryId == null) {
            uiState.itineraryMapState = SelectedMapState.Loading
        } else if (repository == null || ownerId == null) {
            uiState.itineraryMapState = SelectedMapState.Failed(
                "A user ID is required to load itinerary directions.",
            )
        } else {
            uiState.itineraryMapState = SelectedMapState.Loading
            runCatching { repository.getItineraryMap(ownerId, selectedItineraryId) }
                .onSuccess {
                    uiState.itineraryMapState = SelectedMapState.Ready(it)
                    uiState.selectedRouteKey = it.routes.firstOrNull()?.routeKey()
                }
                .onFailure { error ->
                    uiState.itineraryMapState = if (
                        error is ItineraryHttpException && error.statusCode == 404
                    ) {
                        SelectedMapState.Missing
                    } else {
                        SelectedMapState.Failed(
                            error.message ?: "Could not load itinerary directions.",
                        )
                    }
                }
        }
    }
}

@Composable
private fun ResetSelectedMapControls(selectedItineraryId: String?, uiState: MapRouteUiState) {
    LaunchedEffect(selectedItineraryId) {
        uiState.travelMode = TripTravelMode.DRIVING
        uiState.selectedRouteKey = null
        uiState.overlayHeightPx = 0
    }
}

@Composable
private fun MapRouteContent(
    selectedItineraryId: String?,
    uiState: MapRouteUiState,
    onItinerarySelected: (String?) -> Unit,
    onVisitItinerary: () -> Unit,
    showMap: Boolean,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (showMap) {
            MapBackground(selectedItineraryId, uiState)
        }
        AnimatedVisibility(
            visible = uiState.cardsVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { uiState.overlayHeightPx = it.height },
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SavedItinerarySearch(
                    model = uiState.searchModel(selectedItineraryId, onItinerarySelected),
                    modifier = Modifier.fillMaxWidth(),
                )
                SelectedMapOverlay(
                    selectedItineraryId,
                    uiState,
                    onVisitItinerary,
                    Modifier.fillMaxWidth(),
                )
            }
        }
        if (!uiState.cardsVisible) {
            Button(
                onClick = { uiState.cardsVisible = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .testTag("map_show_controls"),
            ) {
                Text(stringResource(R.string.map_show_controls))
            }
        }
    }
}

@Composable
private fun MapBackground(selectedItineraryId: String?, uiState: MapRouteUiState) {
    CampusMapView(
        mode = if (selectedItineraryId == null) {
            MapLayerMode.POPULAR_PLACES
        } else {
            MapLayerMode.TRIP_ROUTES
        },
        options = CampusMapViewOptions(
            itineraryMap = (uiState.itineraryMapState as? SelectedMapState.Ready)?.map,
            overlayTitle = uiState.selectedItinerary?.name,
            travelMode = uiState.travelMode,
            selectedRouteKey = uiState.selectedRouteKey,
            controlsBottomInsetPx = uiState.overlayHeightPx,
            popularPlacesEnabled = uiState.popularPlacesEnabled,
            onMapClick = { uiState.cardsVisible = false },
        ),
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun SelectedMapOverlay(
    selectedItineraryId: String?,
    uiState: MapRouteUiState,
    onVisitItinerary: () -> Unit,
    modifier: Modifier,
) {
    when (val mapState = uiState.itineraryMapState) {
        SelectedMapState.Loading -> LoadingMapOverlay(selectedItineraryId, modifier)
        is SelectedMapState.Failed -> MapMessage(
            mapState.message,
            modifier,
            stringResource(R.string.map_retry),
            onAction = { uiState.mapReloadToken += 1 },
        )
        SelectedMapState.Missing -> MapMessage(
            stringResource(R.string.map_itinerary_missing),
            modifier,
            stringResource(R.string.map_visit_itinerary),
            onAction = onVisitItinerary,
        )
        is SelectedMapState.Ready -> TripTravelOverlay(
            itineraryMap = mapState.map,
            travelMode = uiState.travelMode,
            selectedRouteKey = uiState.selectedRouteKey,
            onTravelModeChange = { uiState.travelMode = it },
            onRouteSelected = { uiState.selectedRouteKey = it },
            onVisitItinerary = onVisitItinerary,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoadingMapOverlay(selectedItineraryId: String?, modifier: Modifier) {
    if (selectedItineraryId == null) return
    Card(modifier.padding(horizontal = 16.dp)) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Text(stringResource(R.string.map_loading_trips))
        }
    }
}

private data class SavedItinerarySearchModel(
    val state: SavedItinerariesState,
    val query: String,
    val selectedItineraryId: String?,
    val popularPlacesEnabled: Boolean,
    val onQueryChange: (String) -> Unit,
    val onPopularPlacesChange: (Boolean) -> Unit,
    val onItinerarySelected: (Itinerary) -> Unit,
)

private fun MapRouteUiState.searchModel(
    selectedItineraryId: String?,
    onItinerarySelected: (String?) -> Unit,
) = SavedItinerarySearchModel(
    state = itinerariesState,
    query = searchText,
    selectedItineraryId = selectedItineraryId,
    popularPlacesEnabled = popularPlacesEnabled,
    onQueryChange = { searchText = it },
    onPopularPlacesChange = { popularPlacesEnabled = it },
    onItinerarySelected = { itinerary ->
        selectedItinerary = itinerary
        onItinerarySelected(itinerary.id)
        searchText = ""
    },
)

@Composable
private fun SavedItinerarySearch(
    model: SavedItinerarySearchModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SavedItinerarySearchControls(model)
            SavedItinerarySearchResults(model)
        }
    }
}

@Composable
private fun SavedItinerarySearchControls(model: SavedItinerarySearchModel) {
    OutlinedTextField(
        value = model.query,
        onValueChange = model.onQueryChange,
        label = { Text(stringResource(R.string.map_search_itineraries)) },
        modifier = Modifier.fillMaxWidth().testTag("map_itinerary_search"),
        singleLine = true,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("map_popular_locations_toggle"),
    ) {
        Checkbox(
            checked = model.popularPlacesEnabled,
            onCheckedChange = model.onPopularPlacesChange,
        )
        Text(stringResource(R.string.map_show_popular))
    }
}

@Composable
private fun SavedItinerarySearchResults(model: SavedItinerarySearchModel) {
    when (val state = model.state) {
        SavedItinerariesState.Loading -> Text(
            stringResource(R.string.map_loading_saved_itineraries),
            style = MaterialTheme.typography.bodySmall,
        )
        is SavedItinerariesState.Failed -> Text(
            state.message,
            style = MaterialTheme.typography.bodySmall,
        )
        is SavedItinerariesState.Ready -> ReadySavedItineraries(model, state.itineraries)
    }
}

@Composable
private fun ReadySavedItineraries(
    model: SavedItinerarySearchModel,
    itineraries: List<Itinerary>,
) {
    val matchingItineraries = itineraries.filter {
        it.name.contains(model.query.trim(), ignoreCase = true)
    }
    when {
        model.query.isNotBlank() && matchingItineraries.isEmpty() ->
            Text(stringResource(R.string.map_no_itinerary_matches))
        model.query.isNotBlank() -> ItinerarySearchResults(matchingItineraries, model)
        else -> SelectedItineraryLabel(model.selectedItineraryId, itineraries)
    }
}

@Composable
private fun ItinerarySearchResults(
    itineraries: List<Itinerary>,
    model: SavedItinerarySearchModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
    ) {
        items(itineraries, key = { it.id }) { itinerary ->
            androidx.compose.material3.TextButton(
                onClick = { model.onItinerarySelected(itinerary) },
                modifier = Modifier.fillMaxWidth().testTag("map_itinerary_${itinerary.id}"),
            ) {
                Text(itinerary.name, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SelectedItineraryLabel(
    selectedItineraryId: String?,
    itineraries: List<Itinerary>,
) {
    if (selectedItineraryId == null) return
    Text(
        text = itineraries.firstOrNull { it.id == selectedItineraryId }?.name.orEmpty(),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun MapMessage(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(modifier.padding(horizontal = 16.dp)) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(message)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}
