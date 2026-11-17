package com.exploreUBC.cpen321application.e2e.maps.mock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.BuildConfig
import com.exploreUBC.cpen321application.MainActivity2
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

class ViewItineraryMapMockTest {
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
            createdItineraryIds.forEach { id ->
                runCatching { api.deleteItinerary(APP_USER_ID, id) }
            }
        }
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 2a, from the main screen.
    fun reportsWhenNoSavedItineraryMatches() {
        composeRule.openTab("Map")
        composeRule.waitForTag("map_itinerary_search")
        composeRule.onNodeWithTag("map_itinerary_search")
            .performTextReplacement("missing trip ${System.currentTimeMillis()}")
        composeRule.onNodeWithText("No saved itineraries match your search.").assertIsDisplayed()
    }

    @Test
    // Section 4.2 — View Itinerary on Map failure 2b, loaded from the backend.
    fun reportsWhenItineraryHasNothingToPlot() {
        val name = "Empty map ${System.currentTimeMillis()}"
        val itineraryId = runBlocking {
            val itinerary = retryIo {
                api.createItinerary(APP_USER_ID, CreateItineraryInput(name = name))
            }
            createdItineraryIds += itinerary.id
            itinerary.id
        }

        composeRule.openTab("Map")
        composeRule.waitForTag("map_itinerary_search")
        composeRule.onNodeWithTag("map_itinerary_search").performTextReplacement(name)
        composeRule.waitForTag("map_itinerary_$itineraryId")
        composeRule.onNodeWithTag("map_itinerary_$itineraryId").performClick()
        composeRule.waitForText(
            "No routes were returned. Pins are shown without route lines.",
            timeoutMillis = 20_000,
        )
    }
}
