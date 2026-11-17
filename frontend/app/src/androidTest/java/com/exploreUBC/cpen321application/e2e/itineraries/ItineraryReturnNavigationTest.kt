package com.exploreUBC.cpen321application.e2e.itineraries

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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class ItineraryReturnNavigationTest {
    private val composeRule = createAndroidComposeRule<MainActivity2>()

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(GrantLocationRule()).around(composeRule)

    private val api = ItinerariesApi(BuildConfig.API_BASE_URL)
    private var itineraryId: String? = null

    @Before
    fun createItineraryOnTheBackend() {
        val name = "Map return ${System.currentTimeMillis()}"
        itineraryId = runBlocking {
            retryIo { api.createItinerary(APP_USER_ID, CreateItineraryInput(name = name)) }.id
        }
    }

    @After
    fun deleteItinerary() {
        val id = itineraryId ?: return
        runBlocking { runCatching { api.deleteItinerary(APP_USER_ID, id) } }
    }

    @Test
    fun returningFromMapOpensTheRequestedItinerary() {
        val id = checkNotNull(itineraryId)
        val name = runBlocking { retryIo { api.getItinerary(APP_USER_ID, id) }.name }
        composeRule.waitForMainScreen()
        composeRule.openTab("Map")
        composeRule.waitForTag("map_itinerary_search")
        composeRule.onNodeWithTag("map_itinerary_search").performTextReplacement(name)
        composeRule.waitForTag("map_itinerary_$id")
        composeRule.onNodeWithTag("map_itinerary_$id").performClick()
        composeRule.waitForTag("map_visit_itinerary")
        composeRule.onNodeWithTag("map_visit_itinerary").performClick()

        composeRule.waitForTag("itinerary_details_edit")
        composeRule.onNodeWithText(name).assertIsDisplayed()
    }
}
