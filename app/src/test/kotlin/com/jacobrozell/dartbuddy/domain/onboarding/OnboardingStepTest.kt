package com.jacobrozell.dartbuddy.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty

class OnboardingStepTest {
    @Test
    fun backFromAppTourGoesToLearnForBeginnerTier() {
        val previous = OnboardingStep.AppTour.backStep(showsRulesIntro = true, skipsRosterSetup = false)
        assertEquals(OnboardingStep.LearnToPlay, previous)
    }

    @Test
    fun backFromAppTourGoesToPreferencesForExperiencedTier() {
        val previous = OnboardingStep.AppTour.backStep(showsRulesIntro = false, skipsRosterSetup = false)
        assertEquals(OnboardingStep.Preferences, previous)
    }

    @Test
    fun welcomeHasNoBackStep() {
        assertNull(OnboardingStep.Welcome.backStep(showsRulesIntro = null, skipsRosterSetup = false))
    }

    @Test
    fun beginnerTiersShowRulesIntro() {
        assertEquals(true, BotDifficulty.VeryEasy.showsOnboardingRulesIntro)
        assertEquals(true, BotDifficulty.Easy.showsOnboardingRulesIntro)
        assertEquals(false, BotDifficulty.Medium.showsOnboardingRulesIntro)
    }
}
