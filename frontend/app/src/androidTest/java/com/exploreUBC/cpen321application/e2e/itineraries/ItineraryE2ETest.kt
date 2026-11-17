package com.exploreUBC.cpen321application.e2e.itineraries

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.BuildConfig
import com.exploreUBC.cpen321application.MainActivity2
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesApi
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.data.itineraries.TransportationItem
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

class ItineraryE2ETest {
    private val composeRule = createAndroidComposeRule<MainActivity2>()

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(GrantLocationRule()).around(composeRule)

    private val api = ItinerariesApi(BuildConfig.API_BASE_URL)
    private val existingItineraryIds = mutableSetOf<String>()

    @Before
    fun startFromMainScreen() {
        runBlocking {
            existingItineraryIds += retryIo {
                api.getItineraries(APP_USER_ID, saved = true)
            }.map { it.id }
        }
        composeRule.waitForMainScreen()
        composeRule.openTab("Trips")
        composeRule.waitForTag("create_itinerary")
    }

    @After
    fun deleteItinerariesCreatedByTheTest() {
        runBlocking {
            retryIo { api.getItineraries(APP_USER_ID, saved = true) }
                .filter { it.id !in existingItineraryIds }
                .forEach { itinerary ->
                    runCatching { api.deleteItinerary(APP_USER_ID, itinerary.id) }
                }
        }
    }

    @Test
    // Section 4.2 — Create an itinerary: steps 1–3, saved through the backend.
    fun createItineraryFromTripsList() {
        composeRule.onNodeWithTag("create_itinerary").assertIsDisplayed().performClick()

        composeRule.waitForText("Itinerary details")
        composeRule.waitForText("Untitled Itinerary")
        org.junit.Assert.assertEquals("Untitled Itinerary", createdItinerary().name)
        composeRule.onNodeWithTag("itinerary_back").performClick()

        composeRule.waitForTag("create_itinerary")
        composeRule.onNodeWithText("Saved Itineraries").assertIsDisplayed()
    }

    @Test
    // Section 4.2 — Modify an itinerary: steps 1–4 and failure 3a–3a1.
    fun modifyItineraryAndValidateMaximumLength() {
        val updatedName = "new name ${System.currentTimeMillis()}"
        createItinerary()

        composeRule.onNodeWithTag("itinerary_details_edit").performClick()
        composeRule.onNodeWithTag("itinerary_name").performTextReplacement("a".repeat(101))
        composeRule.onNodeWithTag("itinerary_name_length_error").assertIsDisplayed()
        composeRule.onNodeWithText("Name must be at most 100 characters.").assertIsDisplayed()
        composeRule.onNodeWithTag("itinerary_save").assertIsNotEnabled()

        composeRule.onNodeWithTag("itinerary_name").performTextReplacement(updatedName)
        composeRule.onNodeWithTag("itinerary_description").performTextReplacement("new description")
        composeRule.onNodeWithTag("itinerary_starting_location").performTextReplacement("UBC nest")
        composeRule.onNodeWithTag("itinerary_save").performClick()

        composeRule.waitForText("Itinerary details saved.", substring = true)
        val saved = createdItinerary()
        org.junit.Assert.assertEquals(updatedName, saved.name)
        org.junit.Assert.assertEquals("new description", saved.description)
        org.junit.Assert.assertEquals("UBC nest", saved.startingLocation)
        composeRule.onNodeWithText(updatedName).assertIsDisplayed()
        composeRule.onNodeWithText("new description").assertIsDisplayed()
    }

    @Test
    // Section 4.2 — Add an activity: steps 1–4 and failures 3a–3b1.
    fun addActivityWithFrontendFailureScenarios() {
        createItinerary()
        composeRule.onNodeWithTag("itinerary_add_activity").performScrollTo().performClick()

        composeRule.onNodeWithText("New activity").assertIsDisplayed()
        composeRule.onNodeWithText("Name *").assertIsDisplayed()
        composeRule.onNodeWithText("Image URL (optional)").assertIsDisplayed()
        composeRule.onNodeWithText("Category *").assertIsDisplayed()
        composeRule.onNodeWithTag("new_item_create").assertIsNotEnabled()

        composeRule.onNodeWithTag("new_activity_category").performClick()
        listOf("Food", "Art", "Outdoors", "Markets", "Live music", "Gaming", "Museum", "Other")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithTag("new_activity_category").performClick()

        composeRule.onNodeWithTag("new_activity_name").performTextReplacement("a".repeat(201))
        composeRule.onNodeWithTag("new_activity_name_length_error").assertIsDisplayed()
        composeRule.onNodeWithTag("new_item_create").assertIsNotEnabled()

        composeRule.onNodeWithTag("new_activity_name").performTextReplacement("new activity")
        composeRule.onNodeWithTag("new_activity_category").performClick()
        composeRule.onNodeWithText("Outdoors").performClick()
        composeRule.onNodeWithTag("new_activity_location").performTextReplacement("UBC nest")
        setDateTime("new_activity_start_time", hour = 2)
        setDateTime("new_activity_end_time", hour = 1)
        composeRule.onNodeWithTag("time_order_error").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("new_item_create").assertIsNotEnabled()

        setDateTime("new_activity_end_time", hour = 3)
        composeRule.onNodeWithTag("new_item_create").assertIsEnabled().performClick()

        composeRule.waitForText("Activity added.", substring = true)
        composeRule.onNodeWithText("new activity").assertIsDisplayed()
        val activity = createdItinerary().items.filterIsInstance<ActivityItem>().single()
        org.junit.Assert.assertEquals("new activity", activity.name)
        org.junit.Assert.assertEquals("outdoors", activity.category)
        org.junit.Assert.assertEquals("UBC nest", activity.location)
    }

    @Test
    // Section 4.2 — Add transportation: steps 1–4 and failures 3a–3c1.
    fun addTransportationWithFrontendFailureScenarios() {
        createItinerary()
        composeRule.onNodeWithTag("itinerary_add_transportation").performScrollTo().performClick()

        composeRule.onNodeWithText("New transportation").assertIsDisplayed()
        composeRule.onNodeWithTag("new_item_create").assertIsNotEnabled()
        composeRule.onNodeWithTag("new_transportation_mode").performClick()
        listOf("Walking", "Cycling", "Transit", "Driving", "Other")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("Walking").performClick()
        composeRule.onNodeWithTag("new_transportation_start_location")
            .performTextReplacement("UBC Nest")
        composeRule.onNodeWithTag("new_transportation_end_location")
            .performTextReplacement("UBC MCLD")

        composeRule.onNodeWithTag("new_transportation_distance").performTextReplacement("abc")
        composeRule.onNodeWithTag("distance_error").assertIsDisplayed()
        composeRule.onNodeWithTag("new_item_create").assertIsNotEnabled()
        composeRule.onNodeWithTag("new_transportation_distance").performTextReplacement("400")

        composeRule.onNodeWithTag("new_transportation_start_location")
            .performTextReplacement("a".repeat(501))
        composeRule.onNodeWithTag("new_transportation_start_location_length_error")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("new_item_create").assertIsNotEnabled()
        composeRule.onNodeWithTag("new_transportation_start_location")
            .performTextReplacement("UBC Nest")

        setDateTime("new_transportation_start_time", hour = 2)
        setDateTime("new_transportation_end_time", hour = 1)
        composeRule.onNodeWithTag("time_order_error").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("new_item_create").assertIsNotEnabled()
        setDateTime("new_transportation_end_time", hour = 3)

        composeRule.onNodeWithTag("new_item_create").assertIsEnabled().performClick()
        composeRule.waitForText("Transportation added.", substring = true)
        composeRule.onNodeWithText("Walking").assertIsDisplayed()
        composeRule.onNodeWithText("Transportation").assertIsDisplayed()
        val segment = createdItinerary().items.filterIsInstance<TransportationItem>().single()
        org.junit.Assert.assertEquals("walking", segment.mode)
        org.junit.Assert.assertEquals(400.0, segment.distanceMeters, 0.0)
    }

    @Test
    fun modifyAndRemoveActivity() {
        createItinerary()
        addValidActivity()
        composeRule.onNodeWithText("new activity").performClick()
        composeRule.waitForTag("itinerary_item_edit")
        composeRule.onNodeWithTag("itinerary_item_edit").performClick()

        composeRule.onNodeWithText("Name *").assertIsDisplayed()
        composeRule.onNodeWithText("Image URL (optional)").assertIsDisplayed()
        composeRule.onNodeWithText("Category *").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_activity_name").performTextReplacement("edited activity")
        composeRule.onNodeWithTag("edit_activity_image").performTextReplacement("")
        composeRule.onNodeWithTag("edit_activity_category").performClick()
        composeRule.onNodeWithText("Art").performClick()
        composeRule.onNodeWithTag("edit_activity_location").performTextReplacement("UBC Nest")
        composeRule.onNodeWithTag("edit_activity_save").performScrollTo().performClick()

        composeRule.waitForText("Activity saved.", substring = true)
        composeRule.onNodeWithTag("itinerary_item_delete").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete item?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_item_delete").performClick()

        composeRule.waitForText("Item deleted.", substring = true)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("edited activity").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun modifyAndRemoveTransportation() {
        createItinerary()
        addValidTransportation()
        composeRule.onNodeWithText("Walking").performClick()
        composeRule.waitForTag("itinerary_item_edit")
        composeRule.onNodeWithTag("itinerary_item_edit").performClick()

        composeRule.onNodeWithTag("edit_transportation_distance").performTextReplacement("abc")
        composeRule.onNodeWithTag("distance_error").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_transportation_save").assertIsNotEnabled()
        composeRule.onNodeWithTag("edit_transportation_distance").performTextReplacement("400")
        composeRule.onNodeWithTag("edit_transportation_start_location")
            .performTextReplacement("a".repeat(501))
        composeRule.onNodeWithTag("edit_transportation_start_location_length_error")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edit_transportation_start_location")
            .performTextReplacement("UBC Nest")
        composeRule.onNodeWithTag("edit_transportation_end_location")
            .performTextReplacement("UBC Exchange")
        composeRule.onNodeWithTag("edit_transportation_mode").performClick()
        composeRule.onNodeWithText("Transit").performClick()
        setDateTime("edit_transportation_start_time", hour = 6)
        composeRule.onNodeWithTag("time_order_error").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edit_transportation_save").assertIsNotEnabled()
        setDateTime("edit_transportation_start_time", hour = 1, period = "AM")
        composeRule.onNodeWithTag("edit_transportation_save").performScrollTo().performClick()

        composeRule.waitForText("Transportation saved.", substring = true)
        composeRule.onNodeWithTag("itinerary_item_delete").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete item?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_item_delete").performClick()

        composeRule.waitForText("Item deleted.", substring = true)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Transit").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    // Section 4.2 — Delete an itinerary: steps 1–3, including cancellation.
    fun deleteItineraryRequiresConfirmation() {
        createItinerary()
        composeRule.onNodeWithTag("itinerary_delete").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete itinerary?").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_delete_itinerary").performClick()
        composeRule.onNodeWithTag("itinerary_delete").assertIsDisplayed()

        composeRule.onNodeWithTag("itinerary_delete").performClick()
        composeRule.onNodeWithTag("confirm_delete_itinerary").performClick()

        composeRule.waitForTag("create_itinerary")
        composeRule.waitForText("Itinerary deleted.", substring = true)
        val remaining = runBlocking {
            retryIo { api.getItineraries(APP_USER_ID, saved = true) }.map { it.id }
        }
        org.junit.Assert.assertTrue(remaining.none { it !in existingItineraryIds })
    }

    private fun createdItinerary(): Itinerary = runBlocking {
        retryIo { api.getItineraries(APP_USER_ID, saved = true) }
            .last { it.id !in existingItineraryIds }
    }

    private fun createItinerary() {
        composeRule.onNodeWithTag("create_itinerary").performClick()
        composeRule.waitForTag("itinerary_details_edit")
    }

    private fun addValidActivity() {
        composeRule.onNodeWithTag("itinerary_add_activity").performScrollTo().performClick()
        composeRule.onNodeWithTag("new_activity_name").performTextReplacement("new activity")
        composeRule.onNodeWithTag("new_activity_category").performClick()
        composeRule.onNodeWithText("Outdoors").performClick()
        composeRule.onNodeWithTag("new_activity_location").performTextReplacement("UBC nest")
        setDateTime("new_activity_start_time", hour = 2)
        setDateTime("new_activity_end_time", hour = 3)
        composeRule.onNodeWithTag("new_item_create").performScrollTo().performClick()
        composeRule.waitForText("Activity added.", substring = true)
    }

    private fun addValidTransportation() {
        composeRule.onNodeWithTag("itinerary_add_transportation").performScrollTo().performClick()
        composeRule.onNodeWithTag("new_transportation_mode").performClick()
        composeRule.onNodeWithText("Walking").performClick()
        composeRule.onNodeWithTag("new_transportation_start_location")
            .performTextReplacement("UBC Nest")
        composeRule.onNodeWithTag("new_transportation_end_location")
            .performTextReplacement("UBC MCLD")
        composeRule.onNodeWithTag("new_transportation_distance").performTextReplacement("400")
        setDateTime("new_transportation_start_time", hour = 4)
        setDateTime("new_transportation_end_time", hour = 5)
        composeRule.onNodeWithTag("new_item_create").performScrollTo().performClick()
        composeRule.waitForText("Transportation added.", substring = true)
    }

    private fun setDateTime(tag: String, hour: Int, period: String = "PM") {
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithContentDescription("$hour o'clock").performClick()
        composeRule.onAllNodesWithContentDescription("0 minutes").onLast().performClick()
        composeRule.onNodeWithText(period).performClick()
        composeRule.onNodeWithText("OK").performClick()
    }
}
