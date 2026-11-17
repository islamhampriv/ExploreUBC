package com.exploreUBC.cpen321application.e2e.maps.nomock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.BuildConfig
import com.exploreUBC.cpen321application.MainActivity2
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateItineraryInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesApi
import com.exploreUBC.cpen321application.e2e.APP_USER_ID
import com.exploreUBC.cpen321application.e2e.GrantLocationRule
import com.exploreUBC.cpen321application.e2e.openTab
import com.exploreUBC.cpen321application.e2e.retryIo
import com.exploreUBC.cpen321application.e2e.waitForMainScreen
import com.exploreUBC.cpen321application.e2e.waitForTag
import com.exploreUBC.cpen321application.e2e.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class ViewItineraryMapNoMockTest {
    private val composeRule = createAndroidComposeRule<MainActivity2>()

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(GrantLocationRule()).around(composeRule)

    private val api = ItinerariesApi(BuildConfig.API_BASE_URL)
    private val createdItineraryIds = mutableListOf<String>()

    @Before
    fun startFromMainScreen() {
        composeRule.waitForMainScreen()
    }

    @After
    fun deleteCreatedItineraries() {
        runBlocking {
            createdItineraryIds.forEach { itineraryId ->
                runCatching { api.deleteItinerary(APP_USER_ID, itineraryId) }
            }
        }
    }

    @Test
    // Section 4.2 — View Itinerary on Map success from the main screen.
    fun showsDirectionsFromBackendMap() {
        val name = "Map E2E ${System.currentTimeMillis()}"
        val itineraryId = runBlocking {
            val itinerary = retryIo {
                api.createItinerary(APP_USER_ID, CreateItineraryInput(name = name))
            }
            createdItineraryIds += itinerary.id
            retryIo { api.addActivity(APP_USER_ID, itinerary.id, activity("Museum of Anthropology", MUSEUM)) }
            retryIo { api.addActivity(APP_USER_ID, itinerary.id, activity("Walter Gage", NEST)) }
            itinerary.id
        }

        openItineraryMap(name, itineraryId)
        composeRule.waitForText("min", timeoutMillis = 45_000, substring = true)
        composeRule.onNodeWithTag("map_travel_walk").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("map_travel_drive").assertIsDisplayed()
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 5a from the main screen.
    fun keepsMapAvailableWhenOneLocationIsInvalid() {
        val name = "Partial map ${System.currentTimeMillis()}"
        val itineraryId = runBlocking {
            val itinerary = retryIo {
                api.createItinerary(APP_USER_ID, CreateItineraryInput(name = name))
            }
            createdItineraryIds += itinerary.id
            retryIo {
                api.addActivity(APP_USER_ID, itinerary.id, activity("Unclear stop", "zzzz-not-a-place-99999"))
            }
            retryIo { api.addActivity(APP_USER_ID, itinerary.id, activity("Museum of Anthropology", MUSEUM)) }
            itinerary.id
        }

        openItineraryMap(name, itineraryId)
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

    private fun openItineraryMap(name: String, itineraryId: String) {
        composeRule.openTab("Map")
        composeRule.waitForTag("map_itinerary_search")
        composeRule.onNodeWithTag("map_itinerary_search").performTextReplacement(name)
        composeRule.waitForTag("map_itinerary_$itineraryId")
        composeRule.onNodeWithTag("map_itinerary_$itineraryId").performClick()
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

private const val MUSEUM = "6393 NW Marine Dr, Vancouver, BC, Canada"
private const val NEST = "5959 Student Union Blvd, Vancouver, BC, Canada"
