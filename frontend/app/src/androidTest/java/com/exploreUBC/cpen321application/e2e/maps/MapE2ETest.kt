package com.exploreUBC.cpen321application.e2e.maps

import android.widget.TextView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.exploreUBC.cpen321application.MainActivity2
import com.exploreUBC.cpen321application.R
import com.exploreUBC.cpen321application.data.itineraries.CreateActivityInput
import com.exploreUBC.cpen321application.data.itineraries.CreateItineraryInput
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesApi
import com.exploreUBC.cpen321application.testutil.REAL_BACKEND_USER_ID
import com.exploreUBC.cpen321application.testutil.grantLocationPermissionRule
import com.exploreUBC.cpen321application.testutil.realBackendClient
import com.exploreUBC.cpen321application.testutil.signedInTestUserRule
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class MapE2ETest {
    private val locationPermissionRule = grantLocationPermissionRule()
    private val composeRule = createAndroidComposeRule<MainActivity2>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(locationPermissionRule)
        .around(signedInTestUserRule())
        .around(composeRule)

    private lateinit var itinerariesApi: ItinerariesApi
    private lateinit var fixtureName: String
    private var fixtureId: String? = null

    @Before
    fun createBackendFixture() {
        itinerariesApi = ItinerariesApi(realBackendClient())
        fixtureName = "Map E2E ${UUID.randomUUID().toString().take(8)}"
        runBlocking {
            val itinerary = itinerariesApi.createItinerary(
                REAL_BACKEND_USER_ID,
                CreateItineraryInput(
                    name = fixtureName,
                    description = "Created by the non-mocked map E2E test.",
                ),
            )
            fixtureId = itinerary.id
            itinerariesApi.addActivity(
                REAL_BACKEND_USER_ID,
                itinerary.id,
                CreateActivityInput(
                    name = FIRST_ACTIVITY,
                    category = "food",
                    location = "UBC Nest, Vancouver, BC",
                    startTime = "2027-11-30T18:00:00.000Z",
                    endTime = "2027-11-30T19:00:00.000Z",
                ),
            )
            itinerariesApi.addActivity(
                REAL_BACKEND_USER_ID,
                itinerary.id,
                CreateActivityInput(
                    name = SECOND_ACTIVITY,
                    category = "museum",
                    location = "Museum of Anthropology at UBC, Vancouver, BC",
                    startTime = "2027-11-30T20:00:00.000Z",
                    endTime = "2027-11-30T21:00:00.000Z",
                ),
            )
            itinerariesApi.addActivity(
                REAL_BACKEND_USER_ID,
                itinerary.id,
                CreateActivityInput(
                    name = THIRD_ACTIVITY,
                    category = "park",
                    location = "Rose Garden, Vancouver, BC",
                    startTime = "2027-11-30T22:00:00.000Z",
                    endTime = "2027-11-30T23:00:00.000Z",
                ),
            )
        }
        waitForTag("navigation_Map")
    }

    @After
    fun removeBackendFixture() {
        val itineraryId = fixtureId ?: return
        runCatching {
            runBlocking {
                itinerariesApi.deleteItinerary(REAL_BACKEND_USER_ID, itineraryId)
            }
        }
    }

    @Test
    fun viewSavedItineraryRoutesAndDirectionsFromMainApp() {
        composeRule.onNodeWithTag("navigation_Map").performClick()
        waitForTag("map_itinerary_search")

        composeRule.onNodeWithTag("map_itinerary_search")
            .performTextReplacement(fixtureName)
        val itineraryId = checkNotNull(fixtureId)
        waitForTag("map_itinerary_$itineraryId")
        composeRule.onNodeWithTag("map_itinerary_$itineraryId").performClick()

        waitForTag("map_route_dropdown", MAP_TIMEOUT_MILLIS)
        composeRule.onNodeWithText(fixtureName).assertIsDisplayed()
        composeRule.onNodeWithTag("map_travel_walk").assertIsDisplayed()
        composeRule.onNodeWithTag("map_travel_drive").assertIsDisplayed()
        composeRule.onNodeWithTag("map_directions_visibility").assertIsDisplayed()
        composeRule.onNodeWithTag("map_visit_itinerary").assertIsDisplayed()
        composeRule.onNodeWithTag("map_route_dropdown").performClick()
        waitForTag("map_route_1")
        composeRule.onNodeWithTag("map_route_1").performClick()
        composeRule.onNodeWithText("$SECOND_ACTIVITY → $THIRD_ACTIVITY").assertIsDisplayed()

        composeRule.onNodeWithTag("map_travel_walk").performClick()
        composeRule.onNodeWithTag("map_travel_summary")
            .assertTextContains("Walk", substring = true)
        composeRule.onNodeWithTag("map_direction_step_0").assertIsDisplayed()

        composeRule.onNodeWithTag("map_travel_drive").performClick()
        composeRule.onNodeWithTag("map_travel_summary")
            .assertTextContains("Drive", substring = true)
        composeRule.onNodeWithTag("map_direction_step_0").assertIsDisplayed()

        composeRule.onNodeWithTag("map_visit_itinerary").performClick()
        waitForTag("itinerary_details_edit")
        composeRule.onNodeWithText(fixtureName).assertIsDisplayed()
    }

    @Test
    fun browseAndTogglePopularLocationsFromMainApp() {
        composeRule.onNodeWithTag("navigation_Map").performClick()
        waitForTag("map_popular_locations_checkbox")

        composeRule.onNodeWithTag("map_popular_locations_checkbox").assertIsOn()
        assertPopularPlacesOutcome()

        composeRule.onNodeWithTag("map_popular_locations_checkbox").performClick()
        composeRule.onNodeWithTag("map_popular_locations_checkbox").assertIsOff()
        waitForMapStatus(R.string.map_popular_hidden)

        composeRule.onNodeWithTag("map_popular_locations_checkbox").performClick()
        composeRule.onNodeWithTag("map_popular_locations_checkbox").assertIsOn()
        assertPopularPlacesOutcome()
    }

    private fun waitForTag(tag: String, timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertPopularPlacesOutcome() {
        val status = waitForMapStatus(
            R.string.map_popular_ready,
            R.string.map_popular_empty,
            R.string.map_popular_failed,
            R.string.map_tiles_failed,
        )
        if (status == composeRule.activity.getString(R.string.map_popular_ready) ||
            status == composeRule.activity.getString(R.string.map_popular_empty)
        ) {
            onView(withId(R.id.map)).check(matches(isDisplayed()))
        }
    }

    private fun waitForMapStatus(vararg expectedIds: Int): String {
        val expected = expectedIds.map(composeRule.activity::getString)
        composeRule.waitUntil(MAP_TIMEOUT_MILLIS) {
            runCatching { mapStatusText() in expected }.getOrDefault(false)
        }
        return mapStatusText()
    }

    private fun mapStatusText(): String {
        var text = ""
        onView(withId(R.id.text_map_status)).check { view, error ->
            if (error != null) throw error
            text = (view as TextView).text.toString()
        }
        return text
    }

    private companion object {
        const val FIRST_ACTIVITY = "E2E Nest Lunch"
        const val SECOND_ACTIVITY = "E2E Museum Visit"
        const val THIRD_ACTIVITY = "E2E Rose Garden Walk"
        const val DEFAULT_TIMEOUT_MILLIS = 20_000L
        const val MAP_TIMEOUT_MILLIS = 90_000L
    }
}
