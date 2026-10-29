package com.exploreUBC.cpen321application.data.itineraries

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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

    suspend fun addActivity(
        userId: String,
        itineraryId: String,
        activity: CreateActivityInput,
        replaceConflicts: Boolean,
    ): ActivityItem = addActivity(userId, itineraryId, activity)

    suspend fun addTransportationSegment(
        userId: String,
        itineraryId: String,
        segment: CreateTransportationInput,
    ): TransportationItem

    suspend fun addTransportationSegment(
        userId: String,
        itineraryId: String,
        segment: CreateTransportationInput,
        replaceConflicts: Boolean,
    ): TransportationItem = addTransportationSegment(userId, itineraryId, segment)

    suspend fun updateActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
        update: ActivityUpdate,
    ): ActivityItem

    suspend fun updateActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
        update: ActivityUpdate,
        replaceConflicts: Boolean,
    ): ActivityItem = updateActivity(userId, itineraryId, activityId, update)

    suspend fun updateTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
        update: TransportationSegmentUpdate,
    ): TransportationItem

    suspend fun updateTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
        update: TransportationSegmentUpdate,
        replaceConflicts: Boolean,
    ): TransportationItem =
        updateTransportationSegment(userId, itineraryId, segmentId, update)

    suspend fun deleteActivity(userId: String, itineraryId: String, activityId: String)

    suspend fun deleteTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
    )
}

class ItinerariesApi(private val apiBaseUrl: String) : ItinerariesRepository {
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
    ): ActivityItem = addActivity(userId, itineraryId, activity, false)

    override suspend fun addActivity(
        userId: String,
        itineraryId: String,
        activity: CreateActivityInput,
        replaceConflicts: Boolean,
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
            path = replacementPath(
                "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/activities",
                replaceConflicts,
            ),
            requestBody = requestBody,
        )
        parseItem(response.getJSONObject("activity")) as ActivityItem
    }

    override suspend fun addTransportationSegment(
        userId: String,
        itineraryId: String,
        segment: CreateTransportationInput,
    ): TransportationItem = addTransportationSegment(
        userId,
        itineraryId,
        segment,
        false,
    )

    override suspend fun addTransportationSegment(
        userId: String,
        itineraryId: String,
        segment: CreateTransportationInput,
        replaceConflicts: Boolean,
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
            path = replacementPath(
                "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/transportation-segments",
                replaceConflicts,
            ),
            requestBody = requestBody,
        )
        parseItem(response.getJSONObject("transportationSegment")) as TransportationItem
    }

    override suspend fun updateActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
        update: ActivityUpdate,
    ): ActivityItem = updateActivity(userId, itineraryId, activityId, update, false)

    override suspend fun updateActivity(
        userId: String,
        itineraryId: String,
        activityId: String,
        update: ActivityUpdate,
        replaceConflicts: Boolean,
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
            path = replacementPath(
                "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/activities/${encode(activityId)}",
                replaceConflicts,
            ),
            requestBody = requestBody,
        )
        parseItem(response.getJSONObject("activity")) as ActivityItem
    }

    override suspend fun updateTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
        update: TransportationSegmentUpdate,
    ): TransportationItem = updateTransportationSegment(
        userId,
        itineraryId,
        segmentId,
        update,
        false,
    )

    override suspend fun updateTransportationSegment(
        userId: String,
        itineraryId: String,
        segmentId: String,
        update: TransportationSegmentUpdate,
        replaceConflicts: Boolean,
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
            path = replacementPath(
                "${itineraryCollectionPath(userId)}/${encode(itineraryId)}/transportation-segments/${encode(segmentId)}",
                replaceConflicts,
            ),
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
        "/api/v1/users/${encode(userId)}/itineraries"

    private fun replacementPath(path: String, replaceConflicts: Boolean): String =
        if (replaceConflicts) "$path?replaceConflicts=true" else path

    private fun execute(
        method: String,
        path: String,
        requestBody: String? = null,
    ): JSONObject {
        val connection = URL("${apiBaseUrl.trimEnd('/')}$path")
            .openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("Accept", "application/json")
            if (requestBody != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { it.write(requestBody) }
            }

            val status = connection.responseCode
            val responseText = if (status in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (status !in 200..299) {
                val errorBody = runCatching {
                    JSONObject(responseText).getJSONObject("error")
                }.getOrNull()
                if (
                    status == HttpURLConnection.HTTP_CONFLICT &&
                    errorBody?.optString("code") == "ITINERARY_TIME_CONFLICT"
                ) {
                    val conflictsJson = errorBody.optJSONArray("conflicts") ?: JSONArray()
                    val conflicts = List(conflictsJson.length()) { index ->
                        val conflict = conflictsJson.getJSONObject(index)
                        ItineraryTimeConflict(
                            id = conflict.getString("id"),
                            type = conflict.getString("type"),
                            name = conflict.getString("name"),
                            startTime = conflict.getString("startTime"),
                            endTime = conflict.getString("endTime"),
                        )
                    }
                    throw ItineraryTimeConflictException(
                        errorBody.optString(
                            "message",
                            "The itinerary item overlaps with the existing schedule.",
                        ),
                        conflicts,
                    )
                }
                val message = runCatching {
                    errorBody?.getString("message")
                }.getOrNull()
                throw IOException(
                    message ?: "Itinerary request failed with HTTP $status"
                )
            }
            if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
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
