package com.exploreUBC.cpen321application.e2e.itineraries

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.ui.trips.TripsRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItineraryE2ETest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: InMemoryItinerariesRepository

    @Before
    fun setUp() {
        repository = InMemoryItinerariesRepository()
        composeRule.setContent {
            TripsRoute(
                userId = E2E_USER_ID,
                itinerariesRepository = repository,
            )
        }
        waitForTag("saved_itinerary_$E2E_ITINERARY_ID")
    }

    @Test
    // Section 4.2 — Create an itinerary: steps 1–3.
    fun createItineraryFromTripsList() {
        composeRule.onNodeWithTag("create_itinerary").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Itinerary details").assertIsDisplayed()
        composeRule.onNodeWithText("Untitled Itinerary").assertIsDisplayed()
        composeRule.onNodeWithTag("itinerary_back").performClick()

        composeRule.onNodeWithText("Saved Itineraries").assertIsDisplayed()
        composeRule.onNodeWithText("Untitled Itinerary").assertIsDisplayed()
        assertTrue(repository.itineraries.any { it.name == "Untitled Itinerary" })
    }

    @Test
    // Section 4.2 — Modify an itinerary: steps 1–4 and failure 3a–3a1.
    fun modifyItineraryAndValidateMaximumLength() {
        openItinerary()
        composeRule.onNodeWithTag("itinerary_details_edit").performClick()

        composeRule.onNodeWithTag("itinerary_name")
            .performTextReplacement("a".repeat(101))
        composeRule.onNodeWithTag("itinerary_name_length_error").assertIsDisplayed()
        composeRule.onNodeWithText("Name must be at most 100 characters.").assertIsDisplayed()
        composeRule.onNodeWithTag("itinerary_save").assertIsNotEnabled()

        composeRule.onNodeWithTag("itinerary_name").performTextReplacement("new name")
        composeRule.onNodeWithTag("itinerary_description")
            .performTextReplacement("new description")
        composeRule.onNodeWithTag("itinerary_starting_location")
            .performTextReplacement("UBC nest")
        composeRule.onNodeWithTag("itinerary_save").performClick()

        composeRule.onNodeWithTag("itinerary_status")
            .assertTextContains("Itinerary details saved.")
        composeRule.onNodeWithTag("itinerary_details_edit").performClick()
        composeRule.onNodeWithTag("itinerary_name").assertTextContains("new name")
    }

    @Test
    // Section 4.2 — Add an activity: steps 1–4 and failures 3a–3b1.
    fun addActivityWithFrontendFailureScenarios() {
        openItinerary()
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

        composeRule.onNodeWithTag("new_activity_name")
            .performTextReplacement("a".repeat(201))
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

        composeRule.onNodeWithTag("itinerary_status").assertTextContains("Activity added.")
        composeRule.onNodeWithText("new activity").assertIsDisplayed()
        assertTrue(repository.itineraries.single().items.any {
            it is com.exploreUBC.cpen321application.data.itineraries.ActivityItem &&
                it.name == "new activity" && it.image.isEmpty() && it.category == "outdoors"
        })
    }

    @Test
    // Section 4.2 — Add transportation: steps 1–4 and failures 3a–3c1.
    fun addTransportationWithFrontendFailureScenarios() {
        openItinerary()
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

        composeRule.onNodeWithTag("new_transportation_distance")
            .performTextReplacement("abc")
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
        composeRule.onNodeWithTag("itinerary_status")
            .assertTextContains("Transportation added.")
        composeRule.onNodeWithTag("itinerary_item_transportation-2").assertIsDisplayed()
    }

    @Test
    fun modifyAndRemoveActivity() {
        // Section 4.2 — Modify an activity: steps 1–4.
        openItinerary()
        composeRule.onNodeWithTag("itinerary_item_$E2E_ACTIVITY_ID").performClick()
        composeRule.onNodeWithTag("itinerary_item_edit").performClick()

        composeRule.onNodeWithText("Name *").assertIsDisplayed()
        composeRule.onNodeWithText("Image URL (optional)").assertIsDisplayed()
        composeRule.onNodeWithText("Category *").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_activity_name")
            .performTextReplacement("edited activity")
        composeRule.onNodeWithTag("edit_activity_image").performTextReplacement("")
        composeRule.onNodeWithTag("edit_activity_category").performClick()
        composeRule.onNodeWithText("Art").performClick()
        composeRule.onNodeWithTag("edit_activity_location").performTextReplacement("UBC Nest")
        composeRule.onNodeWithTag("edit_activity_save").performScrollTo().performClick()

        composeRule.onNodeWithTag("itinerary_item_status").assertTextContains("Activity saved.")

        // Section 4.2 — Remove an Activity: steps 1–3.
        composeRule.onNodeWithTag("itinerary_item_delete").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete item?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_item_delete").performClick()

        composeRule.onNodeWithTag("itinerary_status").assertTextContains("Item deleted.")
        assertFalse(repository.itineraries.single().items.any { it.id == E2E_ACTIVITY_ID })
    }

    @Test
    fun modifyAndRemoveTransportation() {
        // Section 4.2 — Modify transportation: steps 1–4 and failures 3a–3c1.
        openItinerary()
        composeRule.onNodeWithTag("itinerary_item_$E2E_TRANSPORTATION_ID").performClick()
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
        setDateTime("edit_transportation_start_time", hour = 3)
        composeRule.onNodeWithTag("time_order_error").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edit_transportation_save").assertIsNotEnabled()
        setDateTime("edit_transportation_start_time", hour = 1, period = "AM")
        composeRule.onNodeWithTag("edit_transportation_save").performScrollTo().performClick()

        composeRule.onNodeWithTag("itinerary_item_status")
            .assertTextContains("Transportation saved.")

        // Section 4.2 — Remove Transportation: steps 1–3.
        composeRule.onNodeWithTag("itinerary_item_delete").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete item?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_item_delete").performClick()

        composeRule.onNodeWithTag("itinerary_status").assertTextContains("Item deleted.")
        assertFalse(repository.itineraries.single().items.any { it.id == E2E_TRANSPORTATION_ID })
    }

    @Test
    // Section 4.2 — Delete an itinerary: steps 1–3, including cancellation.
    fun deleteItineraryRequiresConfirmation() {
        openItinerary()
        composeRule.onNodeWithTag("itinerary_delete").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete itinerary?").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_delete_itinerary").performClick()
        assertTrue(repository.itineraries.any { it.id == E2E_ITINERARY_ID })

        composeRule.onNodeWithTag("itinerary_delete").performClick()
        composeRule.onNodeWithTag("confirm_delete_itinerary").performClick()

        composeRule.onNodeWithText("Trips").assertIsDisplayed()
        composeRule.onNodeWithTag("trips_status").assertTextContains("Itinerary deleted.")
        assertFalse(repository.itineraries.any { it.id == E2E_ITINERARY_ID })
    }

    private fun openItinerary() {
        composeRule.onNodeWithTag("saved_itinerary_$E2E_ITINERARY_ID").performClick()
        waitForTag("itinerary_details_edit")
    }

    private fun setDateTime(tag: String, hour: Int, period: String = "PM") {
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithContentDescription("$hour o'clock").performClick()
        composeRule.onNodeWithContentDescription("0 minutes").performClick()
        composeRule.onNodeWithText(period).performClick()
        composeRule.onNodeWithText("OK").performClick()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
