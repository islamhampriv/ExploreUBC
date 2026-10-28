package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.ActivityUpdate
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateTransportationInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryDetails
import com.exploreUBC.cpen321application.data.itineraries.ItineraryItem
import com.exploreUBC.cpen321application.data.itineraries.ItineraryUpdate
import com.exploreUBC.cpen321application.data.itineraries.TransportationItem
import com.exploreUBC.cpen321application.data.itineraries.TransportationSegmentUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal enum class NewItemType {
    ACTIVITY,
    TRANSPORTATION,
}

internal class TripsRouteState(
    private val userId: String?,
    private val repository: ItinerariesRepository,
    private val scope: CoroutineScope,
) {
    var savedItineraries by mutableStateOf<List<Itinerary>>(emptyList())
        private set
    var recommendedItineraries by mutableStateOf<List<Itinerary>>(emptyList())
        private set
    var selectedItinerary by mutableStateOf<Itinerary?>(null)
        private set
    var selectedItem by mutableStateOf<ItineraryItem?>(null)
        private set
    var newItemType by mutableStateOf<NewItemType?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var isGenerating by mutableStateOf(false)
        private set
    var showGenerateConfirmation by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    val hasUser: Boolean get() = !userId.isNullOrBlank()

    suspend fun loadItineraries() {
        val ownerId = userId?.takeIf(String::isNotBlank) ?: return
        isLoading = true
        statusMessage = null
        runCatching {
            coroutineScope {
                val saved = async { repository.getItineraries(ownerId, saved = true) }
                val recommended = async {
                    repository.getItineraries(ownerId, inRecommendation = true)
                }
                saved.await() to recommended.await()
            }
        }.onSuccess { (saved, recommended) ->
            savedItineraries = saved
            recommendedItineraries = recommended
        }.onFailure { statusMessage = it.message ?: "Could not load itineraries." }
        isLoading = false
    }

    val openItinerary: (Itinerary) -> Unit = { itinerary ->
        val ownerId = userId?.takeIf(String::isNotBlank)
        if (ownerId != null) {
            scope.launch {
                isLoading = true
                statusMessage = null
                runCatching { repository.getItinerary(ownerId, itinerary.id) }
                    .onSuccess { selectedItinerary = it }
                    .onFailure { statusMessage = it.message ?: "Could not load the itinerary." }
                isLoading = false
            }
        }
    }

    val openItem: (ItineraryItem) -> Unit = { item ->
        statusMessage = null
        selectedItem = item
    }

    val closeItem: () -> Unit = {
        selectedItem = null
        statusMessage = null
    }

    val closeItinerary: () -> Unit = {
        selectedItem = null
        newItemType = null
        selectedItinerary = null
        statusMessage = null
    }

    val openNewItem: (NewItemType) -> Unit = { type ->
        if (selectedItinerary?.inRecommendation == false) {
            statusMessage = null
            newItemType = type
        }
    }

    val closeNewItem: () -> Unit = {
        newItemType = null
        statusMessage = null
    }

    val updateActivity: (ActivityUpdate) -> Unit = { update ->
        editableItemContext()?.let { context ->
            saveRequest(
                failureMessage = "Could not save the activity.",
                request = {
                    repository.updateActivity(
                        context.ownerId,
                        context.itinerary.id,
                        context.item.id,
                        update,
                    )
                },
            ) { updatedItem -> updateSelectedItem(updatedItem, "Activity saved.") }
        }
    }

    val updateTransportation: (TransportationSegmentUpdate) -> Unit = { update ->
        editableItemContext()?.let { context ->
            saveRequest(
                failureMessage = "Could not save transportation.",
                request = {
                    repository.updateTransportationSegment(
                        context.ownerId,
                        context.itinerary.id,
                        context.item.id,
                        update,
                    )
                },
            ) { updatedItem -> updateSelectedItem(updatedItem, "Transportation saved.") }
        }
    }

    val deleteItem: () -> Unit = {
        editableItemContext()?.let { context ->
            saveRequest(
                failureMessage = "Could not delete the item.",
                request = {
                    when (context.item) {
                        is ActivityItem -> repository.deleteActivity(
                            context.ownerId,
                            context.itinerary.id,
                            context.item.id,
                        )
                        is TransportationItem -> repository.deleteTransportationSegment(
                            context.ownerId,
                            context.itinerary.id,
                            context.item.id,
                        )
                    }
                },
            ) {
                val updated = context.itinerary.removeItem(context.item.id)
                selectedItem = null
                updateSelectedItinerary(updated)
                statusMessage = "Item deleted."
            }
        }
    }

    val addActivity: (CreateActivityInput) -> Unit = { input ->
        itineraryContext()?.let { context ->
            saveRequest(
                failureMessage = "Could not add the activity.",
                request = { repository.addActivity(context.ownerId, context.itinerary.id, input) },
            ) { addedItem -> addItem(addedItem, "Activity added.") }
        }
    }

    val addTransportation: (CreateTransportationInput) -> Unit = { input ->
        itineraryContext()?.let { context ->
            saveRequest(
                failureMessage = "Could not add transportation.",
                request = {
                    repository.addTransportationSegment(
                        context.ownerId,
                        context.itinerary.id,
                        input,
                    )
                },
            ) { addedItem -> addItem(addedItem, "Transportation added.") }
        }
    }

    val saveDetails: (ItineraryDetails) -> Unit = { details ->
        itineraryContext()?.let { context ->
            saveRequest(
                failureMessage = "Could not save itinerary details.",
                request = {
                    repository.updateItinerary(
                        context.ownerId,
                        context.itinerary.id,
                        ItineraryUpdate(
                            name = details.name,
                            description = details.description,
                            startingLocation = details.startingLocation,
                        ),
                    )
                },
            ) { updated ->
                updateSelectedItinerary(updated)
                statusMessage = "Itinerary details saved."
            }
        }
    }

    val saveRecommendation: () -> Unit = {
        itineraryContext()?.let { context ->
            saveRequest(
                failureMessage = "Could not save the itinerary.",
                request = {
                    repository.updateItinerary(
                        context.ownerId,
                        context.itinerary.id,
                        ItineraryUpdate(saved = true, inRecommendation = false),
                    )
                },
            ) { updated ->
                selectedItinerary = updated
                savedItineraries = savedItineraries.upsert(updated)
                recommendedItineraries = recommendedItineraries.filterNot {
                    it.id == updated.id
                }
                statusMessage = "Itinerary saved."
            }
        }
    }

    val showGenerateConfirmationAction: () -> Unit = {
        if (hasUser) showGenerateConfirmation = true
    }

    val dismissGenerateConfirmation: () -> Unit = {
        showGenerateConfirmation = false
    }

    val generateRecommendations: () -> Unit = {
        val ownerId = userId?.takeIf(String::isNotBlank)
        if (ownerId != null) {
            showGenerateConfirmation = false
            scope.launch {
                isGenerating = true
                statusMessage = null
                runCatching { repository.generateRecommendations(ownerId) }
                    .onSuccess {
                        recommendedItineraries = it
                        statusMessage = "Recommendations generated."
                    }
                    .onFailure {
                        statusMessage = it.message ?: "Could not generate recommendations."
                    }
                isGenerating = false
            }
        }
    }

    private fun editableItemContext(): ItemContext? {
        val context = itemContext() ?: return null
        return context.takeUnless { it.itinerary.inRecommendation }
    }

    private fun itemContext(): ItemContext? {
        val context = itineraryContext() ?: return null
        val item = selectedItem ?: return null
        return ItemContext(context.ownerId, context.itinerary, item)
    }

    private fun itineraryContext(): ItineraryContext? {
        val ownerId = userId?.takeIf(String::isNotBlank) ?: return null
        val itinerary = selectedItinerary ?: return null
        return ItineraryContext(ownerId, itinerary)
    }

    private fun updateSelectedItem(item: ItineraryItem, message: String) {
        val itinerary = selectedItinerary ?: return
        val updated = itinerary.replaceItem(item)
        selectedItem = item
        updateSelectedItinerary(updated)
        statusMessage = message
    }

    private fun addItem(item: ItineraryItem, message: String) {
        val itinerary = selectedItinerary ?: return
        updateSelectedItinerary(itinerary.copy(items = itinerary.items + item))
        newItemType = null
        statusMessage = message
    }

    private fun updateSelectedItinerary(itinerary: Itinerary) {
        selectedItinerary = itinerary
        savedItineraries = savedItineraries.replace(itinerary)
        recommendedItineraries = recommendedItineraries.replace(itinerary)
    }

    private fun <T> saveRequest(
        failureMessage: String,
        request: suspend () -> T,
        onSuccess: (T) -> Unit,
    ) {
        scope.launch {
            isSaving = true
            statusMessage = null
            runCatching { request() }
                .onSuccess(onSuccess)
                .onFailure { statusMessage = it.message ?: failureMessage }
            isSaving = false
        }
    }
}

private data class ItineraryContext(
    val ownerId: String,
    val itinerary: Itinerary,
)

private data class ItemContext(
    val ownerId: String,
    val itinerary: Itinerary,
    val item: ItineraryItem,
)

@Composable
internal fun rememberTripsRouteState(
    userId: String?,
    repository: ItinerariesRepository,
): TripsRouteState {
    val scope = rememberCoroutineScope()
    return remember(userId, repository, scope) {
        TripsRouteState(userId, repository, scope)
    }
}

private fun List<Itinerary>.replace(updated: Itinerary): List<Itinerary> =
    map { if (it.id == updated.id) updated else it }

private fun List<Itinerary>.upsert(updated: Itinerary): List<Itinerary> =
    if (any { it.id == updated.id }) replace(updated) else this + updated

private fun Itinerary.replaceItem(updated: ItineraryItem): Itinerary = copy(
    items = items.map { if (it.id == updated.id) updated else it },
)

private fun Itinerary.removeItem(itemId: String): Itinerary = copy(
    items = items.filterNot { it.id == itemId },
)
