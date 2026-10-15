package com.exploreUBC.cpen321application.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.exploreUBC.cpen321application.data.events.Event
import com.exploreUBC.cpen321application.data.events.EventsRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val EVENTS_PAGE_SIZE = 20

@Composable
fun LiveEventsScreen(
    userId: String?,
    eventsRepository: EventsRepository,
    modifier: Modifier = Modifier,
) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMoreEvents by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var requestCount by remember { mutableStateOf(0) }

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
        items(events, key = { it.id }) { event ->
            EventCard(event)
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
private fun EventCard(event: Event) {
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
        }
    }
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
