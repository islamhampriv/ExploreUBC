package com.exploreUBC.cpen321application.e2e.maps.nomock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.BuildConfig
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateItineraryInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesApi
import com.exploreUBC.cpen321application.ui.map.MapRoute
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ViewItineraryMapNoMockTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val api = ItinerariesApi(BuildConfig.API_BASE_URL)
    private val createdItineraryIds = mutableListOf<String>()

    @After
    fun deleteCreatedItineraries() {
        runBlocking {
            createdItineraryIds.forEach { itineraryId ->
                runCatching { api.deleteItinerary(USER_ID, itineraryId) }
            }
        }
    }

    @Test
    // Section 4.2 — View Itinerary on Map success against the running backend and Google Routes.
    fun showsDirectionsFromBackendMap() {
        val name = "Map E2E ${System.currentTimeMillis()}"
        val itineraryId = withBackend {
            val itinerary = api.createItinerary(
                USER_ID,
                CreateItineraryInput(name = name),
            )
            createdItineraryIds += itinerary.id
            api.addActivity(USER_ID, itinerary.id, activity("Museum of Anthropology", MUSEUM))
            api.addActivity(USER_ID, itinerary.id, activity("Walter Gage", NEST))
            itinerary.id
        }

        showMap()
        composeRule.onNodeWithTag("map_itinerary_search").performTextReplacement(name)
        composeRule.onNodeWithTag("map_itinerary_$itineraryId").performClick()

        waitForText("min", timeoutMillis = 45_000)
        composeRule.onNodeWithTag("map_travel_walk").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("map_travel_drive").assertIsDisplayed()
        waitForText("min")
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 2b against the running backend.
    fun reportsWhenBackendItineraryHasNoActivities() {
        val itineraryId = withBackend {
            val itinerary = api.createItinerary(
                USER_ID,
                CreateItineraryInput(name = "Empty map ${System.currentTimeMillis()}"),
            )
            createdItineraryIds += itinerary.id
            itinerary.id
        }

        showMap(selectedItineraryId = itineraryId)
        waitForText(
            "No routes were returned. Pins are shown without route lines.",
            timeoutMillis = 20_000,
        )
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 5a: an invalid stop does not fail the map.
    fun keepsMapAvailableWhenOneLocationIsInvalid() {
        val itineraryId = withBackend {
            val itinerary = api.createItinerary(
                USER_ID,
                CreateItineraryInput(name = "Partial map ${System.currentTimeMillis()}"),
            )
            createdItineraryIds += itinerary.id
            api.addActivity(
                USER_ID,
                itinerary.id,
                activity("Unclear stop", "zzzz-not-a-place-99999"),
            )
            api.addActivity(USER_ID, itinerary.id, activity("Museum of Anthropology", MUSEUM))
            itinerary.id
        }

        showMap(selectedItineraryId = itineraryId)
        composeRule.waitUntil(timeoutMillis = 45_000) {
            composeRule.onAllNodesWithTag("map_travel_walk").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText(
                    "No routes were returned. Pins are shown without route lines.",
                ).fetchSemanticsNodes().isNotEmpty()
        }
        org.junit.Assert.assertTrue(
            composeRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure: the itinerary id is not stored.
    fun reportsWhenBackendItineraryIsMissing() {
        showMap(selectedItineraryId = MISSING_ITINERARY_ID)
        waitForText("This itinerary no longer exists.", timeoutMillis = 20_000)
    }

    private fun showMap(selectedItineraryId: String? = null) {
        composeRule.setContent {
            var selectedId by remember { mutableStateOf(selectedItineraryId) }
            MapRoute(
                userId = USER_ID,
                itinerariesRepository = api,
                selectedItineraryId = selectedId,
                onItinerarySelected = { selectedId = it },
                showMap = false,
            )
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("map_itinerary_search").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun <T> withBackend(block: suspend () -> T): T = runBlocking {
        retryIo(block)
    }

    private suspend fun <T> retryIo(block: suspend () -> T): T {
        var lastError: IOException? = null
        for (attempt in 1..3) {
            try {
                return block()
            } catch (error: IOException) {
                lastError = error
                if (attempt < 3) delay(1_000)
            }
        }
        throw lastError ?: IOException("Backend request failed")
    }

    private fun activity(name: String, location: String) = CreateActivityInput(
        name = name,
        image = "",
        description = null,
        category = "outdoors",
        location = location,
        startTime = if (name.startsWith("Museum")) {
            "2027-11-30T18:00:00.000Z"
        } else {
            "2027-11-30T20:00:00.000Z"
        },
        endTime = if (name.startsWith("Museum")) {
            "2027-11-30T19:00:00.000Z"
        } else {
            "2027-11-30T21:00:00.000Z"
        },
    )
}

private const val USER_ID = "6705f00000000000000000aa"
private const val MISSING_ITINERARY_ID = "6705f1000000000000000999"
private const val MUSEUM = "6393 NW Marine Dr, Vancouver, BC, Canada"
private const val NEST = "5959 Student Union Blvd, Vancouver, BC, Canada"
