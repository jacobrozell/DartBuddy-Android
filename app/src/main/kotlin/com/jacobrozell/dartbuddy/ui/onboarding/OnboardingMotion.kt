package com.jacobrozell.dartbuddy.ui.onboarding

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.jacobrozell.dartbuddy.domain.onboarding.OnboardingStep

private const val ONBOARDING_MOTION_MS = 170

fun onboardingStepIndex(step: OnboardingStep): Int = when (step) {
    OnboardingStep.Welcome -> 0
    OnboardingStep.RosterSetup -> 1
    OnboardingStep.Preferences -> 2
    OnboardingStep.LearnToPlay -> 3
    OnboardingStep.AppTour -> 4
    OnboardingStep.Support -> 5
    OnboardingStep.Ready -> 6
}

fun onboardingStepTransition(forward: Boolean, reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) {
        return fadeIn(tween(ONBOARDING_MOTION_MS)) togetherWith fadeOut(tween(ONBOARDING_MOTION_MS))
    }
    return (
        fadeIn(tween(ONBOARDING_MOTION_MS)) +
            slideInHorizontally(tween(ONBOARDING_MOTION_MS)) { full -> if (forward) full else -full }
        ) togetherWith (
        fadeOut(tween(ONBOARDING_MOTION_MS)) +
            slideOutHorizontally(tween(ONBOARDING_MOTION_MS)) { full -> if (forward) -full else full }
        )
}
