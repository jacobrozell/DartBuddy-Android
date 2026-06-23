package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.data.repository.MatchSummary
import com.jacobrozell.dartbuddy.domain.model.MatchStatus
import com.jacobrozell.dartbuddy.domain.model.MatchType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PlayerStatsAggregatorTest {
    @Test
    fun aggregate_returnsNullWhenNoGames() = runBlocking {
        val result = PlayerStatsAggregator.aggregate(
            matches = emptyList(),
            playerId = "p1",
            playerName = "Alice",
            matchType = MatchType.X01,
            statsForMatch = { emptyList() },
        )
        assertNull(result)
    }

    @Test
    fun aggregate_mergesWinsAndStats() = runBlocking {
        val match = MatchSummary(
            id = "m1",
            matchType = MatchType.X01,
            status = MatchStatus.Completed,
            participantNames = listOf("Alice", "Bob"),
            participantIds = listOf("p1", "p2"),
            winnerPlayerId = "p1",
            forfeitedByPlayerId = null,
            startedAt = Instant.parse("2025-01-01T12:00:00Z"),
            updatedAt = Instant.parse("2025-01-01T12:30:00Z"),
        )
        val stats = listOf(
            PlayerMatchStats(
                playerId = "p1",
                name = "Alice",
                darts = 9,
                points = 180,
                highestScore = 60,
                highestCheckout = 0,
                checkouts = 0,
                legsWon = 1,
                setsWon = 0,
                score = 0,
                marksPerRound = 0.0,
                doubles = 0,
                triples = 3,
            ),
        )
        val result = PlayerStatsAggregator.aggregate(
            matches = listOf(match),
            playerId = "p1",
            playerName = "Alice",
            matchType = MatchType.X01,
            statsForMatch = { stats },
        )
        assertNotNull(result)
        assertEquals(1, result!!.games)
        assertEquals(1, result.wins)
        assertEquals(60.0, result.stats.average3Dart, 0.01)
    }

    @Test
    fun recentMatches_listsOpponentsAndOutcome() {
        val match = MatchSummary(
            id = "m1",
            matchType = MatchType.Cricket,
            status = MatchStatus.Completed,
            participantNames = listOf("Alice", "Bob"),
            participantIds = listOf("p1", "p2"),
            winnerPlayerId = "p2",
            forfeitedByPlayerId = null,
            startedAt = Instant.parse("2025-01-02T12:00:00Z"),
            updatedAt = Instant.parse("2025-01-02T12:30:00Z"),
        )
        val recent = PlayerStatsAggregator.recentMatches(listOf(match), "p1")
        assertEquals(1, recent.size)
        assertEquals("Bob", recent.first().opponentLabel)
        assertEquals(false, recent.first().didWin)
    }
}
