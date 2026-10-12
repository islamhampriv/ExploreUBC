package com.exploreUBC.cpen321application.ui.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.exploreUBC.cpen321application.data.users.BudgetPreference
import com.exploreUBC.cpen321application.data.users.InterestPreference
import com.exploreUBC.cpen321application.data.users.PreferencesUpdate
import com.exploreUBC.cpen321application.data.users.TransportationMode
import com.exploreUBC.cpen321application.data.users.UserPreferences
import com.exploreUBC.cpen321application.data.users.UsersRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingUserIdStillRendersEditableProfile() {
        val repository = object : UsersRepository {
            override suspend fun getPreferences(userId: String): UserPreferences {
                error("Repository should not be called without a user ID")
            }

            override suspend fun updatePreferences(
                userId: String,
                update: PreferencesUpdate,
            ): UserPreferences {
                error("Repository should not be called without a user ID")
            }
        }

        composeRule.setContent {
            ProfileRoute(
                userId = null,
                displayName = "Test User",
                usersRepository = repository,
            )
        }

        composeRule.onNodeWithTag("profile_display_name").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_start_location").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_start_location").performTextInput("UBC Bookstore")
        composeRule.onNodeWithTag("profile_start_location").assertTextContains("UBC Bookstore")
        composeRule.onNodeWithTag("profile_transportation_dropdown").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_budget_dropdown").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_interest_dropdown").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("profile_save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saveIncludesTransportationBudgetAndInterest() {
        var savedUpdate: PreferencesUpdate? = null

        composeRule.setContent {
            ProfilePreferencesForm(
                preferences = UserPreferences(
                    transportationMode = TransportationMode.TRANSIT,
                    budget = BudgetPreference.BALANCED,
                    interest = InterestPreference.FOOD,
                ),
                isSaving = false,
                statusMessage = null,
                onSave = { savedUpdate = it },
            )
        }

        composeRule.onNodeWithTag("profile_transportation_dropdown").performClick()
        composeRule.onNodeWithTag("profile_transportation_cycling").performClick()
        composeRule.onNodeWithTag("profile_budget_dropdown").performClick()
        composeRule.onNodeWithTag("profile_budget_premium").performClick()
        composeRule.onNodeWithTag("profile_interest_dropdown").performScrollTo().performClick()
        composeRule.onNodeWithTag("profile_interest_gaming").performClick()
        composeRule.onNodeWithTag("profile_save").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(TransportationMode.CYCLING, savedUpdate?.transportationMode)
            assertEquals(BudgetPreference.PREMIUM, savedUpdate?.budget)
            assertEquals(InterestPreference.GAMING, savedUpdate?.interest)
        }
    }
}
