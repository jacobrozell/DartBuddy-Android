package com.jacobrozell.dartbuddy.domain.service

import com.jacobrozell.dartbuddy.domain.engine.CricketPlayerState
import com.jacobrozell.dartbuddy.domain.engine.CricketState
import com.jacobrozell.dartbuddy.domain.engine.X01PlayerState
import com.jacobrozell.dartbuddy.domain.engine.X01State
import com.jacobrozell.dartbuddy.domain.model.MatchConfigCricket
import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.model.X01LegFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchForfeitResolverTest {
    private val x01Config = MatchConfigX01(
        startScore = 501,
        checkoutMode = X01CheckoutMode.DoubleOut,
        checkInMode = X01CheckInMode.StraightIn,
        legsToWin = 1,
        setsEnabled = false,
        setsToWin = null,
        legFormat = X01LegFormat.FirstTo,
    )

    @Test
    fun x01_twoPlayer_autoWinnerIsOtherPlayer() {
        val state = X01State(
            config = x01Config,
            players = listOf(
                X01PlayerState("p1", 120, 0, 0),
                X01PlayerState("p2", 301, 0, 0),
            ),
            currentPlayerIndex = 0,
            legIndex = 0,
            setIndex = 0,
            turnIndex = 0,
        )
        val resolution = MatchForfeitResolver.resolveX01(
            state,
            listOf("Alice", "Bob"),
            listOf("p1", "p2"),
            "p1",
        )
        assertTrue(resolution is ForfeitWinnerResolution.Automatic)
        assertEquals("p2", (resolution as ForfeitWinnerResolution.Automatic).winnerPlayerId)
    }

    @Test
    fun humanParticipantsExcludeBots() {
        val humans = MatchForfeitResolver.humanParticipantIds(
            listOf("h1", "b1"),
            mapOf("h1" to false, "b1" to true),
        )
        assertEquals(listOf("h1"), humans)
    }

    @Test
    fun cricket_forfeitPicksLeadingScore() {
        val config = MatchConfigCricket(pointsEnabled = true, legsToWin = 1)
        val state = CricketState(
            config = config,
            players = listOf(
                CricketPlayerState("p1", 30, mapOf("20" to 3)),
                CricketPlayerState("p2", 10, mapOf("19" to 2)),
                CricketPlayerState("p3", 5, mapOf("18" to 1)),
            ),
            currentPlayerIndex = 0,
            roundIndex = 0,
            turnIndex = 0,
        )
        val resolution = MatchForfeitResolver.resolveCricket(
            state,
            listOf("A", "B", "C"),
            listOf("p1", "p2", "p3"),
            "p3",
        )
        assertTrue(resolution is ForfeitWinnerResolution.Automatic)
        assertEquals("p1", (resolution as ForfeitWinnerResolution.Automatic).winnerPlayerId)
    }
}
