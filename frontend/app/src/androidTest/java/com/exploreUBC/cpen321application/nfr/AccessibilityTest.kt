package com.exploreUBC.cpen321application.nfr

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertSize
import androidx.compose.ui.test.SatisfiesDp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.exploreUBC.cpen321application.testutil.TestRepositories
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testNavigationLabelsExposedToTalkBack() {
        composeRule.setContent {
            CPEN321ApplicationApp(
                userId = "test-user",
                displayName = "Test User",
                usersRepository = TestRepositories.fakeUsersRepository,
                itinerariesRepository = TestRepositories.fakeItinerariesRepository,
                eventsRepository = TestRepositories.fakeEventsRepository,
            )
        }

        composeRule.onNodeWithContentDescription("Events").assertExists()
        composeRule.onNodeWithContentDescription("Trips").assertExists()
        composeRule.onNodeWithContentDescription("Map").assertExists()
        composeRule.onNodeWithContentDescription("Profile").assertExists()
    }

    @Test
    fun testNavigationTargetMinimumSize() {
        composeRule.setContent {
            CPEN321ApplicationApp(
                userId = "test-user",
                displayName = "Test User",
                usersRepository = TestRepositories.fakeUsersRepository,
                itinerariesRepository = TestRepositories.fakeItinerariesRepository,
                eventsRepository = TestRepositories.fakeEventsRepository,
            )
        }

        composeRule.onNodeWithContentDescription("Events")
            .assertSize(SatisfiesDp { it.width >= 48f && it.height >= 48f })
        composeRule.onNodeWithContentDescription("Trips")
            .assertSize(SatisfiesDp { it.width >= 48f && it.height >= 48f })
        composeRule.onNodeWithContentDescription("Map")
            .assertSize(SatisfiesDp { it.width >= 48f && it.height >= 48f })
        composeRule.onNodeWithContentDescription("Profile")
            .assertSize(SatisfiesDp { it.width >= 48f && it.height >= 48f })
    }

    @Test
    fun testNavigationItemClickActions() {
        composeRule.setContent {
            CPEN321ApplicationApp(
                userId = "test-user",
                displayName = "Test User",
                usersRepository = TestRepositories.fakeUsersRepository,
                itinerariesRepository = TestRepositories.fakeItinerariesRepository,
                eventsRepository = TestRepositories.fakeEventsRepository,
            )
        }

        composeRule.onNodeWithContentDescription("Events").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Trips").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Map").assertHasClickAction()
        composeRule.onNodeWithContentDescription("Profile").assertHasClickAction()
    }

    @Test
    fun testNavigationClickOpensCorrectDestination() {
        composeRule.setContent {
            CPEN321ApplicationApp(
                userId = "test-user",
                displayName = "Test User",
                usersRepository = TestRepositories.fakeUsersRepository,
                itinerariesRepository = TestRepositories.fakeItinerariesRepository,
                eventsRepository = TestRepositories.fakeEventsRepository,
            )
        }

        composeRule.onNodeWithContentDescription("Events").performClick()
        composeRule.onNodeWithText("Events").assertExists()

        composeRule.onNodeWithContentDescription("Trips").performClick()
        composeRule.onNodeWithText("Trips").assertExists()

        composeRule.onNodeWithContentDescription("Map").performClick()
        composeRule.onNodeWithText("Map").assertExists()

        composeRule.onNodeWithContentDescription("Profile").performClick()
        composeRule.onNodeWithText("Profile").assertExists()
    }
}
