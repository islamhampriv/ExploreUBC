package com.exploreUBC.cpen321application.e2e

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.exploreUBC.cpen321application.MainActivity2
import java.io.IOException
import kotlinx.coroutines.delay
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** The account MainActivity2 loads from the running backend. */
internal const val APP_USER_ID = "6acd519c76376cefc61e97f1"

internal class GrantLocationRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val packageName = instrumentation.targetContext.packageName
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ).forEach { permission ->
                    instrumentation.uiAutomation.grantRuntimePermission(packageName, permission)
                }
                base.evaluate()
            }
        }
    }
}

internal fun AndroidComposeTestRule<*, MainActivity2>.waitForMainScreen() {
    waitUntil(timeoutMillis = 45_000) {
        runCatching {
            onNodeWithTag("navigation_Events").assertIsDisplayed()
            onNodeWithTag("navigation_Trips").assertIsDisplayed()
            onNodeWithTag("navigation_Map").assertIsDisplayed()
        }.isSuccess
    }
}

internal fun AndroidComposeTestRule<*, MainActivity2>.openTab(label: String) {
    onNodeWithTag("navigation_$label").performClick()
}

internal fun AndroidComposeTestRule<*, *>.waitForTag(
    tag: String,
    timeoutMillis: Long = 15_000,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

/** Retries a backend call when the emulator cannot connect to the host. */
internal suspend fun <T> retryIo(block: suspend () -> T): T {
    var lastError: IOException? = null
    repeat(3) { attempt ->
        try {
            return block()
        } catch (error: IOException) {
            lastError = error
            if (attempt < 2) delay(1_000)
        }
    }
    throw lastError ?: IOException("Backend request failed")
}

internal fun AndroidComposeTestRule<*, *>.waitForText(
    text: String,
    timeoutMillis: Long = 15_000,
    substring: Boolean = false,
) {
    waitUntil(timeoutMillis) {
        onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }
}
