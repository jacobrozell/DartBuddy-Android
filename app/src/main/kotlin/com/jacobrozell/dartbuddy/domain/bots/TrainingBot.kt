package com.jacobrozell.dartbuddy.domain.bots

import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.service.PlayerModeStats

object BotKind {
    const val PRESET = "preset"
    const val CUSTOM = "custom"
    const val TRAINING = "training"
}

data class TrainingBotEligibility(
    val isEligible: Boolean,
    val gamesPlayed: Int,
    val requiredGames: Int = TrainingBotEligibilityService.REQUIRED_GAMES,
    val mode: MatchType,
)

object TrainingBotEligibilityService {
    const val REQUIRED_GAMES = 5

    fun eligibility(modeStats: PlayerModeStats?, mode: MatchType): TrainingBotEligibility {
        val games = modeStats?.games ?: 0
        return TrainingBotEligibility(
            isEligible = games >= REQUIRED_GAMES,
            gamesPlayed = games,
            mode = mode,
        )
    }
}

object TrainingBotSkillTuning {
    const val X01_AVG_MULTIPLIER = 1.04
    const val X01_AVG_OFFSET = 2.0
    const val X01_MIN_BUMP = 1.5
    const val X01_MAX_AVERAGE = 92.0

    const val CRICKET_MPR_MULTIPLIER = 1.05
    const val CRICKET_MPR_OFFSET = 0.10
    const val CRICKET_MIN_BUMP = 0.08
    const val CRICKET_MAX_MPR = 3.8
}

object TrainingBotSkillResolver {
    fun resolve(modeStats: PlayerModeStats?, mode: MatchType): BotSkillProfile =
        when (mode) {
            MatchType.X01 -> {
                val playerAvg = modeStats?.stats?.average3Dart ?: 0.0
                if (playerAvg > 0) {
                    val target = minOf(
                        maxOf(
                            playerAvg * TrainingBotSkillTuning.X01_AVG_MULTIPLIER + TrainingBotSkillTuning.X01_AVG_OFFSET,
                            playerAvg + TrainingBotSkillTuning.X01_MIN_BUMP,
                        ),
                        TrainingBotSkillTuning.X01_MAX_AVERAGE,
                    )
                    BotSkillProfileInterpolator.profileForX01Average(target)
                } else {
                    BotSkillProfileInterpolator.profileForX01Average(24.0)
                }
            }
            MatchType.Cricket -> {
                val playerMpr = modeStats?.stats?.marksPerRound ?: 0.0
                if (playerMpr > 0) {
                    val target = minOf(
                        maxOf(
                            playerMpr * TrainingBotSkillTuning.CRICKET_MPR_MULTIPLIER + TrainingBotSkillTuning.CRICKET_MPR_OFFSET,
                            playerMpr + TrainingBotSkillTuning.CRICKET_MIN_BUMP,
                        ),
                        TrainingBotSkillTuning.CRICKET_MAX_MPR,
                    )
                    BotSkillProfileInterpolator.profileForCricketMPR(target)
                } else {
                    BotSkillProfileInterpolator.profileForCricketMPR(1.35)
                }
            }
        }

    fun calibratedX01Summary(profile: BotSkillProfile): Double =
        (profile.x01.scoringVisitMin + profile.x01.scoringVisitMax) / 2.0

    fun calibratedCricketSummary(profile: BotSkillProfile): Double =
        (profile.cricket.hitChances.triple + profile.cricket.hitChances.double) * 2.0
}

sealed class TrainingBotException(message: String) : Exception(message) {
    data object Duplicate : TrainingBotException("duplicate")
    data object InvalidPlayer : TrainingBotException("invalid_player")
    data object Ineligible : TrainingBotException("ineligible")
    data object NotTrainingBot : TrainingBotException("not_training_bot")
}
