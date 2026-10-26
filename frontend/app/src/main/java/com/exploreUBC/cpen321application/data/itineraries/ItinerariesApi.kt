package com.exploreUBC.cpen321application.data.itineraries

import java.io.IOException
import com.exploreUBC.cpen321application.data.network.ApiRequest
import com.exploreUBC.cpen321application.data.network.BackendClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface ItinerariesRepository {
    /** Creates an empty saved itinerary for a user. */
    suspend fun createItinerary(
        userId: String,
        input: CreateItineraryInput,
    ): Itinerary

    suspend fun getItineraries(
        userId: String,
        saved: Boolean? = null,
        inRecommendation: Boolean? = null,
    ): List<Itinerary>

    suspend fun getItinerary(userId: String, itineraryId: String): Itinerary

    suspend fun updateItineraryDetails(
        userId: String,
        itineraryId: String,
        details: ItineraryDetails,
    ): Itinerary

    suspend fun addActivity(
        userId: String,
        itineraryId: String,
        activity: CreateActivityInput,
    ): ActivityItem

    suspend fun addTransportationSegment(
        userId: String,
        itineraryId: String,
        segment: CreateTransportationInput,
    ): TransportationItem

    suspend fun updateActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
        update: ActivityUpdate,
    ): ActivityItem

    suspend fun updateTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
        update: TransportationSegmentUpdate,
    ): TransportationItem

    suspend fun deleteActivity(userId: String, itineraryId: String, activityId: String)

    suspend fun deleteTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
    )
}

class ItinerariesApi(private val backendClient: BackendClient) : ItinerariesRepository {
    /** Creates an empty saved itinerary for a user. */
    override suspend fun createItinerary(
        userId: String,
        input: CreateItineraryInput,
    ): Itinerary = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("ownerId", userId)
            .put("name", input.name)
            .put("items", JSONArray())
            .put("saved", true)
            .put("inRecommendation", false)
            .put("score", JSONObject())
            .apply {
                input.description?.let { put("description", it) }
                input.startingLocation?.let { put("startingLocation", it) }
            }
            .toString()
        val response = execute(
            method = "POST",
            path = itineraryCollectionPath(userId),
            requestBody = requestBody,
        )
        parseItinerary(response.getJSONObject("itinerary"))
    }

    override suspend fun getItineraries(
        userId: String,
        saved: Boolean?,
        inRecommendation: Boolean?,
    ): List<Itinerary> = withContext(Dispatchers.IO) {
        val query = buildList {
            saved?.let { add("saved=$it") }
            inRecommendation?.let { add("inRecommendation=$it") }
        }.joinToString("&")
        val path = itineraryCollectionPath(userId) +
            if (query.isEmpty()) "" else "?$query"
        val response = execute("GET", path)
        val itineraries = response.getJSONArray("itineraries")
        List(itineraries.length()) { index ->
            parseItinerary(itineraries.getJSONObject(index))
        }
    }

    override suspend fun getItinerary(
        userId: String,
        itineraryId: String,
    ): Itinerary = withContext(Dispatchers.IO) {
        val response = execute(
            method = "GET",
            path = "${itineraryCollectionPath(userId)}/${encode(itineraryId)}",
        )
        parseItinerary(response.getJSONObject("itinerary"))
    }

    override suspend fun updateItineraryDetails(
        userId: String,
        itineraryId: String,
        details: ItineraryDetails,
    ): Itinerary = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("name", details.name)
            .apply {
                details.description?.let { put("description", it) }
                details.startingLocation?.let { put("startingLocation", it) }
            }
            .toString()
        val response = execute(
            method = "PATCH",
            path = "${itineraryCollectionPath(userId)}/${encode(itineraryId)}",
            requestBody = requestBody,
        )
        parseItinerary(response.getJSONObject("itinerary"))
    }

    override suspend fun addActivity(
        userId: String,
        itineraryId: String,
        activity: CreateActivityInput,
    ): ActivityItem = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("type", "activity")
            .put("name", activity.name)
            .put("image", activity.image)
            .put("location", activity.location)
            .put("startTime", activity.startTime)
            .put("endTime", activity.endTime)
            .apply {
                activity.description?.let { put("description", it) }
                activity.category?.let { put("category", it) }
            }
            .toString()
        val response = execute(
            method = "POST",
            path = "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/activities",
            requestBody = requestBody,
        )
        parseItem(response.getJSONObject("activity")) as ActivityItem
    }

    override suspend fun addTransportationSegment(
        userId: String,
        itineraryId: String,
        segment: CreateTransportationInput,
    ): TransportationItem = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("type", "transportation")
            .put("mode", segment.mode)
            .put("startLocation", segment.startLocation)
            .put("endLocation", segment.endLocation)
            .put("distanceMeters", segment.distanceMeters)
            .put("startTime", segment.startTime)
            .put("endTime", segment.endTime)
            .toString()
        val response = execute(
            method = "POST",
            path = "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/transportation-segments",
            requestBody = requestBody,
        )
        parseItem(response.getJSONObject("transportationSegment")) as TransportationItem
    }

    override suspend fun updateActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
        update: ActivityUpdate,
    ): ActivityItem = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("name", update.name)
            .put("image", update.image)
            .put("location", update.location)
            .put("startTime", update.startTime)
            .put("endTime", update.endTime)
            .apply {
                update.description?.let { put("description", it) }
                update.category?.let { put("category", it) }
            }
            .toString()
        val response = execute(
            method = "PATCH",
            path = "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/activities/${encode(activityId)}",
            requestBody = requestBody,
        )
        parseItem(response.getJSONObject("activity")) as ActivityItem
    }

    override suspend fun updateTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
        update: TransportationSegmentUpdate,
    ): TransportationItem = withContext(Dispatchers.IO) {
        val requestBody = JSONObject()
            .put("mode", update.mode)
            .put("startLocation", update.startLocation)
            .put("endLocation", update.endLocation)
            .put("distanceMeters", update.distanceMeters)
            .put("startTime", update.startTime)
            .put("endTime", update.endTime)
            .toString()
        val response = execute(
            method = "PATCH",
            path = "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/transportation-segments/${encode(segmentId)}",
            requestBody = requestBody,
        )
        parseItem(response.getJSONObject("transportationSegment")) as TransportationItem
    }

    override suspend fun deleteActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
    ): Unit = withContext(Dispatchers.IO) {
        execute(
            method = "DELETE",
            path = "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/activities/${encode(activityId)}",
        )
    }

    override suspend fun deleteTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
    ): Unit = withContext(Dispatchers.IO) {
        execute(
            method = "DELETE",
            path = "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/transportation-segments/${encode(segmentId)}",
        )
    }

    private fun itineraryCollectionPath(userId: String): String =
        "/users/${encode(userId)}/itineraries"

    private suspend fun execute(
        method: String,
        path: String,
        requestBody: String? = null,
    ): JSONObject {
        val responseText = backendClient.execute(
            ApiRequest(
                method = method,
                path = path,
                body = requestBody,
            ),
        )
        return if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
    }

    private fun parseItinerary(json: JSONObject): Itinerary {
        val itemJson = json.getJSONArray("items")
        val scoreJson = json.getJSONObject("score")
        return Itinerary(
            id = json.getString("id"),
            ownerId = json.getString("ownerId"),
            name = json.getString("name"),
            description = json.optionalString("description"),
            startingLocation = json.optionalString("startingLocation"),
            items = List(itemJson.length()) { index -> parseItem(itemJson.getJSONObject(index)) },
            createdAt = json.getString("createdAt"),
            updatedAt = json.getString("updatedAt"),
            saved = json.getBoolean("saved"),
            inRecommendation = json.getBoolean("inRecommendation"),
            score = scoreJson.keys().asSequence().associateWith { key -> scoreJson.getDouble(key) },
        )
    }

    private fun parseItem(json: JSONObject): ItineraryItem = when (json.getString("type")) {
        "activity" -> ActivityItem(
            id = json.getString("id"),
            name = json.getString("name"),
            image = json.getString("image"),
            description = json.optionalString("description"),
            category = json.optionalString("category"),
            location = json.getString("location"),
            startTime = json.getString("startTime"),
            endTime = json.getString("endTime"),
        )
        "transportation" -> TransportationItem(
            id = json.getString("id"),
            mode = json.getString("mode"),
            startLocation = json.getString("startLocation"),
            endLocation = json.getString("endLocation"),
            distanceMeters = json.getDouble("distanceMeters"),
            startTime = json.getString("startTime"),
            endTime = json.getString("endTime"),
        )
        else -> throw IOException("Unknown itinerary item type: ${json.optString("type")}")
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
