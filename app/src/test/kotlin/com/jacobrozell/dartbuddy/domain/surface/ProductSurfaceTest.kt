package com.jacobrozell.dartbuddy.domain.surface

import com.jacobrozell.dartbuddy.domain.model.MatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSurfaceTest {
    @Test
    fun leanLaunchArgumentExposesLean1_0Gates() {
        val config = ProductSurface.configurationFor(listOf(ProductSurface.LEAN_SURFACE_LAUNCH_ARGUMENT))

        assertFalse(config.showsModesTab)
        assertFalse(config.showsPartyModes)
        assertFalse(config.showsCoopModes)
        assertFalse(config.showsTrainingBots)
        assertTrue(config.showsCustomBots)
        assertFalse(config.showsPlayerExport)
        assertTrue(config.showsAccessibilityMarketing)
        assertEquals(listOf("en"), config.bundledLocaleCodes)
    }

    @Test
    fun fullLaunchArgumentEnablesExtendedAreas() {
        val config = ProductSurface.configurationFor(listOf(ProductSurface.FULL_SURFACE_LAUNCH_ARGUMENT))

        assertTrue(config.showsModesTab)
        assertTrue(config.showsPartyModes)
        assertTrue(config.showsCoopModes)
        assertTrue(config.showsTrainingBots)
        assertTrue(config.showsCustomBots)
        assertTrue(config.showsPlayerExport)
        assertTrue(config.showsAccessibilityMarketing)
        assertEquals(listOf("en", "de", "es", "nl", "fr", "zh-Hans", "it"), config.bundledLocaleCodes)
    }

    @Test
    fun leanSurfaceOnlyReachableForX01AndCricket() {
        val args = listOf(ProductSurface.LEAN_SURFACE_LAUNCH_ARGUMENT)

        assertTrue(ProductSurface.isMatchTypeReachable(MatchType.X01, args))
        assertTrue(ProductSurface.isMatchTypeReachable(MatchType.Cricket, args))
    }

    @Test
    fun fullSurfaceReachableForAllShippedMatchTypes() {
        val args = listOf(ProductSurface.FULL_SURFACE_LAUNCH_ARGUMENT)

        MatchType.entries.forEach { matchType ->
            assertTrue(ProductSurface.isMatchTypeReachable(matchType, args))
        }
    }
}
