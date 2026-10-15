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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private const val EVENTS_PAGE_SIZE = 20

@Composable
fun LiveEventsScreen(
    userId: String?,
    eventsRepository: EventsRepository,
    itinerariesRepository: ItinerariesRepository,
    modifier: Modifier = Modifier,
) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMoreEvents by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var requestCount by remember { mutableStateOf(0) }
    var eventToAdd by remember { mutableStateOf<Event?>(null) }
    var itineraries by remember { mutableStateOf<List<Itinerary>>(emptyList()) }
    var isLoadingItineraries by remember { mutableStateOf(false) }
    var isAddingToItinerary by remember { mutableStateOf(false) }
    var itineraryError by remember { mutableStateOf<String?>(null) }
    var itineraryStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, eventsRepository, currentPage, requestCount) {
        val isFirstPage = currentPage == 1
        isLoading = isFirstPage
        isLoadingMore = !isFirstPage
        errorMessage = null
        if (userId.isNullOrBlank()) {
            errorMessage = "User is unavailable"
            isLoading = false
            isLoadingMore = false
            return@LaunchedEffect
        }
        try {
            val newEvents = eventsRepository.getEvents(
                userId = userId,
                latitude = 49.2606,
                longitude = -123.2460,
                page = currentPage,
                limit = EVENTS_PAGE_SIZE,
            )
            events = if (isFirstPage) {
                newEvents
            } else {
                (events + newEvents).distinctBy { it.id }
            }
            hasMoreEvents = newEvents.size == EVENTS_PAGE_SIZE
        } catch (error: Exception) {
            errorMessage = error.message ?: "Unable to load events"
        } finally {
            isLoading = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(eventToAdd, userId, itinerariesRepository) {
        if (eventToAdd == null || userId.isNullOrBlank()) {
            return@LaunchedEffect
        }
        isLoadingItineraries = true
        itineraryError = null
        runCatching {
            itinerariesRepository.getItineraries(userId, saved = true)
        }.onSuccess {
            itineraries = it
        }.onFailure {
            itineraryError = it.message ?: "Unable to load itineraries"
        }
        isLoadingItineraries = false
    }

    when {
        isLoading -> Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        errorMessage != null && events.isEmpty() -> MessageWithButton(
            message = errorMessage ?: "Unable to load events",
            buttonText = "Retry",
            onClick = { requestCount++ },
            modifier = modifier
        )
        events.isEmpty() -> MessageWithButton(
            message = "No events found",
            buttonText = "Refresh",
            onClick = { requestCount++ },
            modifier = modifier
        )
        else -> EventList(
            events = events,
            hasMoreEvents = hasMoreEvents,
            isLoadingMore = isLoadingMore,
            loadMoreError = errorMessage,
            itineraryStatus = itineraryStatus,
            onAddToItinerary = {
                itineraryStatus = null
                eventToAdd = it
            },
            onLoadMore = {
                if (errorMessage == null) {
                    currentPage++
                } else {
                    requestCount++
                }
            },
            onRefresh = {
                events = emptyList()
                hasMoreEvents = true
                if (currentPage == 1) {
                    requestCount++
                } else {
                    currentPage = 1
                }
            },
            modifier = modifier
        )
    }

    val selectedEvent = eventToAdd
    if (selectedEvent != null) {
        AddToItineraryDialog(
            event = selectedEvent,
            itineraries = itineraries,
            isLoading = isLoadingItineraries,
            isWorking = isAddingToItinerary,
            errorMessage = itineraryError,
            onDismiss = { eventToAdd = null },
            onSelect = { itinerary ->
                val currentUserId = userId ?: return@AddToItineraryDialog
                scope.launch {
                    isAddingToItinerary = true
                    itineraryError = null
                    runCatching {
                        itinerariesRepository.addActivity(
                            currentUserId,
                            itinerary.id,
                            selectedEvent.toActivityInput(),
                        )
                    }.onSuccess {
                        itineraryStatus = "Added to ${itinerary.name}."
                        eventToAdd = null
                    }.onFailure {
                        itineraryError = it.message ?: "Unable to add event"
                    }
                    isAddingToItinerary = false
                }
            },
            onCreate = { itineraryName ->
                val currentUserId = userId ?: return@AddToItineraryDialog
                scope.launch {
                    isAddingToItinerary = true
                    itineraryError = null
                    runCatching {
                        val itinerary = itinerariesRepository.createItinerary(
                            currentUserId,
                            CreateItineraryInput(name = itineraryName),
                        )
                        itinerariesRepository.addActivity(
                            currentUserId,
                            itinerary.id,
                            selectedEvent.toActivityInput(),
                        )
                        itinerary
                    }.onSuccess {
                        itineraryStatus = "Created ${it.name} and added the event."
                        eventToAdd = null
                    }.onFailure {
                        itineraryError = it.message ?: "Unable to create itinerary"
                    }
                    isAddingToItinerary = false
                }
            },
        )
    }
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

@Composable
private fun EventList(
    events: List<Event>,
    hasMoreEvents: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    itineraryStatus: String?,
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
        if (itineraryStatus != null) {
            item {
                Text(itineraryStatus, color = MaterialTheme.colorScheme.primary)
            }
        }
        items(events, key = { it.id }) { event ->
            EventCard(event, onAddToItinerary)
        }
        if (loadMoreError != null) {
            item {
                Text(
                    text = loadMoreError,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (hasMoreEvents) {
            item {
                Button(
                    onClick = onLoadMore,
                    enabled = !isLoadingMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            isLoadingMore -> "Loading..."
                            loadMoreError != null -> "Retry loading"
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

/** Displays itinerary choices for an event. */
@Composable
private fun AddToItineraryDialog(
    event: Event,
    itineraries: List<Itinerary>,
    isLoading: Boolean,
    isWorking: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSelect: (Itinerary) -> Unit,
    onCreate: (String) -> Unit,
) {
    var itineraryName by remember(event.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${event.title}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isLoading -> CircularProgressIndicator()
                    itineraries.isEmpty() -> {
                        Text("You do not have an itinerary yet.")
                        OutlinedTextField(
                            value = itineraryName,
                            onValueChange = { itineraryName = it },
                            label = { Text("Itinerary name") },
                            enabled = !isWorking,
                        )
                        Button(
                            onClick = { onCreate(itineraryName.trim()) },
                            enabled = itineraryName.isNotBlank() && !isWorking,
                        ) {
                            Text(if (isWorking) "Creating..." else "Create and add")
                        }
                    }
                    else -> itineraries.forEach { itinerary ->
                        OutlinedButton(
                            onClick = { onSelect(itinerary) },
                            enabled = !isWorking,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(itinerary.name)
                        }
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text("Cancel")
            }
        },
    )
}

/** Converts an event into a two-hour itinerary activity. */
private fun Event.toActivityInput(): CreateActivityInput {
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
