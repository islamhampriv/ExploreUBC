package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryDetails
import com.exploreUBC.cpen321application.data.itineraries.ItineraryHttpException
import com.exploreUBC.cpen321application.data.itineraries.ItineraryItem
import com.exploreUBC.cpen321application.data.itineraries.ItineraryMap
import com.exploreUBC.cpen321application.data.itineraries.MapRoute
import com.exploreUBC.cpen321application.ui.map.CampusMapView
import com.exploreUBC.cpen321application.ui.map.MapLayerMode
import com.exploreUBC.cpen321application.ui.map.TripTravelMode
import com.exploreUBC.cpen321application.ui.map.TripTravelOverlay
import com.exploreUBC.cpen321application.ui.map.routeKey
import java.util.Locale

private const val MISSING_USER_MESSAGE = "A user ID is required to load itineraries."

@Composable
fun TripsRoute(
    userId: String?,
    itinerariesRepository: ItinerariesRepository,
    modifier: Modifier = Modifier,
) {
    val state = rememberTripsRouteState(userId, itinerariesRepository)

    LaunchedEffect(state) {
        if (state.hasUser) {
            state.loadItineraries()
        }
    }

    TripsRouteContent(state, userId, itinerariesRepository, modifier)
    if (state.showGenerateConfirmation) GenerateRecommendationsDialog(state)
}

@Composable
private fun TripsRouteContent(
    state: TripsRouteState,
    userId: String?,
    itinerariesRepository: ItinerariesRepository,
    modifier: Modifier,
) {
    val itinerary = state.selectedItinerary
    if (itinerary == null) {
        TripsOverview(state, modifier)
    } else {
        SelectedItineraryRoute(state, userId, itinerariesRepository, itinerary, modifier)
    }
}

@Composable
private fun TripsOverview(state: TripsRouteState, modifier: Modifier) {
    TripsList(
        model = TripsListModel(
            savedItineraries = state.savedItineraries,
            recommendedItineraries = state.recommendedItineraries,
            isLoading = state.isLoading,
            isCreating = state.isCreating,
            isGenerating = state.isGenerating,
            statusMessage = if (state.hasUser) state.statusMessage else MISSING_USER_MESSAGE,
        ),
        actions = TripsListActions(
            state.openItinerary,
            state.createItinerary,
            state.showGenerateConfirmationAction,
        ),
        modifier = modifier,
    )
}

@Composable
private fun SelectedItineraryRoute(
    state: TripsRouteState,
    userId: String?,
    repository: ItinerariesRepository,
    itinerary: Itinerary,
    modifier: Modifier,
) {
    var showMap by remember(itinerary.id) { mutableStateOf(false) }
    var travelRoutes by remember(userId, itinerary.id) {
        mutableStateOf<List<MapRoute>>(emptyList())
    }
    val item = state.selectedItem
    val itemType = state.newItemType

    LaunchedEffect(userId, itinerary.id, itinerary.updatedAt) {
        travelRoutes = if (userId.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                repository.getItineraryMap(userId, itinerary.id).routes
            }.getOrDefault(emptyList())
        }
    }
    when {
        showMap -> ItineraryMapScreen(
            userId = userId.orEmpty(),
            itineraryId = itinerary.id,
            itineraryName = itinerary.name,
            itinerariesRepository = repository,
            onHideMap = { showMap = false },
            onItineraryMissing = {
                showMap = false
                state.closeItinerary()
            },
            modifier = modifier,
        )
        item != null -> ItineraryItemScreen(
            ItineraryItemScreenModel(
                item, itinerary.inRecommendation, state.isSaving, state.statusMessage,
            ),
            ItineraryItemScreenActions(
                state.closeItem,
                state.updateActivity,
                state.updateTransportation,
                state.deleteItem,
            ),
            modifier,
        )
        itemType != null -> NewItineraryItemScreen(
            itemType = itemType,
            isSaving = state.isSaving,
            statusMessage = state.statusMessage,
            onBack = state.closeNewItem,
            onAddActivity = state.addActivity,
            onAddTransportation = state.addTransportation,
            modifier = modifier,
        )
        else -> SelectedItineraryDetails(
            state,
            itinerary,
            travelRoutes,
            { showMap = true },
            modifier,
        )
    }
}

@Composable
private fun SelectedItineraryDetails(
    state: TripsRouteState,
    itinerary: Itinerary,
    travelRoutes: List<MapRoute>,
    onViewMap: () -> Unit,
    modifier: Modifier,
) {
    ItineraryScreen(
        model = ItineraryScreenModel(
            itinerary,
            state.isSaving,
            state.statusMessage,
            travelRoutes,
        ),
        actions = ItineraryScreenActions(
            onBack = state.closeItinerary,
            onItemClick = state.openItem,
            onAddActivity = { state.openNewItem(NewItemType.ACTIVITY) },
            onAddTransportation = { state.openNewItem(NewItemType.TRANSPORTATION) },
            onSaveDetails = state.saveDetails,
            onSaveRecommendation = state.saveRecommendation,
            onDelete = state.deleteItinerary,
            onViewMap = onViewMap,
        ),
        modifier = modifier,
    )
}

private sealed interface ItineraryMapUiState {
    data object Loading : ItineraryMapUiState
    data class Ready(val map: ItineraryMap) : ItineraryMapUiState
    data object NotFound : ItineraryMapUiState
    data class Failed(val message: String) : ItineraryMapUiState
}

@Composable
private fun ItineraryMapScreen(
    userId: String,
    itineraryId: String,
    itineraryName: String,
    itinerariesRepository: ItinerariesRepository,
    onHideMap: () -> Unit,
    onItineraryMissing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reloadToken by remember(userId, itineraryId) { mutableIntStateOf(0) }
    var state by remember(userId, itineraryId) {
        mutableStateOf<ItineraryMapUiState>(ItineraryMapUiState.Loading)
    }
    var travelMode by remember(userId, itineraryId) {
        mutableStateOf(TripTravelMode.DRIVING)
    }
    var selectedRouteKey by remember(userId, itineraryId) { mutableStateOf<String?>(null) }
    var overlayHeightPx by remember(userId, itineraryId) { mutableIntStateOf(0) }

    LaunchedEffect(userId, itineraryId, reloadToken) {
        state = ItineraryMapUiState.Loading
        runCatching {
            itinerariesRepository.getItineraryMap(userId, itineraryId)
        }.onSuccess { map ->
            state = ItineraryMapUiState.Ready(map)
            if (map.routes.none { it.routeKey() == selectedRouteKey }) {
                selectedRouteKey = map.routes.firstOrNull()?.routeKey()
            }
        }.onFailure { error ->
            state = if (error is ItineraryHttpException && error.statusCode == 404) {
                ItineraryMapUiState.NotFound
            } else {
                ItineraryMapUiState.Failed(
                    error.message ?: "Could not load the map.",
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ItineraryMapContent(
            state,
            itineraryName,
            travelMode,
            selectedRouteKey,
            overlayHeightPx,
            onItineraryMissing,
            onRetry = { reloadToken += 1 },
        )
        ItineraryMapChrome(
            state = state,
            travelMode = travelMode,
            selectedRouteKey = selectedRouteKey,
            onTravelModeChange = { travelMode = it },
            onRouteSelected = { selectedRouteKey = it },
            onHideMap = onHideMap,
            onOverlayHeightChanged = { overlayHeightPx = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ItineraryMapContent(
    state: ItineraryMapUiState,
    itineraryName: String,
    travelMode: TripTravelMode,
    selectedRouteKey: String?,
    controlsBottomInsetPx: Int,
    onItineraryMissing: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        ItineraryMapUiState.Loading -> CenteredMapContent {
            CircularProgressIndicator()
            Text(stringResource(R.string.map_loading_trips))
        }
        is ItineraryMapUiState.Ready -> CampusMapView(
            mode = MapLayerMode.TRIP_ROUTES,
            itineraryMap = state.map,
            overlayTitle = itineraryName,
            travelMode = travelMode,
            selectedRouteKey = selectedRouteKey,
            controlsBottomInsetPx = controlsBottomInsetPx,
            modifier = Modifier.fillMaxSize(),
        )
        ItineraryMapUiState.NotFound -> CenteredMapAction(
            stringResource(R.string.map_itinerary_missing),
            stringResource(R.string.map_back_to_trips),
            "itinerary_map_missing_back",
            onItineraryMissing,
        )
        is ItineraryMapUiState.Failed -> CenteredMapAction(
            state.message.ifBlank { stringResource(R.string.map_load_failed) },
            stringResource(R.string.map_retry),
            "itinerary_map_retry",
            onRetry,
        )
    }
}

@Composable
private fun ItineraryMapChrome(
    state: ItineraryMapUiState,
    travelMode: TripTravelMode,
    selectedRouteKey: String?,
    onTravelModeChange: (TripTravelMode) -> Unit,
    onRouteSelected: (String) -> Unit,
    onHideMap: () -> Unit,
    onOverlayHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measuredModifier = modifier.onSizeChanged { size ->
        onOverlayHeightChanged(size.height)
    }
    when (state) {
        is ItineraryMapUiState.Ready -> TripTravelOverlay(
            itineraryMap = state.map,
            travelMode = travelMode,
            selectedRouteKey = selectedRouteKey,
            onTravelModeChange = onTravelModeChange,
            onRouteSelected = onRouteSelected,
            onHideMap = onHideMap,
            modifier = measuredModifier,
        )
        ItineraryMapUiState.NotFound -> Unit
        else -> HideItineraryMapButton(24.dp, onHideMap, measuredModifier)
    }
}

@Composable
private fun CenteredMapContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        content()
    }
}

@Composable
private fun CenteredMapAction(
    message: String,
    actionLabel: String,
    actionTag: String,
    onClick: () -> Unit,
) {
    CenteredMapContent {
        Text(message)
        Button(onClick = onClick, modifier = Modifier.testTag(actionTag)) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun HideItineraryMapButton(
    bottomPadding: androidx.compose.ui.unit.Dp,
    onHideMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onHideMap,
        modifier = Modifier
            .then(modifier)
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = bottomPadding,
            )
            .testTag("itinerary_hide_map"),
    ) {
        Text(stringResource(R.string.map_hide))
    }
}

@Composable
private fun GenerateRecommendationsDialog(state: TripsRouteState) {
    AlertDialog(
        onDismissRequest = state.dismissGenerateConfirmation,
        title = { Text("Generate new recommendations?") },
        text = { Text("Your current recommended itineraries will be removed and replaced.") },
        confirmButton = {
            TextButton(
                onClick = state.generateRecommendations,
                modifier = Modifier.testTag("confirm_generate_recommendations"),
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(
                onClick = state.dismissGenerateConfirmation,
                modifier = Modifier.testTag("cancel_generate_recommendations"),
            ) { Text("Cancel") }
        },
    )
}

data class TripsListModel(
    val savedItineraries: List<Itinerary>,
    val recommendedItineraries: List<Itinerary>,
    val isLoading: Boolean,
    val isCreating: Boolean,
    val isGenerating: Boolean,
    val statusMessage: String?,
)

data class TripsListActions(
    val onItineraryClick: (Itinerary) -> Unit,
    val onCreateItinerary: () -> Unit,
    val onGenerateRecommendations: () -> Unit,
)

private data class ItinerarySectionModel(
    val title: String,
    val emptyMessage: String,
    val itineraries: List<Itinerary>,
    val tagPrefix: String,
    val showGenerateAction: Boolean = false,
    val showCreateAction: Boolean = false,
    val isCreating: Boolean = false,
    val isGenerating: Boolean = false,
)

@Composable
fun TripsList(
    model: TripsListModel,
    actions: TripsListActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Trips",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        if (model.isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.testTag("trips_loading")) }
        }
        model.statusMessage?.let { message ->
            item { Text(message, modifier = Modifier.testTag("trips_status")) }
        }
        itinerarySection(
            model = ItinerarySectionModel(
                title = "Saved Itineraries",
                emptyMessage = "No saved itineraries.",
                itineraries = model.savedItineraries,
                tagPrefix = "saved",
                showCreateAction = true,
                isCreating = model.isCreating,
            ),
            actions = actions,
        )
        itinerarySection(
            model = ItinerarySectionModel(
                title = "Recommended Itineraries",
                emptyMessage = "No recommended itineraries.",
                itineraries = model.recommendedItineraries,
                tagPrefix = "recommended",
                showGenerateAction = true,
                isGenerating = model.isGenerating,
            ),
            actions = actions,
        )
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itinerarySection(
    model: ItinerarySectionModel,
    actions: TripsListActions,
) {
    item { ItinerarySectionHeader(model, actions) }
    if (model.itineraries.isEmpty()) {
        item { Text(model.emptyMessage) }
    } else {
        items(model.itineraries, key = { "${model.tagPrefix}-${it.id}" }) { itinerary ->
            ItineraryCard(itinerary, model.tagPrefix, actions.onItineraryClick)
        }
    }
}

@Composable
private fun ItinerarySectionHeader(model: ItinerarySectionModel, actions: TripsListActions) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (model.showCreateAction) {
            Button(
                onClick = actions.onCreateItinerary,
                enabled = !model.isCreating,
                modifier = Modifier.testTag("create_itinerary"),
            ) {
                if (model.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp).height(18.dp)
                            .testTag("create_itinerary_loading"),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Create")
                }
            }
        }
        if (model.showGenerateAction) {
            Button(
                onClick = actions.onGenerateRecommendations,
                enabled = !model.isGenerating,
                modifier = Modifier.testTag("generate_recommendations"),
            ) {
                if (model.isGenerating) GenerateProgress() else Text("Generate")
            }
        }
    }
}

@Composable
private fun GenerateProgress() {
    CircularProgressIndicator(
        modifier = Modifier.width(18.dp).height(18.dp)
            .testTag("generate_recommendations_loading"),
        strokeWidth = 2.dp,
    )
    Spacer(Modifier.width(8.dp))
    Text("Generating…")
}

@Composable
private fun ItineraryCard(
    itinerary: Itinerary,
    tagPrefix: String,
    onClick: (Itinerary) -> Unit,
) {
    Card(
        onClick = { onClick(itinerary) },
        modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_itinerary_${itinerary.id}"),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(itinerary.name, fontWeight = FontWeight.Bold)
            itinerary.description?.takeIf(String::isNotBlank)?.let { Text(it) }
            ItineraryCardFooter(itinerary)
        }
    }
}

@Composable
private fun ItineraryCardFooter(itinerary: Itinerary) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(text = "${itinerary.items.size} items", modifier = Modifier.weight(1f))
        val score = itinerary.score["interestSimilarityOutOfFive"]
        if (itinerary.inRecommendation && score != null) {
            Text(
                text = String.format(Locale.US, "%.1f/5", score),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("recommended_itinerary_score_${itinerary.id}"),
            )
        }
    }
}

data class ItineraryScreenModel(
    val itinerary: Itinerary,
    val isSaving: Boolean,
    val statusMessage: String?,
    val travelRoutes: List<MapRoute> = emptyList(),
)

data class ItineraryScreenActions(
    val onBack: () -> Unit,
    val onItemClick: (ItineraryItem) -> Unit,
    val onAddActivity: () -> Unit,
    val onAddTransportation: () -> Unit,
    val onSaveDetails: (ItineraryDetails) -> Unit,
    val onSaveRecommendation: () -> Unit,
    val onDelete: () -> Unit,
    val onViewMap: () -> Unit = {},
)

private class ItineraryEditorState(itinerary: Itinerary) {
    var name by mutableStateOf(itinerary.name)
    var description by mutableStateOf(itinerary.description.orEmpty())
    var startingLocation by mutableStateOf(itinerary.startingLocation.orEmpty())
    var isEditing by mutableStateOf(false)

    fun toggle(itinerary: Itinerary) {
        if (isEditing) {
            name = itinerary.name
            description = itinerary.description.orEmpty()
            startingLocation = itinerary.startingLocation.orEmpty()
        }
        isEditing = !isEditing
    }

    fun details() = ItineraryDetails(name, description, startingLocation)
}

@Composable
fun ItineraryScreen(
    model: ItineraryScreenModel,
    actions: ItineraryScreenActions,
    modifier: Modifier = Modifier,
) {
    val itinerary = model.itinerary
    var showDeleteConfirmation by remember(itinerary.id) { mutableStateOf(false) }
    val editor = remember(itinerary.id, itinerary.updatedAt) {
        ItineraryEditorState(itinerary)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = actions.onBack, modifier = Modifier.testTag("itinerary_back")) {
            Text("Back to itineraries")
        }
        ItineraryDetailsHeader(model, editor)
        if (editor.isEditing) {
            ItineraryDetailsEditor(editor, model.isSaving, actions.onSaveDetails)
        } else {
            ItineraryDetailsContent(itinerary)
        }
        Button(
            onClick = actions.onViewMap,
            modifier = Modifier.fillMaxWidth().testTag("itinerary_view_on_map"),
        ) {
            Text("View on Map")
        }
        model.statusMessage?.let { Text(it, modifier = Modifier.testTag("itinerary_status")) }
        ItineraryItems(itinerary, model.travelRoutes, actions.onItemClick)
        ItineraryActions(model, actions)
        Button(
            onClick = { showDeleteConfirmation = true },
            enabled = !model.isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.fillMaxWidth().testTag("itinerary_delete"),
        ) { Text("Delete itinerary") }
        Spacer(Modifier.height(16.dp))
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete itinerary?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        actions.onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_itinerary"),
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.testTag("cancel_delete_itinerary"),
                ) { Text("No") }
            },
        )
    }
}

@Composable
private fun ItineraryDetailsHeader(model: ItineraryScreenModel, editor: ItineraryEditorState) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Itinerary details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (!model.itinerary.inRecommendation) {
            TextButton(
                onClick = { editor.toggle(model.itinerary) },
                enabled = !model.isSaving,
                modifier = Modifier.testTag("itinerary_details_edit"),
            ) { Text(if (editor.isEditing) "Cancel" else "Edit") }
        }
    }
}

@Composable
private fun ItineraryDetailsEditor(
    editor: ItineraryEditorState,
    isSaving: Boolean,
    onSave: (ItineraryDetails) -> Unit,
) {
    val isValid = editor.name.isNotBlank() &&
        editor.name.length <= ITINERARY_NAME_MAX_LENGTH &&
        editor.description.length <= DESCRIPTION_MAX_LENGTH &&
        editor.startingLocation.length <= LOCATION_MAX_LENGTH
    FormField(
        editor.name, { editor.name = it }, "Name", "itinerary_name",
        maxLength = ITINERARY_NAME_MAX_LENGTH,
    )
    FormField(
        editor.description,
        { editor.description = it },
        "Description",
        "itinerary_description",
        maxLength = DESCRIPTION_MAX_LENGTH,
    )
    AddressAutocompleteField(
        value = editor.startingLocation,
        onValueChange = { editor.startingLocation = it },
        label = "Starting location",
        tag = "itinerary_starting_location",
        options = AddressAutocompleteOptions(
            maxLength = LOCATION_MAX_LENGTH,
            placeholder = "e.g. 6138 Student Union Blvd, Vancouver",
            supportingText = "Pick a recommended address to use as the itinerary's starting pin.",
        ),
    )
    Button(
        onClick = { onSave(editor.details()) },
        enabled = !isSaving && isValid,
        modifier = Modifier.fillMaxWidth().testTag("itinerary_save"),
    ) { Text(if (isSaving) "Saving…" else "Save details") }
}

@Composable
private fun ItineraryDetailsContent(itinerary: Itinerary) {
    DetailField("Name", itinerary.name)
    DetailField("Description", itinerary.description?.takeIf(String::isNotBlank) ?: "Not set")
    DetailField(
        "Starting location",
        itinerary.startingLocation?.takeIf(String::isNotBlank) ?: "Not set",
    )
}

@Composable
private fun ItineraryItems(
    itinerary: Itinerary,
    travelRoutes: List<MapRoute>,
    onItemClick: (ItineraryItem) -> Unit,
) {
    Text(
        text = "Activities and transportation",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (itinerary.items.isEmpty()) Text("No activities or transportation.")
    itinerary.items.forEachIndexed { index, item ->
        ItineraryItemCard(item = item, onClick = { onItemClick(item) })
        val nextActivity = itinerary.items
            .drop(index + 1)
            .filterIsInstance<ActivityItem>()
            .firstOrNull()
        val route = travelRoutes.find { candidate ->
            candidate.fromActivityId == item.id && candidate.toActivityId == nextActivity?.id
        }
        if (
            item is ActivityItem &&
            route != null &&
            nextActivity != null
        ) {
            TravelLegCard(route, item.name, nextActivity.name)
        }
    }
}

@Composable
private fun ItineraryActions(model: ItineraryScreenModel, actions: ItineraryScreenActions) {
    if (model.itinerary.inRecommendation) {
        Button(
            onClick = actions.onSaveRecommendation,
            enabled = !model.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("itinerary_save_recommendation"),
        ) { Text(if (model.isSaving) "Saving…" else "Save itinerary") }
    } else {
        Row(modifier = Modifier.fillMaxWidth()) {
            AddItemButton("+ Activity", "itinerary_add_activity", model, actions.onAddActivity)
            Spacer(Modifier.width(12.dp))
            AddItemButton(
                "+ Transportation",
                "itinerary_add_transportation",
                model,
                actions.onAddTransportation,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AddItemButton(
    label: String,
    tag: String,
    model: ItineraryScreenModel,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = model.itinerary.saved,
        modifier = Modifier.weight(1f).testTag(tag),
    ) { Text(label) }
}
