package com.jacobrozell.dartbuddy.domain.bots

import com.jacobrozell.dartbuddy.domain.engine.CricketEngine
import com.jacobrozell.dartbuddy.domain.model.MatchConfigCricket
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import java.util.UUID

class DartBotEngineTest {
    @Test
    fun generateX01Turn_producesOneToThreeDarts() {
        val rng = Random(42)
        val darts = DartBotEngine.generateX01Turn(
            remaining = 501,
            difficulty = BotDifficulty.Medium,
            checkoutMode = X01CheckoutMode.DoubleOut,
            checkInMode = X01CheckInMode.StraightIn,
            isCheckedIn = true,
            rng = rng,
        )
        assertTrue(darts.size in 1..3)
        assertTrue(darts.all { !it.isMiss || it.points == 0 })
    }

    @Test
    fun dartFromCheckoutLabel_parsesCommonLabels() {
        assertEquals(DartMultiplier.Triple, DartBotEngine.dartFromCheckoutLabel("T20")?.multiplier)
        assertEquals(DartMultiplier.Double, DartBotEngine.dartFromCheckoutLabel("D16")?.multiplier)
        assertEquals(DartSegment.InnerBull, DartBotEngine.dartFromCheckoutLabel("Bull")?.segment)
    }

    @Test
    fun generateCricketTurn_producesThreeDarts() {
        val config = MatchConfigCricket()
        val playerIds = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val state = CricketEngine.makeInitialState(config, playerIds)
        val darts = DartBotEngine.generateCricketTurn(
            state = state,
            playerIndex = 0,
            difficulty = BotDifficulty.Medium,
            rng = Random(7),
        )
        assertEquals(3, darts.size)
    }

    @Test
    fun proBotAveragesHigherThanHard() {
        var hardTotal = 0
        var proTotal = 0
        for (seed in 0 until 80) {
            hardTotal += DartBotEngine.generateX01Turn(
                remaining = 501,
                difficulty = BotDifficulty.Hard,
                checkoutMode = X01CheckoutMode.DoubleOut,
                checkInMode = X01CheckInMode.StraightIn,
                isCheckedIn = true,
                rng = Random(seed),
            ).sumOf { it.points }
            proTotal += DartBotEngine.generateX01Turn(
                remaining = 501,
                difficulty = BotDifficulty.Pro,
                checkoutMode = X01CheckoutMode.DoubleOut,
                checkInMode = X01CheckInMode.StraightIn,
                isCheckedIn = true,
                rng = Random(seed),
            ).sumOf { it.points }
        }
        assertTrue(proTotal > hardTotal)
    }

    @Test
    fun easyBotAveragesHigherThanVeryEasy() {
        var veryEasyTotal = 0
        var easyTotal = 0
        for (seed in 0 until 80) {
            veryEasyTotal += DartBotEngine.generateX01Turn(
                remaining = 501,
                difficulty = BotDifficulty.VeryEasy,
                checkoutMode = X01CheckoutMode.DoubleOut,
                checkInMode = X01CheckInMode.StraightIn,
                isCheckedIn = true,
                rng = Random(seed),
            ).sumOf { it.points }
            easyTotal += DartBotEngine.generateX01Turn(
                remaining = 501,
                difficulty = BotDifficulty.Easy,
                checkoutMode = X01CheckoutMode.DoubleOut,
                checkInMode = X01CheckInMode.StraightIn,
                isCheckedIn = true,
                rng = Random(seed),
            ).sumOf { it.points }
        }
        assertTrue(easyTotal > veryEasyTotal)
    }
}
