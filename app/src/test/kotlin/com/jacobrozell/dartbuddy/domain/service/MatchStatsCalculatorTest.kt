package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.domain.engine.CricketDartTouch
import com.jacobrozell.dartbuddy.domain.engine.CricketTurnEvent
import com.jacobrozell.dartbuddy.domain.engine.X01DartEvent
import com.jacobrozell.dartbuddy.domain.engine.X01TurnEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchStatsCalculatorTest {
    @Test
    fun x01Stats_tracksSectorHitsAndCheckouts() {
        val playerId = "p1"
        val events = listOf(
            X01TurnEvent(
                id = "1",
                playerId = playerId,
                turnIndex = 0,
                legIndex = 0,
                setIndex = 0,
                startRemaining = 501,
                enteredTotal = 60,
                appliedTotal = 60,
                endRemaining = 441,
                isBust = false,
                didCheckout = false,
                checkoutModeRaw = "doubleOut",
                darts = listOf(
                    X01DartEvent(0, "single", "20", 20, false),
                    X01DartEvent(1, "triple", "20", 60, false),
                    X01DartEvent(2, "miss", "0", 0, true),
                ),
                timestampEpochMillis = 0L,
            ),
            X01TurnEvent(
                id = "2",
                playerId = playerId,
                turnIndex = 1,
                legIndex = 0,
                setIndex = 0,
                startRemaining = 32,
                enteredTotal = 32,
                appliedTotal = 32,
                endRemaining = 0,
                isBust = false,
                didCheckout = true,
                checkoutModeRaw = "doubleOut",
                darts = listOf(
                    X01DartEvent(0, "double", "16", 32, false),
                ),
                timestampEpochMillis = 1L,
            ),
        )

        val stats = MatchStatsCalculator.x01Stats(
            participantIds = listOf(playerId),
            names = listOf("Alice"),
            events = events,
            finalLegsWon = mapOf(playerId to 1),
            finalSetsWon = mapOf(playerId to 0),
            winnerId = playerId,
        ).single()

        assertEquals(4, stats.darts)
        assertEquals(92, stats.points)
        assertEquals(1, stats.checkouts)
        assertEquals(32, stats.highestCheckout)
        assertEquals(60, stats.highestScore)
        assertEquals(1, stats.triples)
        assertEquals(1, stats.doubles)
        assertEquals(2, stats.hitsBySector["20"])
        assertEquals(1, stats.hitsBySector[HitsBySectorKeys.MISS])
    }

    @Test
    fun cricketStats_tracksMarksAndSectors() {
        val playerId = "p1"
        val events = listOf(
            CricketTurnEvent(
                id = "1",
                playerId = playerId,
                turnIndex = 0,
                roundIndex = 0,
                totalPointsAdded = 0,
                targetsTouched = listOf(
                    CricketDartTouch(
                        dartOrder = 0,
                        targetRaw = "20",
                        multiplierRaw = "triple",
                        marksAdded = 3,
                        overflowMarks = 0,
                        pointsAdded = 0,
                        wasMiss = false,
                    ),
                ),
                timestampEpochMillis = 0L,
            ),
        )

        val stats = MatchStatsCalculator.cricketStats(
            participantIds = listOf(playerId),
            names = listOf("Bob"),
            events = events,
            finalScores = mapOf(playerId to 0),
            finalLegsWon = mapOf(playerId to 0),
            finalSetsWon = mapOf(playerId to 0),
            winnerId = null,
        ).single()

        assertEquals(1, stats.darts)
        assertEquals(1, stats.triples)
        assertEquals(1, stats.hitsBySector["20"])
        assertTrue(stats.marksPerRound > 0)
    }
}
