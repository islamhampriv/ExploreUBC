package com.exploreUBC.cpen321application.e2e.users

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.exploreUBC.cpen321application.BuildConfig
import com.exploreUBC.cpen321application.MainActivity2
import com.exploreUBC.cpen321application.data.users.User
import com.exploreUBC.cpen321application.data.users.UserUpdate
import com.exploreUBC.cpen321application.data.users.UsersApi
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

class UserProfileE2ETest {
    private val composeRule = createAndroidComposeRule<MainActivity2>()

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(GrantLocationRule()).around(composeRule)

    private val api = UsersApi(BuildConfig.API_BASE_URL)
    private lateinit var originalUser: User

    @Before
    fun startFromMainScreen() {
        originalUser = runBlocking { retryIo { api.getUser(APP_USER_ID) } }
        composeRule.waitForMainScreen()
        composeRule.openTab("Profile")
        composeRule.waitForTag("profile_display_name")
    }

    @After
    fun restoreProfile() {
        if (!::originalUser.isInitialized) return
        runBlocking {
            runCatching {
                retryIo { api.updateUser(
                    APP_USER_ID,
                    UserUpdate(
                        displayName = originalUser.displayName,
                        startLocation = originalUser.startLocation,
                        preferences = originalUser.preferences,
                    ),
                ) }
            }
        }
    }

    @Test
    // Section 4.2 — Update Preferences: steps 1–3 and failure 2a–2a1.
    fun updateProfileDetailsAndValidateMaximumLength() {
        val updatedName = "Updated Explorer ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("profile_display_name")
            .assertTextContains(originalUser.displayName)
        if (originalUser.startLocation.isNotBlank()) {
            composeRule.onNodeWithText(originalUser.startLocation).assertIsDisplayed()
        }

        composeRule.onNodeWithTag("profile_edit").performClick()
        composeRule.onNodeWithTag("profile_display_name_input")
            .performTextReplacement("a".repeat(201))
        composeRule.onNodeWithTag("profile_display_name_length_error").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_details_save").assertIsNotEnabled()

        composeRule.onNodeWithTag("profile_display_name_input")
            .performTextReplacement(updatedName)
        composeRule.onNodeWithTag("profile_start_location")
            .performTextReplacement("12345 abc Avenue, Vancouver BC")
        composeRule.onNodeWithTag("profile_details_save").performClick()

        composeRule.waitForTag("profile_display_name")
        composeRule.onNodeWithTag("profile_display_name").assertTextContains(updatedName)
        composeRule.openTab("Trips")
        composeRule.waitForTag("create_itinerary")
        composeRule.openTab("Profile")
        composeRule.waitForText(updatedName, substring = true)
        composeRule.onNodeWithText("12345 abc Avenue, Vancouver BC").assertIsDisplayed()
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

        composeRule.waitForText("Preferences saved.", substring = true)
        val saved = runBlocking { retryIo { api.getUser(APP_USER_ID) } }
        org.junit.Assert.assertEquals("transit", saved.preferences.transportation.mode.apiValue)
        org.junit.Assert.assertEquals("balanced", saved.preferences.budget.apiValue)
        org.junit.Assert.assertEquals("outdoors", saved.preferences.interest.apiValue)
    }
}
