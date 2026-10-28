package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryDetails
import com.exploreUBC.cpen321application.data.itineraries.ItineraryItem
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

    TripsRouteContent(state, modifier)
    if (state.showGenerateConfirmation) GenerateRecommendationsDialog(state)
}

@Composable
private fun TripsRouteContent(state: TripsRouteState, modifier: Modifier) {
    val itinerary = state.selectedItinerary
    val item = state.selectedItem
    val itemType = state.newItemType
    val listStatus = if (state.hasUser) state.statusMessage else MISSING_USER_MESSAGE
    if (itinerary == null) {
        TripsList(
            model = TripsListModel(
                savedItineraries = state.savedItineraries,
                recommendedItineraries = state.recommendedItineraries,
                isLoading = state.isLoading,
                isGenerating = state.isGenerating,
                statusMessage = listStatus,
            ),
            actions = TripsListActions(
                state.openItinerary,
                state.showGenerateConfirmationAction,
            ),
            modifier = modifier,
        )
    } else if (item != null) {
        ItineraryItemScreen(
            model = ItineraryItemScreenModel(
                item, itinerary.inRecommendation, state.isSaving, state.statusMessage,
            ),
            actions = ItineraryItemScreenActions(
                state.closeItem,
                state.updateActivity,
                state.updateTransportation,
                state.deleteItem,
            ),
            modifier = modifier,
        )
    } else if (itemType != null) {
        NewItineraryItemScreen(
            itemType = itemType,
            isSaving = state.isSaving,
            statusMessage = state.statusMessage,
            onBack = state.closeNewItem,
            onAddActivity = state.addActivity,
            onAddTransportation = state.addTransportation,
            modifier = modifier,
        )
    } else {
        ItineraryScreen(
            model = ItineraryScreenModel(itinerary, state.isSaving, state.statusMessage),
            actions = ItineraryScreenActions(
                onBack = state.closeItinerary,
                onItemClick = state.openItem,
                onAddActivity = { state.openNewItem(NewItemType.ACTIVITY) },
                onAddTransportation = { state.openNewItem(NewItemType.TRANSPORTATION) },
                onSaveDetails = state.saveDetails,
                onSaveRecommendation = state.saveRecommendation,
            ),
            modifier = modifier,
        )
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
    val isGenerating: Boolean,
    val statusMessage: String?,
)

data class TripsListActions(
    val onItineraryClick: (Itinerary) -> Unit,
    val onGenerateRecommendations: () -> Unit,
)

private data class ItinerarySectionModel(
    val title: String,
    val emptyMessage: String,
    val itineraries: List<Itinerary>,
    val tagPrefix: String,
    val showGenerateAction: Boolean = false,
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
    item { ItinerarySectionHeader(model, actions.onGenerateRecommendations) }
    if (model.itineraries.isEmpty()) {
        item { Text(model.emptyMessage) }
    } else {
        items(model.itineraries, key = { "${model.tagPrefix}-${it.id}" }) { itinerary ->
            ItineraryCard(itinerary, model.tagPrefix, actions.onItineraryClick)
        }
    }
}

@Composable
private fun ItinerarySectionHeader(model: ItinerarySectionModel, onGenerate: () -> Unit) {
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
        if (model.showGenerateAction) {
            Button(
                onClick = onGenerate,
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
)

data class ItineraryScreenActions(
    val onBack: () -> Unit,
    val onItemClick: (ItineraryItem) -> Unit,
    val onAddActivity: () -> Unit,
    val onAddTransportation: () -> Unit,
    val onSaveDetails: (ItineraryDetails) -> Unit,
    val onSaveRecommendation: () -> Unit,
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
        model.statusMessage?.let { Text(it, modifier = Modifier.testTag("itinerary_status")) }
        ItineraryItems(itinerary, actions.onItemClick)
        ItineraryActions(model, actions)
        Spacer(Modifier.height(16.dp))
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
    FormField(editor.name, { editor.name = it }, "Name", "itinerary_name")
    FormField(
        editor.description,
        { editor.description = it },
        "Description",
        "itinerary_description",
    )
    FormField(
        editor.startingLocation,
        { editor.startingLocation = it },
        "Starting location",
        "itinerary_starting_location",
    )
    Button(
        onClick = { onSave(editor.details()) },
        enabled = !isSaving && editor.name.isNotBlank(),
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
private fun ItineraryItems(itinerary: Itinerary, onItemClick: (ItineraryItem) -> Unit) {
    Text(
        text = "Activities and transportation",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (itinerary.items.isEmpty()) Text("No activities or transportation.")
    itinerary.items.forEach { item ->
        ItineraryItemCard(item = item, onClick = { onItemClick(item) })
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
