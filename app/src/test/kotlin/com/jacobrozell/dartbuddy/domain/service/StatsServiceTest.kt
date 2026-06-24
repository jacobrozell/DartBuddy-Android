package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.domain.engine.CricketDartTouch
import com.jacobrozell.dartbuddy.domain.engine.CricketTurnEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsServiceTest {
    @Test
    fun x01Average3Dart_computesThreeDartAverage() {
        assertEquals(0.0, StatsService.x01Average3Dart(100, 0), 0.001)
        assertEquals(60.0, StatsService.x01Average3Dart(180, 9), 0.001)
        assertEquals(45.0, StatsService.x01Average3Dart(45, 3), 0.001)
    }

    @Test
    fun x01LiveScorecardAverage_usesPerDartForOpeningVisit() {
        assertEquals(11.0, StatsService.x01LiveScorecardAverage(0, 0, 11, 1), 0.001)
        assertEquals(20.0, StatsService.x01LiveScorecardAverage(0, 0, 40, 2), 0.001)
        assertEquals(60.0, StatsService.x01LiveScorecardAverage(0, 0, 60, 3), 0.001)
        assertEquals(150.0, StatsService.x01LiveScorecardAverage(180, 3, 20, 1), 0.001)
    }

    @Test
    fun x01LiveScorecardAverage_returnsZeroWithNoDarts() {
        assertEquals(0.0, StatsService.x01LiveScorecardAverage(0, 0, 0, 0), 0.001)
    }

    @Test
    fun cricketDartsThrown_countsEveryTouchIncludingMisses() {
        val event = CricketTurnEvent(
            id = "1",
            playerId = "p1",
            turnIndex = 0,
            roundIndex = 0,
            totalPointsAdded = 0,
            targetsTouched = listOf(
                CricketDartTouch(0, "20", "triple", 3, 0, 0, false),
                CricketDartTouch(1, "0", "single", 0, 0, 0, true),
            ),
            timestampEpochMillis = 0L,
        )
        assertEquals(2, StatsService.cricketDartsThrown(listOf(event)))
    }
}
