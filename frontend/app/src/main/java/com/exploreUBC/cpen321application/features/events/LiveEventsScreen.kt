package com.exploreUBC.cpen321application.features.events

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
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LiveEventsScreen(apiBaseUrl: String, modifier: Modifier = Modifier) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshCount by remember { mutableStateOf(0) }

    LaunchedEffect(apiBaseUrl, refreshCount) {
        isLoading = true
        errorMessage = null
        try {
            events = fetchEvents(
                apiBaseUrl = apiBaseUrl,
                categories = listOf("Music", "Sports"),
                latitude = 49.2606,
                longitude = -123.2460
            )
        } catch (error: Exception) {
            errorMessage = error.message ?: "Unable to load events"
        } finally {
            isLoading = false
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
        errorMessage != null -> MessageWithButton(
            message = errorMessage ?: "Unable to load events",
            buttonText = "Retry",
            onClick = { refreshCount++ },
            modifier = modifier
        )
        events.isEmpty() -> MessageWithButton(
            message = "No events found",
            buttonText = "Refresh",
            onClick = { refreshCount++ },
            modifier = modifier
        )
        else -> EventList(
            events = events,
            onRefresh = { refreshCount++ },
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
            Text(formatDate(event.startDateTime))
            Text("${event.venueName}, ${event.city}")
            Text(formatPrice(event))
            Text(String.format(Locale.CANADA, "%.1f km away", event.distanceKm))
            Button(onClick = { uriHandler.openUri(event.ticketUrl) }) {
                Text("View tickets")
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
