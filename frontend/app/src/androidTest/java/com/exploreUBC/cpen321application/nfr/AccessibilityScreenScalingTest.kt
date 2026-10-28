package com.exploreUBC.cpen321application.nfr

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.exploreUBC.cpen321application.testutil.AccessibilityTestApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityScreenScalingTest {

    @get:Rule
    val composeRule = createComposeRule()

    // This test only tests if the element is still on the screen 
    // Manual testing needed to check for overlap or irregularity
    @Test
    fun navigationLabelsRemainVisibleAndOperableAt200PercentScale() {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 2.0f,
                ),
            ) {
                AccessibilityTestApp()
            }
        }

        listOf("Events", "Trips", "Map", "Profile").forEach { label ->
            composeRule.onNodeWithText(label)
                .assertIsDisplayed()
            composeRule.onNodeWithTag("navigation_$label")
                .assertHasClickAction()
        }
    }
}
