package com.exploreUBC.cpen321application.data.users

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
class UsersApiTest {
    private val userResponse = """
        {
          "user": {
            "id": "507f1f77bcf86cd799439011",
            "googleSubject": "google-subject",
            "email": "person@example.com",
            "displayName": "Example Person",
            "start_location": "UBC Exchange",
            "preferences": {
              "transportation": { "mode": "transit" },
              "budget": "balanced",
              "interest": "art"
            },
            "create_at": "2026-10-24T08:00:00.000Z",
            "updated_at": "2026-10-24T08:00:00.000Z"
          }
        }
    """.trimIndent()

    @Test
    fun getUserUsesProtectedApiClientRequest() = runBlocking {
        val client = RecordingBackendClient(userResponse)
        val usersApi = UsersApi(client)

        val user = usersApi.getUser("507f1f77bcf86cd799439011")

        val request = client.requests.single()
        assertEquals("GET", request.method)
        assertEquals("/users/507f1f77bcf86cd799439011", request.path)
        assertNull(request.body)
        assertTrue(request.requiresAuthentication)
        assertEquals("Example Person", user.displayName)
        assertEquals(TransportationMode.TRANSIT, user.preferences.transportation.mode)
    }

    @Test
    fun updateUserUsesProtectedApiClientRequest() = runBlocking {
        val client = RecordingBackendClient(userResponse)
        val usersApi = UsersApi(client)

        usersApi.updateUser(
            userId = "507f1f77bcf86cd799439011",
            update = UserUpdate(
                startLocation = "AMS Nest",
                preferences = UserPreferences(
                    transportation = TransportationPreference(TransportationMode.WALKING),
                    budget = BudgetPreference.ECONOMICAL,
                    interest = InterestPreference.FOOD,
                ),
            ),
        )

        val request = client.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("/users/507f1f77bcf86cd799439011", request.path)
        assertTrue(request.requiresAuthentication)
        val body = JSONObject(request.body!!)
        assertEquals("AMS Nest", body.getString("start_location"))
        assertEquals(
            "walking",
            body.getJSONObject("preferences")
                .getJSONObject("transportation")
                .getString("mode"),
        )
    }
}

private class RecordingBackendClient(
    responses: String,
) : BackendClient {
    private val responses = ArrayDeque(listOf(responses))
    val requests = mutableListOf<ApiRequest>()

    override suspend fun execute(request: ApiRequest): String {
        requests += request
        return responses.removeFirst()
    }
}
