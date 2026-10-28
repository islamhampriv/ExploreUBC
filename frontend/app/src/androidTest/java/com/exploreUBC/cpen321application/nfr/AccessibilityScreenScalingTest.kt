package com.exploreUBC.cpen321application.nfr

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.setSystemFontScale
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.exploreUBC.cpen321application.testutil.TestRepositories
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityScreenScalingTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testLabelsVisibleAt200PercentScale() {
        composeRule.setSystemFontScale(2.0f)

        composeRule.setContent {
            CPEN321ApplicationApp(
                userId = "test-user",
                displayName = "Test User",
                usersRepository = TestRepositories.fakeUsersRepository,
                itinerariesRepository = TestRepositories.fakeItinerariesRepository,
                eventsRepository = TestRepositories.fakeEventsRepository,
            )
        }

        composeRule.onNodeWithText("Events").assertExists()
        composeRule.onNodeWithText("Events").assertHasClickAction()

        composeRule.onNodeWithText("Trips").assertExists()
        composeRule.onNodeWithText("Trips").assertHasClickAction()

        composeRule.onNodeWithText("Map").assertExists()
        composeRule.onNodeWithText("Map").assertHasClickAction()

        composeRule.onNodeWithText("Profile").assertExists()
        composeRule.onNodeWithText("Profile").assertHasClickAction()
    }
}