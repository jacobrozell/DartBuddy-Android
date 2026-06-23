package com.jacobrozell.dartbuddy.domain.engine

import com.jacobrozell.dartbuddy.domain.model.MatchConfigX01
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class X01EngineTest {
    @Test
    fun singleOutCheckoutCompletesMatch() {
        val players = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val config = MatchConfigX01(
            startScore = 301,
            legsToWin = 1,
            setsEnabled = false,
            checkoutMode = X01CheckoutMode.SingleOut,
        )
        var state = X01Engine.makeInitialState(config, players)

        state = X01Engine.submitTurn(state, 180, null).updatedState
        state = X01Engine.submitTurn(state, 0, null).updatedState
        state = X01Engine.submitTurn(state, 121, null).updatedState

        assertTrue(state.isComplete)
        assertEquals(players[0], state.winnerPlayerId)
    }

    @Test
    fun soloMatchCompletesOnCheckout() {
        val players = listOf(UUID.randomUUID().toString())
        val config = MatchConfigX01(
            startScore = 301,
            legsToWin = 1,
            setsEnabled = false,
            checkoutMode = X01CheckoutMode.SingleOut,
        )
        var state = X01Engine.makeInitialState(config, players)

        state = X01Engine.submitTurn(state, 180, null).updatedState
        assertEquals(0, state.currentPlayerIndex)
        state = X01Engine.submitTurn(state, 121, null).updatedState

        assertTrue(state.isComplete)
        assertEquals(players[0], state.winnerPlayerId)
    }

    @Test
    fun doubleOutInvalidFinishBecomesBust() {
        val players = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val config = MatchConfigX01(
            startScore = 301,
            legsToWin = 1,
            setsEnabled = false,
            checkoutMode = X01CheckoutMode.DoubleOut,
        )
        var state = X01Engine.makeInitialState(config, players)

        state = X01Engine.submitTurn(state, 180, null).updatedState
        state = X01Engine.submitTurn(state, 0, null).updatedState
        state = X01Engine.submitTurn(state, 81, null).updatedState
        state = X01Engine.submitTurn(state, 0, null).updatedState

        val outcome = X01Engine.submitTurn(
            state,
            null,
            listOf(
                DartInput(DartMultiplier.Single, DartSegment.OneToTwenty(20)),
                DartInput(DartMultiplier.Single, DartSegment.OneToTwenty(20)),
            ),
        )

        assertTrue(outcome.event.isBust)
        assertEquals(40, outcome.updatedState.players[0].remainingScore)
        assertFalse(outcome.updatedState.isComplete)
    }

    @Test
    fun overflowBustResetsAppliedScore() {
        val players = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val config = MatchConfigX01(
            startScore = 301,
            legsToWin = 1,
            setsEnabled = false,
            checkoutMode = X01CheckoutMode.SingleOut,
        )
        var state = X01Engine.makeInitialState(config, players)

        state = X01Engine.submitTurn(state, 180, null).updatedState
        state = X01Engine.submitTurn(state, 0, null).updatedState
        state = X01Engine.submitTurn(state, 61, null).updatedState
        state = X01Engine.submitTurn(state, 0, null).updatedState

        val outcome = X01Engine.submitTurn(state, 100, null)

        assertTrue(outcome.event.isBust)
        assertEquals(0, outcome.event.appliedTotal)
        assertEquals(60, outcome.updatedState.players[0].remainingScore)
    }
}
