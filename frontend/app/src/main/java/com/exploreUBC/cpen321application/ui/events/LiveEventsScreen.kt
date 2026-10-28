package com.exploreUBC.cpen321application.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.data.events.Event
import com.exploreUBC.cpen321application.data.events.EventsRepository
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateItineraryInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONException

private const val EVENTS_PAGE_SIZE = 20

@Composable
fun LiveEventsScreen(
    userId: String?,
    eventsRepository: EventsRepository,
    itinerariesRepository: ItinerariesRepository,
    modifier: Modifier = Modifier,
) {
    val state = remember { LiveEventsScreenState() }
    LoadEventsEffect(userId, eventsRepository, state)
    LoadItinerariesEffect(userId, itinerariesRepository, state)
    LiveEventsContent(state, modifier)
    EventItineraryDialog(userId, itinerariesRepository, state)
}

private class LiveEventsScreenState {
    var events by mutableStateOf<List<Event>>(emptyList())
    var isLoading by mutableStateOf(true)
    var isLoadingMore by mutableStateOf(false)
    var hasMoreEvents by mutableStateOf(true)
    var errorMessage by mutableStateOf<String?>(null)
    var currentPage by mutableStateOf(1)
    var requestCount by mutableStateOf(0)
    var eventToAdd by mutableStateOf<Event?>(null)
    var itineraries by mutableStateOf<List<Itinerary>>(emptyList())
    var isLoadingItineraries by mutableStateOf(false)
    var isAddingToItinerary by mutableStateOf(false)
    var itineraryError by mutableStateOf<String?>(null)
    var itineraryStatus by mutableStateOf<String?>(null)

    fun selectEvent(event: Event) {
        itineraryStatus = null
        eventToAdd = event
    }

    fun loadMore() {
        if (errorMessage == null) currentPage++ else requestCount++
    }

    fun refresh() {
        events = emptyList()
        hasMoreEvents = true
        if (currentPage == 1) requestCount++ else currentPage = 1
    }
}

@Composable
private fun LoadEventsEffect(
    userId: String?,
    repository: EventsRepository,
    state: LiveEventsScreenState,
) {
    LaunchedEffect(userId, repository, state.currentPage, state.requestCount) {
        val isFirstPage = state.currentPage == 1
        state.isLoading = isFirstPage
        state.isLoadingMore = !isFirstPage
        state.errorMessage = null
        if (userId.isNullOrBlank()) {
            state.errorMessage = "User is unavailable"
            state.isLoading = false
            state.isLoadingMore = false
        } else {
            try {
                val newEvents = repository.getEvents(
                    userId,
                    49.2606,
                    -123.2460,
                    state.currentPage,
                    EVENTS_PAGE_SIZE,
                )
                state.events = if (isFirstPage) {
                    newEvents
                } else {
                    (state.events + newEvents).distinctBy { it.id }
                }
                state.hasMoreEvents = newEvents.size == EVENTS_PAGE_SIZE
            } catch (error: IOException) {
                state.errorMessage = error.message ?: "Unable to load events"
            } catch (error: JSONException) {
                state.errorMessage = error.message ?: "Unable to load events"
            } finally {
                state.isLoading = false
                state.isLoadingMore = false
            }
        }
    }
}

@Composable
private fun LoadItinerariesEffect(
    userId: String?,
    repository: ItinerariesRepository,
    state: LiveEventsScreenState,
) {
    LaunchedEffect(state.eventToAdd, userId, repository) {
        if (state.eventToAdd != null && !userId.isNullOrBlank()) {
            state.isLoadingItineraries = true
            state.itineraryError = null
            runCatching { repository.getItineraries(userId, saved = true) }
                .onSuccess { state.itineraries = it }
                .onFailure {
                    state.itineraryError = it.message ?: "Unable to load itineraries"
                }
            state.isLoadingItineraries = false
        }
    }
}

@Composable
private fun LiveEventsContent(state: LiveEventsScreenState, modifier: Modifier) {
    when {
        state.isLoading -> Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        state.errorMessage != null && state.events.isEmpty() -> MessageWithButton(
            message = state.errorMessage ?: "Unable to load events",
            buttonText = "Retry",
            onClick = { state.requestCount++ },
            modifier = modifier,
        )
        state.events.isEmpty() -> MessageWithButton(
            message = "No events found",
            buttonText = "Refresh",
            onClick = { state.requestCount++ },
            modifier = modifier,
        )
        else -> EventList(
            state = EventListState(
                events = state.events,
                hasMoreEvents = state.hasMoreEvents,
                isLoadingMore = state.isLoadingMore,
                loadMoreError = state.errorMessage,
                itineraryStatus = state.itineraryStatus,
            ),
            onAddToItinerary = state::selectEvent,
            onLoadMore = state::loadMore,
            onRefresh = state::refresh,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventItineraryDialog(
    userId: String?,
    repository: ItinerariesRepository,
    state: LiveEventsScreenState,
) {
    val selectedEvent = state.eventToAdd ?: return
    val scope = rememberCoroutineScope()
    AddEventToItineraryDialog(
        state = AddToItineraryDialogState(
            selectedEvent,
            state.itineraries,
            state.isLoadingItineraries,
            state.isAddingToItinerary,
            state.itineraryError,
        ),
        onDismiss = { state.eventToAdd = null },
        onSelect = { itinerary ->
            if (userId.isNullOrBlank()) {
                state.itineraryError = "User is unavailable"
            } else {
                scope.launch {
                    addEventToItinerary(repository, userId, itinerary, selectedEvent, state)
                }
            }
        },
        onCreate = { name ->
            if (userId.isNullOrBlank()) {
                state.itineraryError = "User is unavailable"
            } else {
                scope.launch {
                    createItineraryWithEvent(repository, userId, name, selectedEvent, state)
                }
            }
        },
    )
}

private suspend fun addEventToItinerary(
    repository: ItinerariesRepository,
    userId: String,
    itinerary: Itinerary,
    event: Event,
    state: LiveEventsScreenState,
) {
    state.isAddingToItinerary = true
    state.itineraryError = null
    runCatching {
        repository.addActivity(userId, itinerary.id, event.toTwoHourActivityInput())
    }.onSuccess {
        state.itineraryStatus = "Added to ${itinerary.name}."
        state.eventToAdd = null
    }.onFailure {
        state.itineraryError = it.message ?: "Unable to add event"
    }
    state.isAddingToItinerary = false
}

private suspend fun createItineraryWithEvent(
    repository: ItinerariesRepository,
    userId: String,
    itineraryName: String,
    event: Event,
    state: LiveEventsScreenState,
) {
    state.isAddingToItinerary = true
    state.itineraryError = null
    runCatching {
        val itinerary = repository.createItinerary(
            userId,
            CreateItineraryInput(name = itineraryName),
        )
        repository.addActivity(userId, itinerary.id, event.toTwoHourActivityInput())
        itinerary
    }.onSuccess {
        state.itineraryStatus = "Created ${it.name} and added the event."
        state.eventToAdd = null
    }.onFailure {
        state.itineraryError = it.message ?: "Unable to create itinerary"
    }
    state.isAddingToItinerary = false
}

@Composable
private fun MessageWithButton(
    message: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message)
        Button(onClick = onClick, modifier = Modifier.padding(top = 12.dp)) {
            Text(buttonText)
        }
    }
}

private data class EventListState(
    val events: List<Event>,
    val hasMoreEvents: Boolean,
    val isLoadingMore: Boolean,
    val loadMoreError: String?,
    val itineraryStatus: String?,
)

@Composable
private fun EventList(
    state: EventListState,
    onAddToItinerary: (Event) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Events", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }
        if (state.itineraryStatus != null) {
            item {
                Text(state.itineraryStatus, color = MaterialTheme.colorScheme.primary)
            }
        }
        items(state.events, key = { it.id }) { event ->
            EventCard(event, onAddToItinerary)
        }
        if (state.loadMoreError != null) {
            item {
                Text(
                    text = state.loadMoreError,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (state.hasMoreEvents) {
            item {
                Button(
                    onClick = onLoadMore,
                    enabled = !state.isLoadingMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            state.isLoadingMore -> "Loading..."
                            state.loadMoreError != null -> "Retry loading"
                            else -> "Load more"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    onAddToItinerary: (Event) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(event.category, style = MaterialTheme.typography.labelLarge)
            Text(
                text = event.description ?: "No description available.",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(formatDate(event.startDateTime))
            Text("${event.venueName}, ${event.city}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatPrice(event))
                Text(String.format(Locale.CANADA, "%.1f km away", event.distanceKm))
            }
            Button(
                onClick = { uriHandler.openUri(event.ticketUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View event details")
            }
            OutlinedButton(
                onClick = { onAddToItinerary(event) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to itinerary")
            }
        }
    }
}

private data class AddToItineraryDialogState(
    val event: Event,
    val itineraries: List<Itinerary>,
    val isLoading: Boolean,
    val isWorking: Boolean,
    val errorMessage: String?,
)

@Composable
private fun AddEventToItineraryDialog(
    state: AddToItineraryDialogState,
    onDismiss: () -> Unit,
    onSelect: (Itinerary) -> Unit,
    onCreate: (String) -> Unit,
) {
    var itineraryName by remember(state.event.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${state.event.title}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    state.itineraries.isEmpty() -> {
                        Text("You do not have an itinerary yet.")
                        OutlinedTextField(
                            value = itineraryName,
                            onValueChange = { itineraryName = it },
                            label = { Text("Itinerary name") },
                            enabled = !state.isWorking,
                        )
                        Button(
                            onClick = { onCreate(itineraryName.trim()) },
                            enabled = itineraryName.isNotBlank() && !state.isWorking,
                        ) {
                            Text(if (state.isWorking) "Creating..." else "Create and add")
                        }
                    }
                    else -> state.itineraries.forEach { itinerary ->
                        OutlinedButton(
                            onClick = { onSelect(itinerary) },
                            enabled = !state.isWorking,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(itinerary.name)
                        }
                    }
                }
                if (state.errorMessage != null) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isWorking) {
                Text("Cancel")
            }
        },
    )
}

private fun Event.toTwoHourActivityInput(): CreateActivityInput {
    val endTime = Instant.parse(startDateTime).plusSeconds(2 * 60 * 60).toString()
    return CreateActivityInput(
        name = title,
        image = imageUrl,
        description = description,
        category = category,
        location = "$venueName, $city",
        startTime = startDateTime,
        endTime = endTime,
    )
}

private fun formatDate(value: String): String = try {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(formatter)
} catch (_: Exception) {
    value
}

private fun formatPrice(event: Event): String {
    val currency = event.currency ?: ""
    return when {
        event.priceMin != null && event.priceMax != null ->
            "$currency ${formatAmount(event.priceMin)} - ${formatAmount(event.priceMax)}"
        event.priceMin != null -> "$currency ${formatAmount(event.priceMin)}"
        else -> "Price unavailable"
    }
}

private fun formatAmount(amount: Double): String =
    String.format(Locale.CANADA, "%.2f", amount)
