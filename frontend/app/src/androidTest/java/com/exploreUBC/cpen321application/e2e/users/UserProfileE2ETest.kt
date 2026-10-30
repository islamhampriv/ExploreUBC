package com.exploreUBC.cpen321application.e2e.users

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.data.users.BudgetPreference
import com.exploreUBC.cpen321application.data.users.InterestPreference
import com.exploreUBC.cpen321application.data.users.TransportationMode
import com.exploreUBC.cpen321application.ui.profile.ProfileRoute
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UserProfileE2ETest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: InMemoryUsersRepository

    @Before
    fun setUp() {
        repository = InMemoryUsersRepository()
        composeRule.setContent {
            ProfileRoute(
                userId = E2E_PROFILE_USER_ID,
                usersRepository = repository,
            )
        }
        waitForTag("profile_display_name")
    }

    @Test
    // Section 4.2 — Update Preferences: steps 1–3 and failure 2a–2a1.
    fun updateProfileDetailsAndValidateMaximumLength() {
        composeRule.onNodeWithTag("profile_display_name")
            .assertTextContains("ExploreUBC User")
        composeRule.onNodeWithText("UBC Bus Exchange").assertIsDisplayed()

        composeRule.onNodeWithTag("profile_edit").performClick()
        composeRule.onNodeWithTag("profile_display_name_input")
            .performTextReplacement("a".repeat(201))
        composeRule.onNodeWithTag("profile_display_name_length_error").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_details_save").assertIsNotEnabled()

        composeRule.onNodeWithTag("profile_display_name_input")
            .performTextReplacement("Updated Explorer")
        composeRule.onNodeWithTag("profile_start_location")
            .performTextReplacement("12345 abc Avenue, Vancouver BC")
        composeRule.onNodeWithTag("profile_details_save").performClick()

        waitForTag("profile_display_name")
        composeRule.onNodeWithTag("profile_display_name").assertTextContains("Updated Explorer")
        composeRule.onNodeWithText("12345 abc Avenue, Vancouver BC").assertIsDisplayed()
        assertEquals("Updated Explorer", repository.user.displayName)
    }

    @Test
    // Section 4.2 — Update Preferences: steps 4–7.
    fun updateTransportationBudgetAndInterestPreferences() {
        composeRule.onNodeWithTag("profile_transportation_dropdown").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_budget_dropdown").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_interest_dropdown").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("profile_transportation_dropdown").performClick()
        listOf("walking", "cycling", "transit", "driving", "other").forEach {
            composeRule.onNodeWithTag("profile_transportation_$it").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("profile_transportation_transit").performClick()

        composeRule.onNodeWithTag("profile_budget_dropdown").performClick()
        listOf("economical", "balanced", "premium").forEach {
            composeRule.onNodeWithTag("profile_budget_$it").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("profile_budget_balanced").performClick()

        composeRule.onNodeWithTag("profile_interest_dropdown").performScrollTo().performClick()
        listOf("food", "art", "outdoors", "markets", "live_music", "gaming").forEach {
            composeRule.onNodeWithTag("profile_interest_$it").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("profile_interest_outdoors").performClick()
        composeRule.onNodeWithTag("profile_save").performScrollTo().performClick()

        composeRule.onNodeWithTag("profile_status").assertTextContains("Preferences saved.")
        assertEquals(TransportationMode.TRANSIT, repository.user.preferences.transportation.mode)
        assertEquals(BudgetPreference.BALANCED, repository.user.preferences.budget)
        assertEquals(InterestPreference.OUTDOORS, repository.user.preferences.interest)
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
