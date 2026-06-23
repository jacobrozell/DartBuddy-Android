package com.jacobrozell.dartbuddy.domain.bots

import androidx.annotation.StringRes
import com.jacobrozell.dartbuddy.R
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.support.L10n
import kotlinx.serialization.Serializable

@Serializable
enum class BotDifficulty(val rawValue: String) {
    VeryEasy("veryEasy"),
    Easy("easy"),
    Medium("medium"),
    Hard("hard"),
    Pro("pro"),
    ;

    @get:StringRes
    val displayNameRes: Int
        get() = when (this) {
            VeryEasy -> R.string.bot_difficulty_veryEasy
            Easy -> R.string.bot_difficulty_easy
            Medium -> R.string.bot_difficulty_medium
            Hard -> R.string.bot_difficulty_hard
            Pro -> R.string.bot_difficulty_pro
        }

    fun displayName(l10n: L10n): String = l10n.get(displayNameRes)

    fun rosterName(l10n: L10n): String = l10n.format(R.string.bot_rosterNameFormat, displayName(l10n))

    val skillProfile: BotSkillProfile
        get() {
            val display = displayProfile
            val safe = safeRemainingBuffers(this)
            return BotSkillProfile(
                x01 = BotSkillProfile.X01(
                    scoringVisitMin = display.x01.scoringVisitMin,
                    scoringVisitMax = display.x01.scoringVisitMax,
                    hitChances = BotSkillProfile.HitChances(
                        single = display.x01.hitChances.single,
                        double = display.x01.hitChances.double,
                        triple = display.x01.hitChances.triple,
                    ),
                    checkoutAttemptChance = display.x01.checkoutAttemptChance,
                    offBoardMissChance = display.x01.offBoardMissChance,
                    riskyBustChance = display.x01.riskyBustChance,
                    triplePreference = display.x01.triplePreference,
                    checkInHitBoost = display.x01.checkInHitBoost,
                    innerBullAimChance = display.x01.innerBullAimChance,
                    masterInTripleOpenerChance = display.x01.masterInTripleOpenerChance,
                    safeRemainingSingleOut = safe.singleOut,
                    safeRemainingDoubleOut = safe.doubleOut,
                    safeRemainingMasterOut = safe.masterOut,
                    scoringBehaviorTierRaw = rawValue,
                ),
                cricket = BotSkillProfile.Cricket(
                    hitChances = BotSkillProfile.HitChances(
                        single = display.cricket.hitChances.single,
                        double = display.cricket.hitChances.double,
                        triple = display.cricket.hitChances.triple,
                    ),
                    offBoardMissChance = display.cricket.offBoardMissChance,
                    wrongBedChance = display.cricket.wrongBedChance,
                    innerBullAimChance = display.x01.innerBullAimChance,
                    tripleOnOpenChance = cricketTripleOnOpenChance(this),
                    doubleOnOpenChance = cricketDoubleOnOpenChance(this),
                ),
            )
        }

    private val displayProfile: BotDifficultyDisplayProfile
        get() = BotDifficultyDisplayProfile(
            x01 = BotDifficultyDisplayProfile.X01(
                scoringVisitMin = scoringVisitRange.first,
                scoringVisitMax = scoringVisitRange.last,
                hitChances = BotDifficultyDisplayProfile.HitChances(
                    single = hitChance(DartMultiplier.Single),
                    double = hitChance(DartMultiplier.Double),
                    triple = hitChance(DartMultiplier.Triple),
                ),
                checkoutAttemptChance = checkoutAttemptChance,
                offBoardMissChance = offBoardMissChance,
                riskyBustChance = riskyDartWhenWouldBustChance,
                triplePreference = prefersTripleOnScoringSegment,
                checkInHitBoost = checkInHitBoost,
                innerBullAimChance = innerBullAimChance,
                masterInTripleOpenerChance = masterInTripleOpenerChance,
            ),
            cricket = BotDifficultyDisplayProfile.Cricket(
                hitChances = BotDifficultyDisplayProfile.HitChances(
                    single = cricketHitChance(DartMultiplier.Single),
                    double = cricketHitChance(DartMultiplier.Double),
                    triple = cricketHitChance(DartMultiplier.Triple),
                ),
                offBoardMissChance = cricketOffBoardMissChance,
                wrongBedChance = cricketWrongBedChance,
            ),
        )

    private val scoringVisitRange: IntRange
        get() = when (this) {
            VeryEasy -> 10..22
            Easy -> 18..42
            Medium -> 22..38
            Hard -> 28..44
            Pro -> 34..50
        }

    private val checkoutAttemptChance: Double
        get() = when (this) {
            VeryEasy -> 0.12
            Easy -> 0.25
            Medium -> 0.40
            Hard -> 0.50
            Pro -> 0.58
        }

    private fun hitChance(intendedMultiplier: DartMultiplier): Double = when (this to intendedMultiplier) {
        VeryEasy to DartMultiplier.Triple -> 0.06
        VeryEasy to DartMultiplier.Double -> 0.14
        VeryEasy to DartMultiplier.Single -> 0.30
        Easy to DartMultiplier.Triple -> 0.18
        Easy to DartMultiplier.Double -> 0.28
        Easy to DartMultiplier.Single -> 0.42
        Medium to DartMultiplier.Triple -> 0.34
        Medium to DartMultiplier.Double -> 0.40
        Medium to DartMultiplier.Single -> 0.52
        Hard to DartMultiplier.Triple -> 0.38
        Hard to DartMultiplier.Double -> 0.44
        Hard to DartMultiplier.Single -> 0.54
        Pro to DartMultiplier.Triple -> 0.48
        Pro to DartMultiplier.Double -> 0.54
        Pro to DartMultiplier.Single -> 0.66
        else -> 0.0
    }

    private val prefersTripleOnScoringSegment: Double
        get() = when (this) {
            VeryEasy -> 0.08
            Easy -> 0.25
            Medium -> 0.32
            Hard -> 0.32
            Pro -> 0.44
        }

    private val innerBullAimChance: Double
        get() = when (this) {
            VeryEasy, Easy, Medium -> 0.0
            Hard -> 0.12
            Pro -> 0.28
        }

    private val masterInTripleOpenerChance: Double
        get() = when (this) {
            VeryEasy, Easy, Medium -> 0.0
            Hard -> 0.15
            Pro -> 0.32
        }

    private val checkInHitBoost: Double
        get() = when (this) {
            VeryEasy -> 0.18
            Easy -> 0.16
            Medium -> 0.12
            Hard -> 0.08
            Pro -> 0.06
        }

    private val offBoardMissChance: Double
        get() = when (this) {
            VeryEasy -> 0.20
            Easy -> 0.12
            Medium -> 0.09
            Hard -> 0.07
            Pro -> 0.05
        }

    private fun cricketHitChance(intendedMultiplier: DartMultiplier): Double = when (this to intendedMultiplier) {
        VeryEasy to DartMultiplier.Triple -> 0.04
        VeryEasy to DartMultiplier.Double -> 0.09
        VeryEasy to DartMultiplier.Single -> 0.20
        Easy to DartMultiplier.Triple -> 0.11
        Easy to DartMultiplier.Double -> 0.17
        Easy to DartMultiplier.Single -> 0.30
        Medium to DartMultiplier.Triple -> 0.20
        Medium to DartMultiplier.Double -> 0.26
        Medium to DartMultiplier.Single -> 0.36
        Hard to DartMultiplier.Triple -> 0.26
        Hard to DartMultiplier.Double -> 0.32
        Hard to DartMultiplier.Single -> 0.42
        Pro to DartMultiplier.Triple -> 0.32
        Pro to DartMultiplier.Double -> 0.38
        Pro to DartMultiplier.Single -> 0.50
        else -> 0.0
    }

    private val cricketOffBoardMissChance: Double
        get() = when (this) {
            VeryEasy -> 0.34
            Easy -> 0.28
            Medium -> 0.22
            Hard -> 0.18
            Pro -> 0.14
        }

    private val cricketWrongBedChance: Double
        get() = when (this) {
            VeryEasy -> 0.42
            Easy -> 0.36
            Medium -> 0.30
            Hard -> 0.24
            Pro -> 0.20
        }

    private val riskyDartWhenWouldBustChance: Double
        get() = when (this) {
            VeryEasy -> 0.70
            Easy -> 0.40
            Medium -> 0.20
            Hard -> 0.16
            Pro -> 0.12
        }

    companion object {
        fun fromRawValue(rawValue: String): BotDifficulty? =
            entries.find { it.rawValue == rawValue }
    }
}

data class BotDifficultyDisplayProfile(
    val x01: X01,
    val cricket: Cricket,
) {
    data class HitChances(
        val single: Double,
        val double: Double,
        val triple: Double,
    )

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
    )

    data class Cricket(
        val hitChances: HitChances,
        val offBoardMissChance: Double,
        val wrongBedChance: Double,
    )

    companion object {
        fun percent(value: Double, signed: Boolean = false): String {
            val formatted = "%.0f%%".format(value * 100)
            return if (signed && value > 0) "+$formatted" else formatted
        }

        fun range(min: Int, max: Int): String = "$min–$max"
    }
}

private data class SafeRemainingBuffers(
    val singleOut: Int,
    val doubleOut: Int,
    val masterOut: Int,
)

private fun safeRemainingBuffers(difficulty: BotDifficulty): SafeRemainingBuffers = when (difficulty) {
    BotDifficulty.VeryEasy -> SafeRemainingBuffers(6, 4, 8)
    BotDifficulty.Easy -> SafeRemainingBuffers(10, 10, 12)
    BotDifficulty.Medium -> SafeRemainingBuffers(20, 28, 32)
    BotDifficulty.Hard -> SafeRemainingBuffers(28, 38, 40)
    BotDifficulty.Pro -> SafeRemainingBuffers(32, 34, 50)
}

private fun cricketTripleOnOpenChance(difficulty: BotDifficulty): Double = when (difficulty) {
    BotDifficulty.VeryEasy, BotDifficulty.Easy -> 0.0
    BotDifficulty.Medium -> 0.55
    BotDifficulty.Hard -> 0.58
    BotDifficulty.Pro -> 0.68
}

private fun cricketDoubleOnOpenChance(difficulty: BotDifficulty): Double = when (difficulty) {
    BotDifficulty.VeryEasy, BotDifficulty.Easy -> 0.0
    BotDifficulty.Medium -> 0.45
    BotDifficulty.Hard -> 0.55
    BotDifficulty.Pro -> 0.5
}
