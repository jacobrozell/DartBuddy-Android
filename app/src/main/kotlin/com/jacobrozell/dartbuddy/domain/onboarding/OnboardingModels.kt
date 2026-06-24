package com.jacobrozell.dartbuddy.domain.onboarding

import com.jacobrozell.dartbuddy.domain.bots.BotDifficulty
import com.jacobrozell.dartbuddy.domain.model.PlayerAvatarStyle
import com.jacobrozell.dartbuddy.domain.model.PlayerColorToken

enum class OnboardingMode {
    FirstLaunch,
    Replay,
}

enum class OnboardingStep {
    Welcome,
    RosterSetup,
    Preferences,
    LearnToPlay,
    AppTour,
    Support,
    Ready,
    ;

    val progressIndex: Int?
        get() = when (this) {
            Welcome -> null
            RosterSetup -> 2
            Preferences, LearnToPlay -> 3
            AppTour -> 4
            Support -> 5
            Ready -> 6
        }

    fun backStep(showsRulesIntro: Boolean?, skipsRosterSetup: Boolean): OnboardingStep? = when (this) {
        Welcome -> null
        RosterSetup -> Welcome
        Preferences, LearnToPlay -> if (skipsRosterSetup) Welcome else RosterSetup
        AppTour -> if (showsRulesIntro == true) LearnToPlay else Preferences
        Support -> AppTour
        Ready -> Support
    }

    companion object {
        const val PROGRESS_TOTAL = 6
    }
}

data class OnboardingRosterDraft(
    val name: String,
    val avatarStyle: PlayerAvatarStyle = PlayerAvatarStyle.Dart,
    val colorToken: PlayerColorToken,
    val botDifficulty: BotDifficulty,
)

val BotDifficulty.showsOnboardingRulesIntro: Boolean
    get() = this == BotDifficulty.VeryEasy || this == BotDifficulty.Easy

val BotDifficulty.onboardingSliderIndex: Int
    get() = BotDifficulty.onboardingOrder.indexOf(this).coerceAtLeast(0)

val BotDifficulty.Companion.onboardingOrder: List<BotDifficulty>
    get() = listOf(BotDifficulty.VeryEasy, BotDifficulty.Easy, BotDifficulty.Medium, BotDifficulty.Hard, BotDifficulty.Pro)

fun BotDifficulty.Companion.fromOnboardingSliderIndex(index: Int): BotDifficulty {
    val order = onboardingOrder
    return order[index.coerceIn(0, order.lastIndex)]
}
