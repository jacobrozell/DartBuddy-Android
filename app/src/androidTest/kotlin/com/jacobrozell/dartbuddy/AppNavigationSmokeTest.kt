package com.jacobrozell.dartbuddy

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacobrozell.dartbuddy.bootstrap.DemoSeedExtras
import com.jacobrozell.dartbuddy.support.accessibility.AccessibilityIds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationSmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun launchMainWithOnboardingSkipped(): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(DemoSeedExtras.SKIP_ONBOARDING, true)
        }
        return ActivityScenario.launch(intent)
    }

    private fun waitForMainTabs() {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(AccessibilityIds.TAB_PLAY).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun mainTabsAreVisibleAfterLaunch() {
        launchMainWithOnboardingSkipped().use {
            waitForMainTabs()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_PLAY).assertIsDisplayed()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_PLAYERS).assertIsDisplayed()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_ACTIVITY).assertIsDisplayed()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_SETTINGS).assertIsDisplayed()
        }
    }

    @Test
    fun canNavigateBetweenTabs() {
        launchMainWithOnboardingSkipped().use {
            waitForMainTabs()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_PLAYERS).performClick()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_PLAYERS).assertIsDisplayed()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_ACTIVITY).performClick()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_SETTINGS).performClick()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_PLAY).performClick()
            composeRule.onNodeWithTag(AccessibilityIds.TAB_PLAY).assertIsDisplayed()
        }
    }
}

@RunWith(AndroidJUnit4::class)
class OnboardingReplaySmokeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun settingsReplayOpensOnboardingWelcome() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(DemoSeedExtras.SKIP_ONBOARDING, true)
        }
        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithTag(AccessibilityIds.TAB_SETTINGS).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag(AccessibilityIds.TAB_SETTINGS).performClick()
            composeRule.onNodeWithTag(AccessibilityIds.SETTINGS_REPLAY_ONBOARDING)
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithText("Show the welcome tour again?").assertIsDisplayed()
            composeRule.onNodeWithText("Confirm").performClick()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("Next").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Next").assertIsDisplayed()
        }
    }
}
