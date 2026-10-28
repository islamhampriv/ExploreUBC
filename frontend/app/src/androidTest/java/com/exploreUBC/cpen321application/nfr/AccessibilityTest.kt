package com.exploreUBC.cpen321application.nfr

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.exploreUBC.cpen321application.testutil.AccessibilityTestApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationVisualLabelsMatchContentDescriptions() {
        setTestContent()

        navigationLabels.forEach { label ->
            composeRule.onNodeWithText(label).assertExists()

            val visibleText = composeRule
                .onNodeWithText(label, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .single()
                .text
            val contentDescription = composeRule
                .onNodeWithContentDescription(label, useUnmergedTree = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .single()

            assertEquals(
                "$label visible text and content description must match",
                visibleText,
                contentDescription,
            )
        }
    }

    @Test
    fun navigationTargetsAreAtLeast48Dp() {
        setTestContent()

        navigationLabels.forEach { label ->
            val bounds = composeRule.onNodeWithTag(navigationTag(label))
                .fetchSemanticsNode()
                .boundsInRoot
            val density = composeRule.density.density

            assertTrue("$label must be at least 48 dp wide", bounds.width / density >= minimumTargetSizeDp)
            assertTrue("$label must be at least 48 dp high", bounds.height / density >= minimumTargetSizeDp)
        }
    }

    @Test
    fun navigationItemsHaveClickActions() {
        setTestContent()

        navigationLabels.forEach { label ->
            composeRule.onNodeWithTag(navigationTag(label)).assertHasClickAction()
        }
    }

    private fun setTestContent() {
        composeRule.setContent { AccessibilityTestApp() }
    }

    private fun navigationTag(label: String) = "navigation_$label"

    private companion object {
        val navigationLabels = listOf("Events", "Trips", "Map", "Profile")
        const val minimumTargetSizeDp = 48f
    }
}
