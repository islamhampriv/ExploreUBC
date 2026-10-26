package com.exploreUBC.cpen321application.ui.trips

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import com.exploreUBC.cpen321application.data.itineraries.ActivityItem
import com.exploreUBC.cpen321application.data.itineraries.Itinerary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItineraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recommendationIsReadOnlyAndShowsSaveAction() {
        var saveCalls = 0
        var itemClicks = 0

        composeRule.setContent {
            ItineraryScreen(
                itinerary = itinerary(saved = false, inRecommendation = true),
                isSaving = false,
                statusMessage = null,
                onBack = {},
                onItemClick = { itemClicks += 1 },
                onAddActivity = { error("Recommendations cannot add activities") },
                onAddTransportation = { error("Recommendations cannot add transportation") },
                onSaveDetails = { error("Recommendation details must be read-only") },
                onSaveRecommendation = { saveCalls += 1 },
            )
        }

        assertTagAbsent("itinerary_details_edit")
        assertTagAbsent("itinerary_add_activity")
        assertTagAbsent("itinerary_add_transportation")
        composeRule.onNodeWithTag("itinerary_item_activity-1").performClick()
        composeRule.runOnIdle { assertEquals(1, itemClicks) }
        composeRule.onNodeWithTag("itinerary_save_recommendation")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle { assertEquals(1, saveCalls) }
    }

    @Test
    fun savedItineraryShowsEditingActionsInsteadOfRecommendationSave() {
        composeRule.setContent {
            ItineraryScreen(
                itinerary = itinerary(saved = true, inRecommendation = false),
                isSaving = false,
                statusMessage = null,
                onBack = {},
                onItemClick = {},
                onAddActivity = {},
                onAddTransportation = {},
                onSaveDetails = {},
                onSaveRecommendation = { error("Saved itinerary must not show recommendation save") },
            )
        }

        composeRule.onNodeWithTag("itinerary_details_edit").assertIsDisplayed()
        composeRule.onNodeWithTag("itinerary_add_activity").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("itinerary_add_transportation").assertIsDisplayed()
        assertTagAbsent("itinerary_save_recommendation")
    }

    @Test
    fun recommendationItemDetailsHideEditAndDeleteActions() {
        val activity = itinerary(saved = false, inRecommendation = true)
            .items
            .filterIsInstance<ActivityItem>()
            .single()

        composeRule.setContent {
            ItineraryItemScreen(
                item = activity,
                isReadOnly = true,
                isSaving = false,
                statusMessage = null,
                onBack = {},
                onUpdateActivity = { error("Recommendation activity cannot be edited") },
                onUpdateTransportation = {
                    error("Recommendation transportation cannot be edited")
                },
                onDelete = { error("Recommendation item cannot be deleted") },
            )
        }

        composeRule.onNodeWithTag("itinerary_item_detail").assertIsDisplayed()
        assertTagAbsent("itinerary_item_edit")
        assertTagAbsent("itinerary_item_delete")
    }

    @Test
    fun recommendedSectionGeneratesAndShowsLoadingState() {
        var generateCalls = 0
        val isGenerating = mutableStateOf(false)

        composeRule.setContent {
            TripsList(
                savedItineraries = emptyList(),
                recommendedItineraries = emptyList(),
                isLoading = false,
                isGenerating = isGenerating.value,
                statusMessage = null,
                onItineraryClick = {},
                onGenerateRecommendations = { generateCalls += 1 },
            )
        }

        composeRule.onNodeWithTag("generate_recommendations")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, generateCalls)
            isGenerating.value = true
        }
        composeRule.onNodeWithTag("generate_recommendations").assertIsNotEnabled()
        composeRule.onNodeWithTag("generate_recommendations_loading").assertIsDisplayed()
    }

    @Test
    fun recommendedCardShowsScoreOutOfFive() {
        composeRule.setContent {
            TripsList(
                savedItineraries = emptyList(),
                recommendedItineraries = listOf(
                    itinerary(saved = false, inRecommendation = true),
                ),
                isLoading = false,
                isGenerating = false,
                statusMessage = null,
                onItineraryClick = {},
                onGenerateRecommendations = {},
            )
        }

        composeRule.onNodeWithText("4.9/5").assertIsDisplayed()
        composeRule.onNodeWithTag("recommended_itinerary_score_itinerary-1")
            .assertIsDisplayed()
    }

    private fun itinerary(saved: Boolean, inRecommendation: Boolean) = Itinerary(
        id = "itinerary-1",
        ownerId = "user-1",
        name = "Stanley Park Coastal Loop",
        description = "A coastal day trip.",
        startingLocation = "UBC Bus Exchange",
        items = listOf(
            ActivityItem(
                id = "activity-1",
                name = "Stanley Park Seawall Walk",
                image = "",
                description = "Walk along the seawall.",
                category = "outdoors",
                location = "Stanley Park",
                startTime = "2026-11-01T17:00:00.000Z",
                endTime = "2026-11-01T19:00:00.000Z",
            ),
        ),
        createdAt = "2026-10-25T12:00:00.000Z",
        updatedAt = "2026-10-25T12:00:00.000Z",
        saved = saved,
        inRecommendation = inRecommendation,
        score = mapOf(
            "interestSimilarity" to 0.9,
            "interestSimilarityOutOfFive" to 4.85,
        ),
    )

    private fun assertTagAbsent(tag: String) {
        assertEquals(0, composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size)
    }
}
