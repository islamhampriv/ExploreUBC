package com.exploreUBC.cpen321application.data.events

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface EventsRepository {
    /** Retrieves recommended events for a user and their current location. */
    suspend fun getEvents(
        userId: String,
        latitude: Double,
        longitude: Double,
    ): List<Event>
}

class EventsApi(private val apiBaseUrl: String) : EventsRepository {
    /** Retrieves recommended events from the backend. */
    override suspend fun getEvents(
        userId: String,
        latitude: Double,
        longitude: Double,
    ): List<Event> = withContext(Dispatchers.IO) {
        val encodedUserId = URLEncoder.encode(userId, "UTF-8")
        val eventsUrl =
            "${apiBaseUrl.trimEnd('/')}/api/v1/users/$encodedUserId/events" +
                "?latitude=$latitude&longitude=$longitude"
        val connection = URL(eventsUrl).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Backend returned HTTP ${connection.responseCode}")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val eventArray = JSONObject(body).getJSONArray("events")

            List(eventArray.length()) { index ->
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
        } finally {
            connection.disconnect()
        }
    }
}

private fun JSONObject.optionalDouble(name: String): Double? =
    if (has(name) && !isNull(name)) getDouble(name) else null

private fun JSONObject.optionalString(name: String): String? =
    if (has(name) && !isNull(name)) getString(name) else null
