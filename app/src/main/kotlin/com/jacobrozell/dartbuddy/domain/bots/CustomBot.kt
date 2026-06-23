package com.jacobrozell.dartbuddy.domain.bots

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

@Serializable
data class CustomBotMetrics(
    val x01Average: Double,
    val cricketMPR: Double,
) {
    init {
        require(x01Average in X01_AVERAGE_RANGE)
        require(cricketMPR in CRICKET_MPR_RANGE)
    }

    companion object {
        const val DEFAULT_X01_AVERAGE = 30.0
        const val DEFAULT_CRICKET_MPR = 1.25
        val X01_AVERAGE_RANGE = 5.0..110.0
        val CRICKET_MPR_RANGE = 0.2..5.0
        private const val PREFIX = "custom:"

        fun clampX01(value: Double): Double = value.coerceIn(X01_AVERAGE_RANGE)
        fun clampMPR(value: Double): Double = value.coerceIn(CRICKET_MPR_RANGE)

        fun decode(botDifficultyRaw: String?): CustomBotMetrics? {
            val raw = botDifficultyRaw ?: return null
            if (!raw.startsWith(PREFIX) || raw.startsWith(CustomBotConfigurationCodec.V2_PREFIX)) return null
            val body = raw.removePrefix(PREFIX)
            val parts = body.split(":")
            if (parts.size != 2) return null
            val x01 = parts[0].toDoubleOrNull() ?: return null
            val mpr = parts[1].toDoubleOrNull() ?: return null
            return CustomBotMetrics(x01Average = clampX01(x01), cricketMPR = clampMPR(mpr))
        }

        fun encode(metrics: CustomBotMetrics): String = "$PREFIX${metrics.x01Average}:${metrics.cricketMPR}"
    }
}

@Serializable
data class CustomBotConfiguration(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val x01Average: Double,
    val cricketMPR: Double,
    val explicitProfile: BotSkillProfile? = null,
    val facetOverrides: CustomBotFacetOverrides? = null,
    @SerialName("scoringBehaviorTier")
    val scoringBehaviorTierRaw: String? = null,
) {
    val metrics: CustomBotMetrics
        get() = CustomBotMetrics(
            x01Average = CustomBotMetrics.clampX01(x01Average),
            cricketMPR = CustomBotMetrics.clampMPR(cricketMPR),
        )

    val scoringBehaviorTier: BotDifficulty?
        get() = scoringBehaviorTierRaw?.let { BotDifficulty.fromRawValue(it) }

    val usesLegacyV1Encoding: Boolean
        get() = explicitProfile == null && facetOverrides == null && scoringBehaviorTier == null

    val isAdvanced: Boolean
        get() = !usesLegacyV1Encoding

    fun resolvedCanonicalProfile(): BotSkillProfile {
        var profile = explicitProfile ?: mergedDerivedProfile(
            x01Average = x01Average,
            cricketMPR = cricketMPR,
            scoringBehaviorTier = scoringBehaviorTier,
        )
        scoringBehaviorTier?.let { profile = profile.withScoringBehaviorTier(it) }
        facetOverrides?.let { profile = it.applyTo(profile) }
        return profile
    }

    fun resetToSimpleTargets(): CustomBotConfiguration = CustomBotConfiguration(
        schemaVersion = CURRENT_SCHEMA_VERSION,
        x01Average = x01Average,
        cricketMPR = cricketMPR,
    )

    fun resetToPreset(difficulty: BotDifficulty): CustomBotConfiguration {
        val profile = difficulty.skillProfile
        return CustomBotConfiguration(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            x01Average = x01Average,
            cricketMPR = cricketMPR,
            explicitProfile = profile,
            facetOverrides = CustomBotFacetOverrides.extract(profile),
            scoringBehaviorTierRaw = difficulty.rawValue,
        )
    }

    fun syncExplicitProfileFromFacets(): CustomBotConfiguration {
        var profile = mergedDerivedProfile(
            x01Average = x01Average,
            cricketMPR = cricketMPR,
            scoringBehaviorTier = scoringBehaviorTier,
        )
        val facets = facetOverrides
        return if (facets != null) {
            copy(
                explicitProfile = facets.applyTo(profile),
                schemaVersion = CURRENT_SCHEMA_VERSION,
            )
        } else {
            copy(
                explicitProfile = profile,
                facetOverrides = CustomBotFacetOverrides.extract(profile),
                schemaVersion = CURRENT_SCHEMA_VERSION,
            )
        }
    }

    fun withMetrics(x01Average: Double, cricketMPR: Double): CustomBotConfiguration = copy(
        x01Average = CustomBotMetrics.clampX01(x01Average),
        cricketMPR = CustomBotMetrics.clampMPR(cricketMPR),
    )

    fun withScoringBehaviorTier(tier: BotDifficulty?): CustomBotConfiguration =
        copy(scoringBehaviorTierRaw = tier?.rawValue)

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2

        fun from(metrics: CustomBotMetrics): CustomBotConfiguration = CustomBotConfiguration(
            schemaVersion = 1,
            x01Average = metrics.x01Average,
            cricketMPR = metrics.cricketMPR,
        )

        private fun mergedDerivedProfile(
            x01Average: Double,
            cricketMPR: Double,
            scoringBehaviorTier: BotDifficulty?,
        ): BotSkillProfile {
            val x01Slice = BotSkillProfileInterpolator.profileForX01Average(x01Average, clampToTierRange = false)
            val cricketSlice = BotSkillProfileInterpolator.profileForCricketMPR(cricketMPR, clampToTierRange = false)
            val tier = scoringBehaviorTier ?: x01Slice.x01.scoringBehaviorTier
            return BotSkillProfile(
                x01 = x01Slice.x01.copy(scoringBehaviorTierRaw = tier.rawValue),
                cricket = cricketSlice.cricket,
            )
        }
    }
}

object CustomBotConfigurationCodec {
    const val V2_PREFIX = "customV2:"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun decode(botDifficultyRaw: String?): CustomBotConfiguration? {
        val raw = botDifficultyRaw ?: return null
        if (raw.startsWith(V2_PREFIX)) {
            val body = raw.removePrefix(V2_PREFIX)
            return runCatching { json.decodeFromString<CustomBotConfiguration>(body) }.getOrNull()
        }
        return CustomBotMetrics.decode(raw)?.let { CustomBotConfiguration.from(it) }
    }

    fun encode(configuration: CustomBotConfiguration): String {
        if (configuration.usesLegacyV1Encoding) {
            return CustomBotMetrics.encode(configuration.metrics)
        }
        return runCatching {
            V2_PREFIX + json.encodeToString(configuration)
        }.getOrElse {
            CustomBotMetrics.encode(configuration.metrics)
        }
    }
}

object BotSkillProfileInterpolator {
    private val x01TierAverages = listOf(
        BotDifficulty.VeryEasy to 20.0,
        BotDifficulty.Easy to 29.0,
        BotDifficulty.Medium to 61.0,
        BotDifficulty.Hard to 75.0,
        BotDifficulty.Pro to 88.0,
    )

    private val cricketTierMPR = listOf(
        BotDifficulty.VeryEasy to 0.85,
        BotDifficulty.Easy to 1.25,
        BotDifficulty.Medium to 1.85,
        BotDifficulty.Hard to 2.45,
        BotDifficulty.Pro to 3.05,
    )

    fun profileForX01Average(target: Double, clampToTierRange: Boolean = true): BotSkillProfile =
        profile(target, x01TierAverages, clampToTierRange)

    fun profileForCricketMPR(target: Double, clampToTierRange: Boolean = true): BotSkillProfile =
        profile(target, cricketTierMPR, clampToTierRange)

    private fun profile(
        target: Double,
        anchors: List<Pair<BotDifficulty, Double>>,
        clampToTierRange: Boolean,
    ): BotSkillProfile {
        val value = if (clampToTierRange) {
            target.coerceIn(anchors.first().second, anchors.last().second)
        } else {
            target
        }
        val (lower, upper, fraction) = bracket(value, anchors, extrapolate = !clampToTierRange)
        return interpolate(
            from = lower.skillProfile,
            to = upper.skillProfile,
            fraction = fraction,
            scoringBehaviorTier = if (fraction < 0.5) lower else upper,
        )
    }

    private fun bracket(
        value: Double,
        anchors: List<Pair<BotDifficulty, Double>>,
        extrapolate: Boolean,
    ): Triple<BotDifficulty, BotDifficulty, Double> {
        if (anchors.size < 2) return Triple(anchors.first().first, anchors.first().first, 0.0)
        if (value <= anchors.first().second) {
            if (extrapolate && value < anchors.first().second) {
                val low = anchors[0]
                val high = anchors[1]
                val span = high.second - low.second
                val t = if (span > 0) (value - low.second) / span else 0.0
                return Triple(low.first, high.first, t)
            }
            return Triple(anchors[0].first, anchors[1].first, 0.0)
        }
        for (index in 0 until anchors.lastIndex) {
            val low = anchors[index]
            val high = anchors[index + 1]
            if (value <= high.second) {
                val span = high.second - low.second
                val t = if (span > 0) (value - low.second) / span else 0.0
                return Triple(low.first, high.first, if (extrapolate) t else t.coerceIn(0.0, 1.0))
            }
        }
        val last = anchors[anchors.size - 2]
        val top = anchors.last()
        if (extrapolate && value > top.second) {
            val span = top.second - last.second
            val t = if (span > 0) (value - last.second) / span else 1.0
            return Triple(last.first, top.first, t)
        }
        return Triple(last.first, top.first, 1.0)
    }

    private fun interpolate(
        from: BotSkillProfile,
        to: BotSkillProfile,
        fraction: Double,
        scoringBehaviorTier: BotDifficulty,
    ): BotSkillProfile {
        val t = fraction
        val lowerX01 = from.x01
        val upperX01 = to.x01
        val lowerCricket = from.cricket
        val upperCricket = to.cricket
        return BotSkillProfile(
            x01 = BotSkillProfile.X01(
                scoringVisitMin = lerpInt(lowerX01.scoringVisitMin, upperX01.scoringVisitMin, t),
                scoringVisitMax = lerpInt(lowerX01.scoringVisitMax, upperX01.scoringVisitMax, t),
                hitChances = lerpHits(lowerX01.hitChances, upperX01.hitChances, t),
                checkoutAttemptChance = lerp(lowerX01.checkoutAttemptChance, upperX01.checkoutAttemptChance, t),
                offBoardMissChance = lerp(lowerX01.offBoardMissChance, upperX01.offBoardMissChance, t),
                riskyBustChance = lerp(lowerX01.riskyBustChance, upperX01.riskyBustChance, t),
                triplePreference = lerp(lowerX01.triplePreference, upperX01.triplePreference, t),
                checkInHitBoost = lerp(lowerX01.checkInHitBoost, upperX01.checkInHitBoost, t),
                innerBullAimChance = lerp(lowerX01.innerBullAimChance, upperX01.innerBullAimChance, t),
                masterInTripleOpenerChance = lerp(lowerX01.masterInTripleOpenerChance, upperX01.masterInTripleOpenerChance, t),
                safeRemainingSingleOut = lerpInt(lowerX01.safeRemainingSingleOut, upperX01.safeRemainingSingleOut, t),
                safeRemainingDoubleOut = lerpInt(lowerX01.safeRemainingDoubleOut, upperX01.safeRemainingDoubleOut, t),
                safeRemainingMasterOut = lerpInt(lowerX01.safeRemainingMasterOut, upperX01.safeRemainingMasterOut, t),
                scoringBehaviorTierRaw = scoringBehaviorTier.rawValue,
            ),
            cricket = BotSkillProfile.Cricket(
                hitChances = lerpHits(lowerCricket.hitChances, upperCricket.hitChances, t),
                offBoardMissChance = lerp(lowerCricket.offBoardMissChance, upperCricket.offBoardMissChance, t),
                wrongBedChance = lerp(lowerCricket.wrongBedChance, upperCricket.wrongBedChance, t),
                innerBullAimChance = lerp(lowerCricket.innerBullAimChance, upperCricket.innerBullAimChance, t),
                tripleOnOpenChance = lerp(lowerCricket.tripleOnOpenChance, upperCricket.tripleOnOpenChance, t),
                doubleOnOpenChance = lerp(lowerCricket.doubleOnOpenChance, upperCricket.doubleOnOpenChance, t),
            ),
        )
    }

    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

    private fun lerpInt(a: Int, b: Int, t: Double): Int = (a + (b - a) * t).roundToInt()

    private fun lerpHits(
        a: BotSkillProfile.HitChances,
        b: BotSkillProfile.HitChances,
        t: Double,
    ): BotSkillProfile.HitChances = BotSkillProfile.HitChances(
        single = lerp(a.single, b.single, t),
        double = lerp(a.double, b.double, t),
        triple = lerp(a.triple, b.triple, t),
    )
}
