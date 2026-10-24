package com.exploreUBC.cpen321application.data.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.exploreUBC.cpen321application.data.network.ApiRequest
import com.exploreUBC.cpen321application.data.network.BackendClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventsApiTest {
    @Test
    fun getEventsUsesProtectedSharedClientRequest() = runBlocking {
        lateinit var request: ApiRequest
        val api = EventsApi(
            BackendClient {
                request = it
                """
                    {
                      "events": [{
                        "id": "event-id",
                        "title": "Campus Event",
                        "description": null,
                        "imageUrl": "https://example.test/image.jpg",
                        "startDateTime": "2026-10-24T20:00:00.000Z",
                        "venueName": "The Nest",
                        "city": "Vancouver",
                        "category": "music",
                        "ticketUrl": "https://example.test/tickets",
                        "priceMin": null,
                        "priceMax": null,
                        "currency": null,
                        "distanceKm": 1.25
                      }]
                    }
                """.trimIndent()
            },
        )

        val events = api.getEvents(
            userId = "user-id",
            latitude = 49.2606,
            longitude = -123.246,
            page = 2,
            limit = 10,
        )

        assertEquals("GET", request.method)
        assertTrue(request.requiresAuthentication)
        assertEquals(
            "/users/user-id/events" +
                "?latitude=49.2606&longitude=-123.246&page=2&limit=10",
            request.path,
        )
        assertEquals("Campus Event", events.single().title)
    }
}
