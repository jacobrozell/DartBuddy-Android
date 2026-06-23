package com.jacobrozell.dartbuddy.domain.bots

import com.jacobrozell.dartbuddy.domain.model.MatchType
import com.jacobrozell.dartbuddy.domain.service.PlayerModeStats
import com.jacobrozell.dartbuddy.domain.service.PlayerMatchStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingBotEligibilityServiceTest {
    @Test
    fun eligibility_requiresFiveGames() {
        val stats = PlayerModeStats(
            games = 4,
            wins = 2,
            stats = sampleStats(),
        )
        assertFalse(TrainingBotEligibilityService.eligibility(stats, MatchType.X01).isEligible)
        assertEquals(4, TrainingBotEligibilityService.eligibility(stats, MatchType.X01).gamesPlayed)

        val eligible = stats.copy(games = 5)
        assertTrue(TrainingBotEligibilityService.eligibility(eligible, MatchType.X01).isEligible)
    }

    @Test
    fun skillResolver_bumpsX01Average() {
        val stats = PlayerModeStats(
            games = 5,
            wins = 3,
            stats = sampleStats(averagePoints = 4500, darts = 300),
        )
        assertTrue(stats.stats.average3Dart > 40.0)
        val profile = TrainingBotSkillResolver.resolve(stats, MatchType.X01)
        val veryEasy = BotDifficulty.VeryEasy.skillProfile
        assertTrue(profile.x01.checkoutAttemptChance > veryEasy.x01.checkoutAttemptChance)
    }

    private fun sampleStats(averagePoints: Int = 180, darts: Int = 9) = PlayerMatchStats(
        playerId = "p1",
        name = "Test",
        darts = darts,
        points = averagePoints,
        highestScore = 60,
        highestCheckout = 0,
        checkouts = 0,
        legsWon = 1,
        setsWon = 0,
        score = 0,
        marksPerRound = 0.0,
        doubles = 0,
        triples = 0,
    )
}
