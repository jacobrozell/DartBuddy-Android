package com.jacobrozell.dartbuddy.domain.rules

import com.jacobrozell.dartbuddy.domain.model.MatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesCatalogTest {
    @Test
    fun guidesArePopulated() {
        for (mode in GameRulesCatalog.supportedMatchTypes) {
            val guide = GameRulesCatalog.guide(mode)
            assertEquals(mode, guide.matchType)
            assertTrue(guide.sections.isNotEmpty())
            assertTrue(guide.sections.all { it.titleRes != 0 && it.bodyRes != 0 })
        }
    }

    @Test
    fun cricketIncludesVariantSections() {
        val ids = GameRulesCatalog.guide(MatchType.Cricket).sections.map { it.id }.toSet()
        assertTrue(ids.contains("normalScore"))
        assertTrue(ids.contains("cutThroatScore"))
        assertTrue(ids.contains("noScore"))
    }

    @Test
    fun x01IncludesCheckInAndCheckOutSections() {
        val ids = GameRulesCatalog.guide(MatchType.X01).sections.map { it.id }.toSet()
        assertTrue(ids.contains("checkIn"))
        assertTrue(ids.contains("checkOut"))
    }

    @Test
    fun supportedTypesForOnboarding() {
        assertEquals(listOf(MatchType.X01, MatchType.Cricket), GameRulesCatalog.supportedMatchTypes)
    }

    @Test
    fun hasGuideMatchesLeanSurface() {
        assertTrue(GameRulesCatalog.hasGuide(MatchType.X01))
        assertTrue(GameRulesCatalog.hasGuide(MatchType.Cricket))
    }
}
