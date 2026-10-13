package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryDetails
import com.exploreUBC.cpen321application.data.itineraries.ItineraryItem
import com.exploreUBC.cpen321application.data.itineraries.TransportationItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun TripsRoute(
    userId: String?,
    itinerariesRepository: ItinerariesRepository,
    modifier: Modifier = Modifier,
) {
    var savedItineraries by remember(userId) { mutableStateOf<List<Itinerary>>(emptyList()) }
    var recommendedItineraries by remember(userId) { mutableStateOf<List<Itinerary>>(emptyList()) }
    var selectedItinerary by remember(userId) { mutableStateOf<Itinerary?>(null) }
    var isLoading by remember(userId) { mutableStateOf(false) }
    var isSaving by remember(userId) { mutableStateOf(false) }
    var statusMessage by remember(userId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, itinerariesRepository) {
        if (userId.isNullOrBlank()) return@LaunchedEffect
        isLoading = true
        statusMessage = null
        runCatching {
            coroutineScope {
                val saved = async {
                    itinerariesRepository.getItineraries(userId, saved = true)
                }
                val recommended = async {
                    itinerariesRepository.getItineraries(
                        userId,
                        inRecommendation = true,
                    )
                }
                saved.await() to recommended.await()
            }
        }.onSuccess { (saved, recommended) ->
            savedItineraries = saved
            recommendedItineraries = recommended
        }.onFailure {
            statusMessage = it.message ?: "Could not load itineraries."
        }
        isLoading = false
    }

    val itinerary = selectedItinerary
    if (itinerary == null) {
        TripsList(
            savedItineraries = savedItineraries,
            recommendedItineraries = recommendedItineraries,
            isLoading = isLoading,
            statusMessage = if (userId.isNullOrBlank()) {
                "A user ID is required to load itineraries."
            } else {
                statusMessage
            },
            onItineraryClick = { selected ->
                if (userId.isNullOrBlank()) return@TripsList
                scope.launch {
                    isLoading = true
                    statusMessage = null
                    runCatching {
                        itinerariesRepository.getItinerary(userId, selected.id)
                    }.onSuccess {
                        selectedItinerary = it
                    }.onFailure {
                        statusMessage = it.message ?: "Could not load the itinerary."
                    }
                    isLoading = false
                }
            },
            modifier = modifier,
        )
    } else {
        ItineraryScreen(
            itinerary = itinerary,
            isSaving = isSaving,
            statusMessage = statusMessage,
            onBack = {
                selectedItinerary = null
                statusMessage = null
            },
            onSave = { details ->
                if (userId.isNullOrBlank()) return@ItineraryScreen
                scope.launch {
                    isSaving = true
                    statusMessage = null
                    runCatching {
                        itinerariesRepository.updateItineraryDetails(
                            userId,
                            itinerary.id,
                            details,
                        )
                    }.onSuccess { updated ->
                        selectedItinerary = updated
                        savedItineraries = savedItineraries.replace(updated)
                        recommendedItineraries = recommendedItineraries.replace(updated)
                        statusMessage = "Itinerary details saved."
                    }.onFailure {
                        statusMessage = it.message ?: "Could not save itinerary details."
                    }
                    isSaving = false
                }
            },
            modifier = modifier,
        )
    }
}

private fun List<Itinerary>.replace(updated: Itinerary): List<Itinerary> =
    map { if (it.id == updated.id) updated else it }

@Composable
fun TripsList(
    savedItineraries: List<Itinerary>,
    recommendedItineraries: List<Itinerary>,
    isLoading: Boolean,
    statusMessage: String?,
    onItineraryClick: (Itinerary) -> Unit,
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
        if (isLoading) {
            item { CircularProgressIndicator(modifier = Modifier.testTag("trips_loading")) }
        }
        statusMessage?.let { message ->
            item { Text(message, modifier = Modifier.testTag("trips_status")) }
        }
        itinerarySection(
            title = "Saved Itineraries",
            emptyMessage = "No saved itineraries.",
            itineraries = savedItineraries,
            tagPrefix = "saved",
            onItineraryClick = onItineraryClick,
        )
        itinerarySection(
            title = "Recommended Itineraries",
            emptyMessage = "No recommended itineraries.",
            itineraries = recommendedItineraries,
            tagPrefix = "recommended",
            onItineraryClick = onItineraryClick,
        )
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itinerarySection(
    title: String,
    emptyMessage: String,
    itineraries: List<Itinerary>,
    tagPrefix: String,
    onItineraryClick: (Itinerary) -> Unit,
) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    if (itineraries.isEmpty()) {
        item { Text(emptyMessage) }
    } else {
        items(itineraries, key = { "$tagPrefix-${it.id}" }) { itinerary ->
            Card(
                onClick = { onItineraryClick(itinerary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_itinerary_${itinerary.id}"),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(itinerary.name, fontWeight = FontWeight.Bold)
                    itinerary.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    Text("${itinerary.items.size} items")
                }
            }
        }
    }
}

@Composable
fun ItineraryScreen(
    itinerary: Itinerary,
    isSaving: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onSave: (ItineraryDetails) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(itinerary.id, itinerary.updatedAt) { mutableStateOf(itinerary.name) }
    var description by remember(itinerary.id, itinerary.updatedAt) {
        mutableStateOf(itinerary.description.orEmpty())
    }
    var startingLocation by remember(itinerary.id, itinerary.updatedAt) {
        mutableStateOf(itinerary.startingLocation.orEmpty())
    }
    var selectedItemId by remember(itinerary.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("itinerary_back")) {
            Text("Back to itineraries")
        }
        Text(
            text = "Itinerary details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("itinerary_name"),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().testTag("itinerary_description"),
        )
        OutlinedTextField(
            value = startingLocation,
            onValueChange = { startingLocation = it },
            label = { Text("Starting location") },
            modifier = Modifier.fillMaxWidth().testTag("itinerary_starting_location"),
        )
        Button(
            onClick = {
                onSave(
                    ItineraryDetails(
                        name = name,
                        description = description,
                        startingLocation = startingLocation,
                    ),
                )
            },
            enabled = !isSaving && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag("itinerary_save"),
        ) {
            Text(if (isSaving) "Saving…" else "Save details")
        }
        statusMessage?.let { Text(it, modifier = Modifier.testTag("itinerary_status")) }

        Text(
            text = "Activities and transportation",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (itinerary.items.isEmpty()) {
            Text("No activities or transportation.")
        }
        itinerary.items.forEach { item ->
            ItineraryItemCard(
                item = item,
                expanded = selectedItemId == item.id,
                onClick = {
                    selectedItemId = if (selectedItemId == item.id) null else item.id
                },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ItineraryItemCard(
    item: ItineraryItem,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("itinerary_item_${item.id}"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (item) {
                is ActivityItem -> {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text("Activity")
                    if (expanded) {
                        item.category?.let { Text("Category: $it") }
                        item.description?.let { Text(it) }
                        Text("Location: ${item.location}")
                        Text("Time: ${item.startTime} – ${item.endTime}")
                    }
                }
                is TransportationItem -> {
                    Text(item.mode.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                    Text("Transportation")
                    if (expanded) {
                        Text("From: ${item.startLocation}")
                        Text("To: ${item.endLocation}")
                        Text("Distance: ${item.distanceMeters.toInt()} m")
                        Text("Time: ${item.startTime} – ${item.endTime}")
                    }
                }
            }
        }
    }
}
