package com.exploreUBC.cpen321application.e2e.recommendation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.exploreUBC.cpen321application.testutil.E2E_RECOMMENDATION_ID
import com.exploreUBC.cpen321application.testutil.E2E_USER_ID
import com.exploreUBC.cpen321application.testutil.InMemoryItinerariesRepository
import com.exploreUBC.cpen321application.ui.trips.TripsRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecommendationE2ETest {
    @get:Rule
    val composeRule = createComposeRule()

    // Section 4.2 — Use Case: Generate a list of recommended itineraries, steps 1–4.
    @Test
    fun generateFourRecommendedItineraries() {
        val repository = InMemoryItinerariesRepository().apply {
            prepareRecommendationGeneration(count = 4)
        }
        showTrips(repository)

        composeRule.onNodeWithTag("generate_recommendations")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithText("Generate new recommendations?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_generate_recommendations").performClick()

        waitForTag("generate_recommendations_loading")
        composeRule.onNodeWithTag("generate_recommendations_loading").assertIsDisplayed()
        composeRule.onNodeWithText("Generating…").assertIsDisplayed()
        composeRule.onNodeWithTag("generate_recommendations").assertIsNotEnabled()

        repository.releaseRecommendationGeneration()

        waitForTag("recommended_itinerary_recommendation-4")
        composeRule.onNodeWithText("Recommended Itineraries").assertIsDisplayed()
        (1..4).forEach { index ->
            composeRule.onNodeWithTag("recommended_itinerary_recommendation-$index")
                .assertIsDisplayed()
        }
        composeRule.onNodeWithTag("trips_status")
            .assertTextContains("Recommendations generated.")
        assertEquals(4, repository.itineraries.count { it.inRecommendation })
    }

    // Section 4.2 — Use Case: Save an itinerary, steps 1–2.
    @Test
    fun saveRecommendedItineraryEnablesEditingActions() {
        val repository = InMemoryItinerariesRepository().apply { addRecommendation() }
        showTrips(repository)
        waitForTag("recommended_itinerary_$E2E_RECOMMENDATION_ID")

        composeRule.onNodeWithTag("recommended_itinerary_$E2E_RECOMMENDATION_ID")
            .performClick()
        waitForTag("itinerary_save_recommendation")
        composeRule.onNodeWithText("Itinerary details").assertIsDisplayed()
        composeRule.onNodeWithTag("itinerary_save_recommendation")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        waitForTag("itinerary_add_activity")
        composeRule.onNodeWithTag("itinerary_add_activity")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("itinerary_add_transportation")
            .performScrollTo()
            .assertIsDisplayed()

        val saved = repository.itineraries.single { it.id == E2E_RECOMMENDATION_ID }
        assertTrue(saved.saved)
        assertFalse(saved.inRecommendation)
    }

    private fun showTrips(repository: InMemoryItinerariesRepository) {
        composeRule.setContent {
            TripsRoute(
                userId = E2E_USER_ID,
                itinerariesRepository = repository,
            )
        }
        waitForTag("generate_recommendations")
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
