package com.exploreUBC.cpen321application.data.events

import com.exploreUBC.cpen321application.data.network.ApiRequest
import com.exploreUBC.cpen321application.data.network.BackendClient
import java.net.URLEncoder
import org.json.JSONObject

interface EventsRepository {
    /** Retrieves recommended events for a user and their current location. */
    suspend fun getEvents(
        userId: String,
        latitude: Double,
        longitude: Double,
        page: Int,
        limit: Int,
    ): List<Event>
}

class EventsApi(private val backendClient: BackendClient) : EventsRepository {
    /** Retrieves recommended events from the backend. */
    override suspend fun getEvents(
        userId: String,
        latitude: Double,
        longitude: Double,
        page: Int,
        limit: Int,
    ): List<Event> {
        val encodedUserId = URLEncoder.encode(userId, "UTF-8")
        val path =
            "/users/$encodedUserId/events" +
                "?latitude=$latitude&longitude=$longitude&page=$page&limit=$limit"
        val body = backendClient.execute(ApiRequest(method = "GET", path = path))
        val eventArray = JSONObject(body).getJSONArray("events")

        return List(eventArray.length()) { index ->
            val event = eventArray.getJSONObject(index)
            Event(
                id = event.getString("id"),
                title = event.getString("title"),
                description = event.optionalString("description"),
                imageUrl = event.getString("imageUrl"),
                startDateTime = event.getString("startDateTime"),
                venueName = event.getString("venueName"),
                city = event.getString("city"),
                category = event.getString("category"),
                ticketUrl = event.getString("ticketUrl"),
                priceMin = event.optionalDouble("priceMin"),
                priceMax = event.optionalDouble("priceMax"),
                currency = event.optionalString("currency"),
                distanceKm = event.getDouble("distanceKm")
            )
        }
    }
}

private fun JSONObject.optionalDouble(name: String): Double? =
    if (has(name) && !isNull(name)) getDouble(name) else null

private fun JSONObject.optionalString(name: String): String? =
    if (has(name) && !isNull(name)) getString(name) else null
