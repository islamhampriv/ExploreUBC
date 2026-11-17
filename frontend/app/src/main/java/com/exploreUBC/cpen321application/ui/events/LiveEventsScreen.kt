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
import com.exploreUBC.cpen321application.data.location.UserLocation
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateItineraryInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryTimeConflict
import com.exploreUBC.cpen321application.data.itineraries.ItineraryTimeConflictException
import com.exploreUBC.cpen321application.ui.trips.DateTimePickerField
import com.exploreUBC.cpen321application.ui.trips.TimeConflictDialog
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EVENTS_PAGE_SIZE = 20

@Composable
fun LiveEventsScreen(
    userId: String?,
    eventsRepository: EventsRepository,
    itinerariesRepository: ItinerariesRepository,
    location: UserLocation = UserLocation.UBC,
    initialEvents: List<Event> = emptyList(),
    onFirstPageLoaded: (List<Event>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state = remember(userId, location) { LiveEventsScreenState(initialEvents) }
    LoadEventsEffect(userId, location, eventsRepository, state, onFirstPageLoaded)
    LoadItinerariesEffect(userId, itinerariesRepository, state)
    LiveEventsContent(state, modifier)
    EventItineraryDialog(userId, itinerariesRepository, state)
}

internal class LiveEventsScreenState(initialEvents: List<Event>) {
    var events by mutableStateOf(initialEvents)
    var isLoading by mutableStateOf(initialEvents.isEmpty())
    var isLoadingMore by mutableStateOf(false)
    var hasMoreEvents by mutableStateOf(
        initialEvents.isEmpty() || initialEvents.size == EVENTS_PAGE_SIZE,
    )
    var errorMessage by mutableStateOf<String?>(null)
    var currentPage by mutableStateOf(1)
    var requestCount by mutableStateOf(0)
    var eventToAdd by mutableStateOf<Event?>(null)
    var itineraries by mutableStateOf<List<Itinerary>>(emptyList())
    var isLoadingItineraries by mutableStateOf(false)
    var isAddingToItinerary by mutableStateOf(false)
    var itineraryError by mutableStateOf<String?>(null)
    var itineraryStatus by mutableStateOf<String?>(null)
    var pendingReplacement by mutableStateOf<PendingEventReplacement?>(null)

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

internal data class PendingEventReplacement(
    val itinerary: Itinerary,
    val activity: CreateActivityInput,
    val conflicts: List<ItineraryTimeConflict>,
)

@Composable
private fun LoadEventsEffect(
    userId: String?,
    location: UserLocation,
    repository: EventsRepository,
    state: LiveEventsScreenState,
    onFirstPageLoaded: (List<Event>) -> Unit,
) {
    LaunchedEffect(
        userId,
        location,
        repository,
        state.currentPage,
        state.requestCount,
    ) {
        val isFirstPage = state.currentPage == 1
        state.isLoading = isFirstPage
        state.isLoadingMore = !isFirstPage
        state.errorMessage = null
        if (userId.isNullOrBlank()) {
            publishEventsLoad(
                state,
                isFirstPage,
                Result.failure(IOException("User is unavailable")),
                onFirstPageLoaded,
            )
        } else {
            val loaded = runCatching {
                repository.getEvents(
                    userId,
                    location.latitude,
                    location.longitude,
                    state.currentPage,
                    EVENTS_PAGE_SIZE,
                )
            }
            publishEventsLoad(state, isFirstPage, loaded, onFirstPageLoaded)
        }
    }
}

private suspend fun publishEventsLoad(
    state: LiveEventsScreenState,
    isFirstPage: Boolean,
    loaded: Result<List<Event>>,
    onFirstPageLoaded: (List<Event>) -> Unit,
) {
    withContext(Dispatchers.Main.immediate) {
        loaded.onSuccess { newEvents ->
            state.events = if (isFirstPage) {
                newEvents
            } else {
                (state.events + newEvents).distinctBy { it.id }
            }
            if (isFirstPage) onFirstPageLoaded(newEvents)
            state.hasMoreEvents = newEvents.size == EVENTS_PAGE_SIZE
        }.onFailure { error ->
            state.errorMessage = error.message ?: "Unable to load events"
        }
        state.isLoading = false
        state.isLoadingMore = false
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
            val loaded = runCatching { repository.getItineraries(userId, saved = true) }
            withContext(Dispatchers.Main.immediate) {
                loaded.onSuccess { state.itineraries = it }
                    .onFailure {
                        state.itineraryError = it.message ?: "Unable to load itineraries"
                    }
                state.isLoadingItineraries = false
            }
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

