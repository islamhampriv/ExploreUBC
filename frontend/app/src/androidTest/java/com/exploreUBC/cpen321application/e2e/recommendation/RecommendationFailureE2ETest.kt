package com.exploreUBC.cpen321application.e2e.recommendation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.exploreUBC.cpen321application.CPEN321ApplicationApp
import com.exploreUBC.cpen321application.data.itineraries.ItinerariesRepository
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import com.exploreUBC.cpen321application.testutil.E2E_RECOMMENDATION_ID
import com.exploreUBC.cpen321application.testutil.E2E_USER_ID
import com.exploreUBC.cpen321application.testutil.FakeEventsRepository
import com.exploreUBC.cpen321application.testutil.InMemoryItinerariesRepository
import com.exploreUBC.cpen321application.testutil.InMemoryUsersRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Full-app failure journeys with a controlled recommendation repository boundary. */
class RecommendationFailureE2ETest {
    @get:Rule val composeRule = createComposeRule()

    private val stored = InMemoryItinerariesRepository().apply { addRecommendation() }

    @Test
    fun missingStartingLocationPreservesRecommendationsAndAllowsRetry() {
        assertFailureAndRetry("A starting location is required before generating recommendations.")
    }

    @Test
    fun missingUserPreservesRecommendationsAndAllowsRetry() {
        assertFailureAndRetry("The requested user profile was not found.")
    }

    @Test
    fun unavailableGenerationServicePreservesRecommendationsAndAllowsRetry() {
        assertFailureAndRetry("Recommendations could not be generated. Please try again.")
    }

    @Test
    fun invalidGeneratedItinerariesPreserveRecommendationsAndAllowRetry() {
        assertFailureAndRetry("Recommendations could not be generated. Please try again.")
    }

    @Test
    fun rankingFailureReportsErrorAndAllowsRetry() {
        assertFailureAndRetry("An unexpected error occurred.")
    }

    @Test
    fun storageFailureReportsErrorAndAllowsRetry() {
        assertFailureAndRetry("An unexpected error occurred.")
    }

    private fun assertFailureAndRetry(message: String) {
        val repository = FailOnceRecommendationRepository(stored, message)
        composeRule.setContent {
            CPEN321ApplicationApp(
                userId = E2E_USER_ID,
                usersRepository = InMemoryUsersRepository(),
                itinerariesRepository = repository,
                eventsRepository = FakeEventsRepository(),
            )
        }
        waitForTag("navigation_Trips")
        composeRule.onNodeWithTag("navigation_Trips").performClick()
        scrollToTag("recommended_itinerary_$E2E_RECOMMENDATION_ID")
        composeRule.onNodeWithTag("recommended_itinerary_$E2E_RECOMMENDATION_ID")
            .assertIsDisplayed()

        generateAndConfirm()
        scrollToTag("trips_status")
        composeRule.onNodeWithTag("trips_status").assertTextContains(message)
        assertEquals(1, repository.attempts)
        assertEquals(listOf(E2E_RECOMMENDATION_ID), currentRecommendationIds())
        scrollToTag("recommended_itinerary_$E2E_RECOMMENDATION_ID")
        composeRule.onNodeWithTag("recommended_itinerary_$E2E_RECOMMENDATION_ID")
            .assertIsDisplayed()

        scrollToTag("generate_recommendations")
        composeRule.onNodeWithTag("generate_recommendations").assertIsEnabled()
        generateAndConfirm()
        scrollToTag("trips_status")
        composeRule.onNodeWithTag("trips_status")
            .assertTextContains("Recommendations generated.")
        assertEquals(2, repository.attempts)
        assertEquals(4, currentRecommendationIds().size)
    }

    private fun currentRecommendationIds(): List<String> = runBlocking {
        stored.getItineraries(E2E_USER_ID, inRecommendation = true).map { it.id }
    }

    private fun generateAndConfirm() {
        scrollToTag("generate_recommendations")
        composeRule.onNodeWithTag("generate_recommendations").performClick()
        composeRule.onNodeWithText("Generate new recommendations?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_generate_recommendations").performClick()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollToTag(tag: String) {
        composeRule.waitUntil(5_000) {
            runCatching {
                composeRule.onNodeWithTag("trips_list")
                    .performScrollToNode(hasTestTag(tag))
            }.isSuccess
        }
    }
}

private class FailOnceRecommendationRepository(
    private val stored: InMemoryItinerariesRepository,
    private val failureMessage: String,
) : ItinerariesRepository by stored {
    var attempts = 0
        private set

    override suspend fun generateRecommendations(userId: String): List<Itinerary> {
        attempts++
        if (attempts == 1) throw IllegalStateException(failureMessage)
        stored.prepareRecommendationGeneration()
        stored.releaseRecommendationGeneration()
        return stored.generateRecommendations(userId)
    }
}
