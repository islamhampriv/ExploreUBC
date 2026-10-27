package com.exploreUBC.cpen321application.e2e.itineraries

import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.ActivityUpdate
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateItineraryInput
import com.exploreUBC.cpen321application.data.itineraries.CreateTransportationInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.ItineraryMap
import com.exploreUBC.cpen321application.data.itineraries.ItineraryItem
import com.exploreUBC.cpen321application.data.itineraries.ItineraryUpdate
import com.exploreUBC.cpen321application.data.itineraries.TransportationItem
import com.exploreUBC.cpen321application.data.itineraries.TransportationSegmentUpdate

internal const val E2E_USER_ID = "6705f0000000000000000001"
internal const val E2E_ITINERARY_ID = "itinerary-1"
internal const val E2E_ACTIVITY_ID = "activity-1"
internal const val E2E_TRANSPORTATION_ID = "transportation-1"

internal class InMemoryItinerariesRepository : ItinerariesRepository {
    private var revision = 1
    private var nextId = 2

    var itineraries: List<Itinerary> = listOf(sampleItinerary())
        private set

    override suspend fun createItinerary(
        userId: String,
        input: CreateItineraryInput,
    ): Itinerary {
        val itinerary = Itinerary(
            id = "itinerary-${nextId++}",
            ownerId = userId,
            name = input.name,
            description = input.description,
            startingLocation = input.startingLocation,
            items = emptyList(),
            createdAt = timestamp(),
            updatedAt = timestamp(),
            saved = true,
            inRecommendation = false,
            score = emptyMap(),
        )
        itineraries = itineraries + itinerary
        return itinerary
    }

    override suspend fun getItineraries(
        userId: String,
        saved: Boolean?,
        inRecommendation: Boolean?,
    ): List<Itinerary> = itineraries.filter {
        it.ownerId == userId &&
            (saved == null || it.saved == saved) &&
            (inRecommendation == null || it.inRecommendation == inRecommendation)
    }

    override suspend fun getItinerary(userId: String, itineraryId: String): Itinerary =
        requireItinerary(userId, itineraryId)

    override suspend fun generateRecommendations(userId: String): List<Itinerary> = emptyList()

    override suspend fun getItineraryMap(
        userId: String,
        itineraryId: String,
    ): ItineraryMap {
        val itinerary = requireItinerary(userId, itineraryId)
        return ItineraryMap(
            itineraryId = itinerary.id,
            ownerId = itinerary.ownerId,
            itineraryUpdatedAt = itinerary.updatedAt,
            markers = emptyList(),
            routes = emptyList(),
            unmappedActivities = emptyList(),
            generatedAt = itinerary.updatedAt,
        )
    }

    override suspend fun updateItinerary(
        userId: String,
        itineraryId: String,
        update: ItineraryUpdate,
    ): Itinerary = replaceItinerary(userId, itineraryId) { itinerary ->
        itinerary.copy(
            name = update.name ?: itinerary.name,
            description = update.description ?: itinerary.description,
            startingLocation = update.startingLocation ?: itinerary.startingLocation,
            saved = update.saved ?: itinerary.saved,
            inRecommendation = update.inRecommendation ?: itinerary.inRecommendation,
            updatedAt = timestamp(),
        )
    }

    override suspend fun deleteItinerary(userId: String, itineraryId: String) {
        requireItinerary(userId, itineraryId)
        itineraries = itineraries.filterNot { it.id == itineraryId }
    }

    override suspend fun addActivity(
        userId: String,
        itineraryId: String,
        activity: CreateActivityInput,
    ): ActivityItem {
        val added = ActivityItem(
            id = "activity-${nextId++}",
            name = activity.name,
            image = activity.image,
            description = activity.description,
            category = activity.category,
            location = activity.location,
            startTime = activity.startTime,
            endTime = activity.endTime,
        )
        appendItem(userId, itineraryId, added)
        return added
    }

    override suspend fun addTransportationSegment(
        userId: String,
        itineraryId: String,
        segment: CreateTransportationInput,
    ): TransportationItem {
        val added = TransportationItem(
            id = "transportation-${nextId++}",
            mode = segment.mode,
            startLocation = segment.startLocation,
            endLocation = segment.endLocation,
            distanceMeters = segment.distanceMeters,
            startTime = segment.startTime,
            endTime = segment.endTime,
        )
        appendItem(userId, itineraryId, added)
        return added
    }

    override suspend fun updateActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
        update: ActivityUpdate,
    ): ActivityItem {
        var updated: ActivityItem? = null
        replaceItinerary(userId, itineraryId) { itinerary ->
            itinerary.copy(
                items = itinerary.items.map { item ->
                    if (item is ActivityItem && item.id == activityId) {
                        item.copy(
                            name = update.name,
                            image = update.image,
                            description = update.description,
                            category = update.category,
                            location = update.location,
                            startTime = update.startTime,
                            endTime = update.endTime,
                        ).also { updated = it }
                    } else {
                        item
                    }
                },
                updatedAt = timestamp(),
            )
        }
        return requireNotNull(updated)
    }

    override suspend fun updateTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
        update: TransportationSegmentUpdate,
    ): TransportationItem {
        var updated: TransportationItem? = null
        replaceItinerary(userId, itineraryId) { itinerary ->
            itinerary.copy(
                items = itinerary.items.map { item ->
                    if (item is TransportationItem && item.id == segmentId) {
                        item.copy(
                            mode = update.mode,
                            startLocation = update.startLocation,
                            endLocation = update.endLocation,
                            distanceMeters = update.distanceMeters,
                            startTime = update.startTime,
                            endTime = update.endTime,
                        ).also { updated = it }
                    } else {
                        item
                    }
                },
                updatedAt = timestamp(),
            )
        }
        return requireNotNull(updated)
    }

    override suspend fun deleteActivity(userId: String, itineraryId: String, activityId: String) {
        removeItem(userId, itineraryId, activityId)
    }

    override suspend fun deleteTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
    ) {
        removeItem(userId, itineraryId, segmentId)
    }

    private fun appendItem(userId: String, itineraryId: String, item: ItineraryItem) {
        replaceItinerary(userId, itineraryId) { itinerary ->
            itinerary.copy(items = itinerary.items + item, updatedAt = timestamp())
        }
    }

    private fun removeItem(userId: String, itineraryId: String, itemId: String) {
        replaceItinerary(userId, itineraryId) { itinerary ->
            itinerary.copy(
                items = itinerary.items.filterNot { it.id == itemId },
                updatedAt = timestamp(),
            )
        }
    }

    private fun requireItinerary(userId: String, itineraryId: String): Itinerary =
        requireNotNull(itineraries.find { it.ownerId == userId && it.id == itineraryId })

    private fun replaceItinerary(
        userId: String,
        itineraryId: String,
        transform: (Itinerary) -> Itinerary,
    ): Itinerary {
        val current = requireItinerary(userId, itineraryId)
        val updated = transform(current)
        itineraries = itineraries.map { if (it.id == itineraryId) updated else it }
        return updated
    }

    private fun timestamp(): String = "2026-10-29T12:00:${revision++.toString().padStart(2, '0')}Z"
}

private fun sampleItinerary() = Itinerary(
    id = E2E_ITINERARY_ID,
    ownerId = E2E_USER_ID,
    name = "UBC Test Day",
    description = "A deterministic itinerary for frontend end-to-end tests.",
    startingLocation = "UBC Bus Exchange",
    items = listOf(
        ActivityItem(
            id = E2E_ACTIVITY_ID,
            name = "Museum Visit",
            image = "",
            description = "Visit the museum.",
            category = "museum",
            location = "Museum of Anthropology",
            startTime = "2027-11-30T18:00:00Z",
            endTime = "2027-11-30T19:00:00Z",
        ),
        TransportationItem(
            id = E2E_TRANSPORTATION_ID,
            mode = "walking",
            startLocation = "UBC Bus Exchange",
            endLocation = "Museum of Anthropology",
            distanceMeters = 800.0,
            startTime = "2027-11-30T17:00:00Z",
            endTime = "2027-11-30T17:30:00Z",
        ),
    ),
    createdAt = "2026-10-29T12:00:00Z",
    updatedAt = "2026-10-29T12:00:00Z",
    saved = true,
    inRecommendation = false,
    score = emptyMap(),
)
