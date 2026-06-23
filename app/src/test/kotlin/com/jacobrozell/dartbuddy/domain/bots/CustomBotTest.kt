package com.jacobrozell.dartbuddy.domain.bots

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomBotTest {
    @Test
    fun metricsRoundTripV1Encoding() {
        val metrics = CustomBotMetrics(x01Average = 45.0, cricketMPR = 2.1)
        val encoded = CustomBotMetrics.encode(metrics)
        val decoded = CustomBotMetrics.decode(encoded)
        assertNotNull(decoded)
        assertEquals(45.0, decoded!!.x01Average, 0.001)
        assertEquals(2.1, decoded.cricketMPR, 0.001)
    }

    @Test
    fun configurationV1RoundTripDecodesLegacyString() {
        val raw = "custom:30.0:1.25"
        val configuration = CustomBotConfigurationCodec.decode(raw)
        assertNotNull(configuration)
        assertEquals(1, configuration!!.schemaVersion)
        assertEquals(30.0, configuration.x01Average, 0.001)
        assertEquals(1.25, configuration.cricketMPR, 0.001)
        assertEquals(raw, CustomBotConfigurationCodec.encode(configuration))
    }

    @Test
    fun configurationV2RoundTripPreservesAdvancedFields() {
        val configuration = CustomBotConfiguration(
            schemaVersion = 2,
            x01Average = 55.0,
            cricketMPR = 2.2,
            scoringBehaviorTierRaw = BotDifficulty.Hard.rawValue,
            facetOverrides = CustomBotFacetOverrides(
                x01 = X01SkillFacet(checkoutAttemptChance = 0.42),
            ),
        )
        val encoded = CustomBotConfigurationCodec.encode(configuration)
        assertTrue(encoded.startsWith(CustomBotConfigurationCodec.V2_PREFIX))
        val decoded = CustomBotConfigurationCodec.decode(encoded)
        assertEquals(configuration, decoded)
    }

    @Test
    fun resetToSimpleClearsAdvancedState() {
        val configuration = CustomBotConfiguration(
            x01Average = 50.0,
            cricketMPR = 2.0,
            explicitProfile = BotDifficulty.Medium.skillProfile,
            scoringBehaviorTierRaw = BotDifficulty.Medium.rawValue,
            facetOverrides = CustomBotFacetOverrides(
                x01 = X01SkillFacet(checkoutAttemptChance = 0.5),
            ),
        )
        val reset = configuration.resetToSimpleTargets()
        assertNull(reset.explicitProfile)
        assertNull(reset.facetOverrides)
        assertNull(reset.scoringBehaviorTier)
        assertEquals(50.0, reset.x01Average, 0.001)
        assertEquals(2.0, reset.cricketMPR, 0.001)
    }

    @Test
    fun configurationResolvesProfile() {
        val config = CustomBotConfiguration.from(CustomBotMetrics(30.0, 1.25))
        val profile = config.resolvedCanonicalProfile()
        assertTrue(profile.x01.scoringVisitMin > 0)
        assertTrue(profile.cricket.hitChances.single > 0)
    }

    @Test
    fun canonicalProfileMergesSlices() {
        val configuration = CustomBotConfiguration(x01Average = 55.0, cricketMPR = 2.2)
        val profile = configuration.resolvedCanonicalProfile()
        val x01Slice = BotSkillProfileInterpolator.profileForX01Average(55.0, clampToTierRange = false)
        val cricketSlice = BotSkillProfileInterpolator.profileForCricketMPR(2.2, clampToTierRange = false)
        assertEquals(x01Slice.x01.scoringVisitMax, profile.x01.scoringVisitMax)
        assertEquals(cricketSlice.cricket.hitChances.triple, profile.cricket.hitChances.triple, 0.001)
    }

    @Test
    fun syncExplicitProfileFromFacetsIsIdempotent() {
        var configuration = CustomBotConfiguration(x01Average = 50.0, cricketMPR = 2.0)
        configuration = configuration.copy(
            facetOverrides = CustomBotFacetOverrides(
                x01 = X01SkillFacet(checkoutAttemptChance = 0.5),
            ),
        ).syncExplicitProfileFromFacets()
        val first = configuration.explicitProfile
        configuration = configuration.syncExplicitProfileFromFacets()
        assertEquals(first, configuration.explicitProfile)
    }

    @Test
    fun facetCheckoutOverrideAppliesToProfile() {
        val configuration = CustomBotConfiguration(
            x01Average = 40.0,
            cricketMPR = 1.5,
            facetOverrides = CustomBotFacetOverrides(
                x01 = X01SkillFacet(checkoutAttemptChance = 0.88),
            ),
        )
        val profile = configuration.resolvedCanonicalProfile()
        assertEquals(0.88, profile.x01.checkoutAttemptChance, 0.001)
    }

    @Test
    fun higherX01AverageProducesHigherVisitCeiling() {
        val low = CustomBotConfiguration.from(CustomBotMetrics(20.0, 1.0)).resolvedCanonicalProfile()
        val high = CustomBotConfiguration.from(CustomBotMetrics(80.0, 1.0)).resolvedCanonicalProfile()
        assertTrue(high.x01.scoringVisitMax >= low.x01.scoringVisitMax)
    }
}
