package com.jacobrozell.dartbuddy.domain.bots

import kotlinx.serialization.Serializable

@Serializable
data class CustomBotFacetOverrides(
    val x01: X01SkillFacet? = null,
    val cricket: CricketSkillFacet? = null,
    val aim: AimSkillFacet? = null,
) {
    fun applyTo(profile: BotSkillProfile): BotSkillProfile {
        var result = profile
        x01?.let { result = it.applyTo(result) }
        cricket?.let { result = it.applyTo(result) }
        aim?.let { result = it.applyTo(result) }
        return result
    }

    companion object {
        fun extract(profile: BotSkillProfile): CustomBotFacetOverrides = CustomBotFacetOverrides(
            x01 = X01SkillFacet.extract(profile),
            cricket = CricketSkillFacet.extract(profile),
            aim = AimSkillFacet.extract(profile),
        )
    }
}

@Serializable
data class X01SkillFacet(
    val scoringVisitMin: Int? = null,
    val scoringVisitMax: Int? = null,
    val singleHitChance: Double? = null,
    val doubleHitChance: Double? = null,
    val tripleHitChance: Double? = null,
    val checkoutAttemptChance: Double? = null,
    val offBoardMissChance: Double? = null,
    val riskyBustChance: Double? = null,
    val triplePreference: Double? = null,
    val checkInHitBoost: Double? = null,
    val innerBullAimChance: Double? = null,
    val masterInTripleOpenerChance: Double? = null,
) {
    fun applyTo(profile: BotSkillProfile): BotSkillProfile {
        val x01 = profile.x01
        return profile.copy(
            x01 = x01.copy(
                scoringVisitMin = scoringVisitMin ?: x01.scoringVisitMin,
                scoringVisitMax = scoringVisitMax ?: x01.scoringVisitMax,
                hitChances = BotSkillProfile.HitChances(
                    single = singleHitChance ?: x01.hitChances.single,
                    double = doubleHitChance ?: x01.hitChances.double,
                    triple = tripleHitChance ?: x01.hitChances.triple,
                ),
                checkoutAttemptChance = checkoutAttemptChance ?: x01.checkoutAttemptChance,
                offBoardMissChance = offBoardMissChance ?: x01.offBoardMissChance,
                riskyBustChance = riskyBustChance ?: x01.riskyBustChance,
                triplePreference = triplePreference ?: x01.triplePreference,
                checkInHitBoost = checkInHitBoost ?: x01.checkInHitBoost,
                innerBullAimChance = innerBullAimChance ?: x01.innerBullAimChance,
                masterInTripleOpenerChance = masterInTripleOpenerChance ?: x01.masterInTripleOpenerChance,
            ),
        )
    }

    companion object {
        fun extract(profile: BotSkillProfile): X01SkillFacet {
            val x01 = profile.x01
            return X01SkillFacet(
                scoringVisitMin = x01.scoringVisitMin,
                scoringVisitMax = x01.scoringVisitMax,
                singleHitChance = x01.hitChances.single,
                doubleHitChance = x01.hitChances.double,
                tripleHitChance = x01.hitChances.triple,
                checkoutAttemptChance = x01.checkoutAttemptChance,
                offBoardMissChance = x01.offBoardMissChance,
                riskyBustChance = x01.riskyBustChance,
                triplePreference = x01.triplePreference,
                checkInHitBoost = x01.checkInHitBoost,
                innerBullAimChance = x01.innerBullAimChance,
                masterInTripleOpenerChance = x01.masterInTripleOpenerChance,
            )
        }
    }
}

@Serializable
data class CricketSkillFacet(
    val singleHitChance: Double? = null,
    val doubleHitChance: Double? = null,
    val tripleHitChance: Double? = null,
    val offBoardMissChance: Double? = null,
    val wrongBedChance: Double? = null,
) {
    fun applyTo(profile: BotSkillProfile): BotSkillProfile {
        val cricket = profile.cricket
        return profile.copy(
            cricket = cricket.copy(
                hitChances = BotSkillProfile.HitChances(
                    single = singleHitChance ?: cricket.hitChances.single,
                    double = doubleHitChance ?: cricket.hitChances.double,
                    triple = tripleHitChance ?: cricket.hitChances.triple,
                ),
                offBoardMissChance = offBoardMissChance ?: cricket.offBoardMissChance,
                wrongBedChance = wrongBedChance ?: cricket.wrongBedChance,
            ),
        )
    }

    companion object {
        fun extract(profile: BotSkillProfile): CricketSkillFacet {
            val cricket = profile.cricket
            return CricketSkillFacet(
                singleHitChance = cricket.hitChances.single,
                doubleHitChance = cricket.hitChances.double,
                tripleHitChance = cricket.hitChances.triple,
                offBoardMissChance = cricket.offBoardMissChance,
                wrongBedChance = cricket.wrongBedChance,
            )
        }
    }
}

@Serializable
data class AimSkillFacet(
    val scoringBehaviorTierRaw: String? = null,
    val tripleOnOpenChance: Double? = null,
    val doubleOnOpenChance: Double? = null,
) {
    val scoringBehaviorTier: BotDifficulty?
        get() = scoringBehaviorTierRaw?.let { BotDifficulty.fromRawValue(it) }

    fun applyTo(profile: BotSkillProfile): BotSkillProfile {
        var updated = profile
        scoringBehaviorTier?.let { updated = updated.withScoringBehaviorTier(it) }
        val cricket = updated.cricket
        return updated.copy(
            cricket = cricket.copy(
                tripleOnOpenChance = tripleOnOpenChance ?: cricket.tripleOnOpenChance,
                doubleOnOpenChance = doubleOnOpenChance ?: cricket.doubleOnOpenChance,
            ),
        )
    }

    companion object {
        fun extract(profile: BotSkillProfile): AimSkillFacet = AimSkillFacet(
            scoringBehaviorTierRaw = profile.x01.scoringBehaviorTierRaw,
            tripleOnOpenChance = profile.cricket.tripleOnOpenChance,
            doubleOnOpenChance = profile.cricket.doubleOnOpenChance,
        )
    }
}
