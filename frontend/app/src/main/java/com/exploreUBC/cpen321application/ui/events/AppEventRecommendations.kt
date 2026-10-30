package com.exploreUBC.cpen321application.ui.events

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.exploreUBC.cpen321application.data.events.Event
import com.exploreUBC.cpen321application.data.events.EventsRepository
import com.exploreUBC.cpen321application.data.location.UserLocation
import java.io.IOException
import org.json.JSONException

private const val NOTIFICATION_PAGE_SIZE = 20

private data class EventRecommendationsSnapshot(
    val location: UserLocation,
    val events: List<Event>,
)

internal class AppEventRecommendationsState {
    private var snapshot by mutableStateOf<EventRecommendationsSnapshot?>(null)
    private var notification by mutableStateOf<String?>(null)
    var refreshCount by mutableStateOf(0)
        private set

    val snackbarHostState = SnackbarHostState()

    fun eventsAt(location: UserLocation): List<Event> =
        snapshot?.takeIf { it.location == location }?.events.orEmpty()

    fun recordRecommendations(
        location: UserLocation,
        recommendations: List<Event>,
        notifyAboutNewEvents: Boolean,
    ) {
        val previousIds = snapshot?.events?.mapTo(mutableSetOf()) { it.id }
        snapshot = EventRecommendationsSnapshot(location, recommendations)
        if (!notifyAboutNewEvents || previousIds == null) return

        val newEventCount = recommendations.count { it.id !in previousIds }
        notification = when {
            newEventCount == 1 -> "1 new event near you"
            newEventCount > 1 -> "$newEventCount new events near you"
            else -> null
        }
    }

    fun refreshAfterPreferenceChange() {
        refreshCount++
    }

    fun currentNotification(): String? = notification

    fun clearNotification() {
        notification = null
    }
}

@Composable
internal fun rememberAppEventRecommendationsState(): AppEventRecommendationsState =
    remember { AppEventRecommendationsState() }

@Composable
internal fun AppEventRecommendationEffects(
    userId: String?,
    location: UserLocation,
    isEventsVisible: Boolean,
    repository: EventsRepository,
    state: AppEventRecommendationsState,
    onViewEvents: () -> Unit,
) {
    LaunchedEffect(location, state.refreshCount) {
        if (!isEventsVisible && !userId.isNullOrBlank()) {
            try {
                val recommendations = repository.getEvents(
                    userId,
                    location.latitude,
                    location.longitude,
                    page = 1,
                    limit = NOTIFICATION_PAGE_SIZE,
                )
                state.recordRecommendations(location, recommendations, true)
            } catch (_: IOException) {
                // Live Events will show any loading error when the user opens it.
            } catch (_: JSONException) {
                // Live Events will show any loading error when the user opens it.
            }
        }
    }

    LaunchedEffect(state.currentNotification()) {
        val message = state.currentNotification()
        if (message != null) {
            val result = state.snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "View",
            )
            if (result == SnackbarResult.ActionPerformed) onViewEvents()
            state.clearNotification()
        }
    }
}
