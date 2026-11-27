package com.exploreUBC.cpen321application.e2e.users

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.CPEN321ApplicationApp
import com.exploreUBC.cpen321application.data.users.User
import com.exploreUBC.cpen321application.data.users.UserUpdate
import com.exploreUBC.cpen321application.data.users.UsersRepository
import com.exploreUBC.cpen321application.testutil.E2E_PROFILE_USER_ID
import com.exploreUBC.cpen321application.testutil.FakeEventsRepository
import com.exploreUBC.cpen321application.testutil.InMemoryItinerariesRepository
import com.exploreUBC.cpen321application.testutil.InMemoryUsersRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/** First-login failure journeys through the full app with a controlled user repository. */
class CreateProfileFailureE2ETest {
    @get:Rule val composeRule = createComposeRule()

    private val stored = InMemoryUsersRepository()
    private val users = FailOnceProfileRepository(stored)

    @Test
    fun missingAndOverlongLocationDisableCreateProfile() {
        openCreateProfile()
        composeRule.onNodeWithTag("create_profile_save").performScrollTo().assertIsNotEnabled()

        composeRule.onNodeWithTag("create_profile_start_location")
            .performTextReplacement("x".repeat(501))
        composeRule.onNodeWithTag("create_profile_start_location_length_error")
            .performScrollTo()
            .assertTextContains("Start location must be at most 500 characters.")
        composeRule.onNodeWithTag("create_profile_save").performScrollTo().assertIsNotEnabled()
        assertEquals(0, users.attempts)
    }

    @Test
    fun failedSaveKeepsEnteredValuesAndAllowsRetry() {
        openCreateProfile()
        composeRule.onNodeWithTag("create_profile_start_location")
            .performTextReplacement("UBC Bus Exchange")
        composeRule.onNodeWithTag("create_profile_transportation_dropdown").performClick()
        composeRule.onNodeWithTag("create_profile_transportation_transit").performClick()
        composeRule.onNodeWithTag("create_profile_budget_dropdown").performClick()
        composeRule.onNodeWithTag("create_profile_budget_balanced").performClick()
        composeRule.onNodeWithTag("create_profile_interest_dropdown")
            .performScrollTo().performClick()
        composeRule.onNodeWithTag("create_profile_interest_outdoors").performClick()
        composeRule.onNodeWithTag("create_profile_save").performScrollTo().performClick()

        waitForTag("create_profile_status")
        composeRule.onNodeWithTag("create_profile_status")
            .assertTextContains("An unexpected error occurred.")
        composeRule.onNodeWithTag("create_profile_start_location")
            .assertTextContains("UBC Bus Exchange")
        composeRule.onNodeWithTag("create_profile_transportation_dropdown")
            .assertTextContains("Transit")
        composeRule.onNodeWithTag("create_profile_budget_dropdown")
            .assertTextContains("Balanced")
        composeRule.onNodeWithTag("create_profile_interest_dropdown")
            .assertTextContains("Outdoors")
        composeRule.onNodeWithTag("create_profile_save").performScrollTo().assertIsEnabled()
        assertFalse(stored.onboardingCompleted)
        assertEquals(1, users.attempts)

        composeRule.onNodeWithTag("create_profile_save").performClick()
        waitForTag("navigation_Events")
        composeRule.onNodeWithTag("navigation_Events").assertIsDisplayed()
        assertEquals(2, users.attempts)
        assertEquals("UBC Bus Exchange", stored.user.startLocation)
        assertEquals("transit", stored.user.preferences.transportation.mode.apiValue)
        assertEquals("balanced", stored.user.preferences.budget.apiValue)
        assertEquals("outdoors", stored.user.preferences.interest.apiValue)
    }

    private fun openCreateProfile() {
        composeRule.setContent {
            CPEN321ApplicationApp(
                userId = E2E_PROFILE_USER_ID,
                onboardingCompletedAt = null,
                usersRepository = users,
                itinerariesRepository = InMemoryItinerariesRepository(),
                eventsRepository = FakeEventsRepository(),
            )
        }
        waitForTag("create_profile_screen")
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

private class FailOnceProfileRepository(
    private val stored: InMemoryUsersRepository,
) : UsersRepository by stored {
    var attempts = 0
        private set

    override suspend fun updateUser(userId: String, update: UserUpdate): User {
        attempts++
        if (attempts == 1) throw IllegalStateException("An unexpected error occurred.")
        return stored.updateUser(userId, update)
    }
}
