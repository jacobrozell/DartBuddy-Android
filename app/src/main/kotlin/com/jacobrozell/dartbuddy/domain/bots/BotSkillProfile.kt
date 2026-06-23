package com.jacobrozell.dartbuddy.domain.bots

import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import kotlinx.serialization.Serializable

@Serializable
data class BotSkillProfile(
    val x01: X01,
    val cricket: Cricket,
) {
    @Serializable
    data class HitChances(
        val single: Double,
        val double: Double,
        val triple: Double,
    )

    @Serializable
    data class X01(
        val scoringVisitMin: Int,
        val scoringVisitMax: Int,
        val hitChances: HitChances,
        val checkoutAttemptChance: Double,
        val offBoardMissChance: Double,
        val riskyBustChance: Double,
        val triplePreference: Double,
        val checkInHitBoost: Double,
        val innerBullAimChance: Double,
        val masterInTripleOpenerChance: Double,
        val safeRemainingSingleOut: Int,
        val safeRemainingDoubleOut: Int,
        val safeRemainingMasterOut: Int,
        val scoringBehaviorTierRaw: String,
    ) {
        val scoringBehaviorTier: BotDifficulty
            get() = BotDifficulty.fromRawValue(scoringBehaviorTierRaw) ?: BotDifficulty.Medium
    }

    @Serializable
    data class Cricket(
        val hitChances: HitChances,
        val offBoardMissChance: Double,
        val wrongBedChance: Double,
        val innerBullAimChance: Double,
        val tripleOnOpenChance: Double,
        val doubleOnOpenChance: Double,
    )
}

fun BotSkillProfile.x01HitChance(intendedMultiplier: DartMultiplier): Double = when (intendedMultiplier) {
    DartMultiplier.Single -> x01.hitChances.single
    DartMultiplier.Double -> x01.hitChances.double
    DartMultiplier.Triple -> x01.hitChances.triple
}

fun BotSkillProfile.cricketHitChance(intendedMultiplier: DartMultiplier): Double = when (intendedMultiplier) {
    DartMultiplier.Single -> cricket.hitChances.single
    DartMultiplier.Double -> cricket.hitChances.double
    DartMultiplier.Triple -> cricket.hitChances.triple
}

val BotSkillProfile.displayProfile: BotDifficultyDisplayProfile
    get() {
        val x01 = x01
        val cricket = cricket
        return BotDifficultyDisplayProfile(
            x01 = BotDifficultyDisplayProfile.X01(
                scoringVisitMin = x01.scoringVisitMin,
                scoringVisitMax = x01.scoringVisitMax,
                hitChances = BotDifficultyDisplayProfile.HitChances(
                    single = x01.hitChances.single,
                    double = x01.hitChances.double,
                    triple = x01.hitChances.triple,
                ),
                checkoutAttemptChance = x01.checkoutAttemptChance,
                offBoardMissChance = x01.offBoardMissChance,
                riskyBustChance = x01.riskyBustChance,
                triplePreference = x01.triplePreference,
                checkInHitBoost = x01.checkInHitBoost,
                innerBullAimChance = x01.innerBullAimChance,
                masterInTripleOpenerChance = x01.masterInTripleOpenerChance,
            ),
            cricket = BotDifficultyDisplayProfile.Cricket(
                hitChances = BotDifficultyDisplayProfile.HitChances(
                    single = cricket.hitChances.single,
                    double = cricket.hitChances.double,
                    triple = cricket.hitChances.triple,
                ),
                offBoardMissChance = cricket.offBoardMissChance,
                wrongBedChance = cricket.wrongBedChance,
            ),
        )
    }

fun BotSkillProfile.withScoringBehaviorTier(tier: BotDifficulty): BotSkillProfile {
    val x01 = x01
    return copy(x01 = x01.copy(scoringBehaviorTierRaw = tier.rawValue))
}
