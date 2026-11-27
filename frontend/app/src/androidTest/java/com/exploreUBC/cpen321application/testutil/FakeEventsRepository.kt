package com.exploreUBC.cpen321application.testutil

import com.exploreUBC.cpen321application.data.events.Event
import com.exploreUBC.cpen321application.data.events.EventsRepository
import java.util.concurrent.CopyOnWriteArrayList

/** One recorded call to [FakeEventsRepository.getEvents]. */
internal data class EventsCall(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val page: Int,
    val limit: Int,
)

/** Events repository whose answer for each call is produced by [handler]; it may throw. */
internal class FakeEventsRepository(
    var handler: suspend (EventsCall) -> List<Event> = { emptyList() },
) : EventsRepository {
    val calls = CopyOnWriteArrayList<EventsCall>()

    override suspend fun getEvents(
        userId: String,
        latitude: Double,
        longitude: Double,
        page: Int,
        limit: Int,
    ): List<Event> {
        val call = EventsCall(userId, latitude, longitude, page, limit)
        calls.add(call)
        return handler(call)
    }
}

internal fun testEvent(
    id: String,
    title: String = "Event $id",
    description: String? = "Description of $id",
    priceMin: Double? = 30.0,
    priceMax: Double? = 60.0,
    currency: String? = "CAD",
    ticketUrl: String = "https://example.com/tickets/$id",
) = Event(
    id = id,
    title = title,
    description = description,
    imageUrl = "https://example.com/$id.jpg",
    startDateTime = "2026-12-05T03:00:00.000Z",
    endDateTime = "2026-12-05T05:00:00.000Z",
    venueName = "Venue $id",
    city = "Vancouver",
    category = "Music",
    ticketUrl = ticketUrl,
    priceMin = priceMin,
    priceMax = priceMax,
    currency = currency,
    distanceKm = 2.5,
)

internal fun testEvents(count: Int, idPrefix: String = "event"): List<Event> =
    List(count) { testEvent("$idPrefix-${it + 1}") }
