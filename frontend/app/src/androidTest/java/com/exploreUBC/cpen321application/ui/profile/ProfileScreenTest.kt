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
import com.exploreUBC.cpen321application.data.users.TransportationMode
import com.exploreUBC.cpen321application.data.users.TransportationPreference
import com.exploreUBC.cpen321application.data.users.User
import com.exploreUBC.cpen321application.data.users.UserPreferences
import com.exploreUBC.cpen321application.data.users.UserUpdate
import com.exploreUBC.cpen321application.data.users.UsersRepository
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsLoadingInsteadOfDefaultsWhileUserIsBeingFetched() {
        val loadedUser = CompletableDeferred<User>()
        val repository = object : UsersRepository {
            override suspend fun getUser(userId: String): User = loadedUser.await()

            override suspend fun updateUser(userId: String, update: UserUpdate): User =
                error("Saving must not be available while loading")
        }

        composeRule.setContent {
            ProfileRoute(
                userId = "6acd519c76376cefc61e97f1",
                usersRepository = repository,
            )
        }

        composeRule.onNodeWithTag("profile_loading").assertIsDisplayed()

        loadedUser.complete(
            User(
                googleSubject = "google-subject",
                email = "test.user@student.ubc.ca",
                displayName = "Database User",
                startLocation = "UBC Bus Exchange",
                preferences = UserPreferences(
                    transportation = TransportationPreference(TransportationMode.TRANSIT),
                    budget = BudgetPreference.BALANCED,
                    interest = InterestPreference.OUTDOORS,
                ),
                createdAt = "2026-10-12T12:00:00.000Z",
                updatedAt = "2026-10-12T12:00:00.000Z",
            ),
        )

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_display_name").assertTextContains("Database User")
    }

    @Test
    fun missingUserIdStillRendersEditableProfile() {
        val repository = object : UsersRepository {
            override suspend fun getUser(userId: String): User {
                error("Repository should not be called without a user ID")
            }

            override suspend fun updateUser(userId: String, update: UserUpdate): User {
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
        composeRule.onNodeWithTag("profile_edit").performClick()
        composeRule.onNodeWithTag("profile_display_name_input").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_start_location").performTextInput("UBC Bookstore")
        composeRule.onNodeWithTag("profile_start_location").assertTextContains("UBC Bookstore")
        composeRule.onNodeWithTag("profile_transportation_dropdown").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_budget_dropdown").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_interest_dropdown").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("profile_save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadedUserPopulatesProfileAndNestedPreferences() {
        val repository = object : UsersRepository {
            override suspend fun getUser(userId: String): User = User(
                googleSubject = "google-subject",
                email = "test.user@student.ubc.ca",
                displayName = "Database User",
                startLocation = "UBC Bus Exchange",
                preferences = UserPreferences(
                    transportation = TransportationPreference(TransportationMode.TRANSIT),
                    budget = BudgetPreference.BALANCED,
                    interest = InterestPreference.OUTDOORS,
                ),
                createdAt = "2026-10-12T12:00:00.000Z",
                updatedAt = "2026-10-12T12:00:00.000Z",
            )

            override suspend fun updateUser(userId: String, update: UserUpdate): User =
                error("Saving is not part of this test")
        }

        composeRule.setContent {
            ProfileRoute(
                userId = "6acd519c76376cefc61e97f1",
                displayName = "Google User",
                usersRepository = repository,
            )
        }

        composeRule.onNodeWithTag("profile_display_name").assertTextContains("Database User")
        composeRule.onNodeWithTag("profile_start_location").assertTextContains("UBC Bus Exchange")
        composeRule.onNodeWithTag("profile_transportation_dropdown").assertTextContains("Transit")
        composeRule.onNodeWithTag("profile_budget_dropdown").assertTextContains("Balanced")
        composeRule.onNodeWithTag("profile_interest_dropdown").performScrollTo()
            .assertTextContains("Outdoors")
    }

    @Test
    fun profileEditSavesDisplayNameAndStartLocation() {
        var savedUpdate: UserUpdate? = null

        composeRule.setContent {
            ProfilePreferencesForm(
                state = ProfileScreenState(
                    displayName = "Original User",
                    startLocation = "Original Location",
                    preferences = UserPreferences(
                        transportation = TransportationPreference(TransportationMode.WALKING),
                        budget = BudgetPreference.ECONOMICAL,
                        interest = InterestPreference.FOOD,
                    ),
                    isSaving = false,
                    isEditingProfile = true,
                ),
                actions = ProfileScreenActions(
                    onSaveProfile = { savedUpdate = it },
                ),
            )
        }

        composeRule.onNodeWithTag("profile_display_name_input").performTextInput(" Updated")
        composeRule.onNodeWithTag("profile_start_location").performTextInput(" Updated")
        composeRule.onNodeWithTag("profile_details_save").performClick()

        composeRule.runOnIdle {
            assertEquals("Original User Updated", savedUpdate?.displayName)
            assertEquals("Original Location Updated", savedUpdate?.startLocation)
        }
    }

    @Test
    fun saveIncludesTransportationBudgetAndInterest() {
        var savedUpdate: UserUpdate? = null

        composeRule.setContent {
            ProfilePreferencesForm(
                state = ProfileScreenState(
                    preferences = UserPreferences(
                        transportation = TransportationPreference(TransportationMode.TRANSIT),
                        budget = BudgetPreference.BALANCED,
                        interest = InterestPreference.FOOD,
                    ),
                    isSaving = false,
                ),
                actions = ProfileScreenActions(
                    onSavePreferences = { savedUpdate = it },
                ),
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
            assertEquals(
                TransportationMode.CYCLING,
                savedUpdate?.preferences?.transportation?.mode,
            )
            assertEquals(BudgetPreference.PREMIUM, savedUpdate?.preferences?.budget)
            assertEquals(InterestPreference.GAMING, savedUpdate?.preferences?.interest)
        }
    }
}
