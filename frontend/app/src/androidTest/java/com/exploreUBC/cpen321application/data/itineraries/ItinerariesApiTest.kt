package com.exploreUBC.cpen321application.data.itineraries

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.exploreUBC.cpen321application.data.network.ApiRequest
import com.exploreUBC.cpen321application.data.network.BackendClient
import java.util.ArrayDeque
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItinerariesApiTest {
    private val itinerary = """
        {
          "id": "itinerary-id",
          "ownerId": "user-id",
          "name": "Campus Day",
          "description": null,
          "startingLocation": "The Nest",
          "items": [],
          "createdAt": "2026-10-24T08:00:00.000Z",
          "updatedAt": "2026-10-24T08:00:00.000Z",
          "saved": true,
          "inRecommendation": false,
          "score": {}
        }
    """.trimIndent()

    @Test
    fun collectionReadUsesProtectedSharedClientRequest() = runBlocking {
        val client = RecordingItineraryClient("{\"itineraries\":[$itinerary]}")
        val api = ItinerariesApi(client)

        val result = api.getItineraries("user-id", saved = true)

        val request = client.requests.single()
        assertEquals("GET", request.method)
        assertEquals("/users/user-id/itineraries?saved=true", request.path)
        assertNull(request.body)
        assertTrue(request.requiresAuthentication)
        assertEquals("Campus Day", result.single().name)
    }

    @Test
    fun mutationUsesProtectedSharedClientRequest() = runBlocking {
        val client = RecordingItineraryClient("{\"itinerary\":$itinerary}")
        val api = ItinerariesApi(client)

        api.createItinerary(
            userId = "user-id",
            input = CreateItineraryInput(name = "Campus Day"),
        )

        val request = client.requests.single()
        assertEquals("POST", request.method)
        assertEquals("/users/user-id/itineraries", request.path)
        assertTrue(request.requiresAuthentication)
        assertEquals("Campus Day", JSONObject(request.body!!).getString("name"))
    }
}

private class RecordingItineraryClient(vararg responses: String) : BackendClient {
    private val responses = ArrayDeque(responses.toList())
    val requests = mutableListOf<ApiRequest>()

    override suspend fun execute(request: ApiRequest): String {
        requests += request
        return responses.removeFirst()
    }
}
