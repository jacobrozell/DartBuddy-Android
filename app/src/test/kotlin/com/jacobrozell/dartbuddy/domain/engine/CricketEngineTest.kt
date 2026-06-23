package com.jacobrozell.dartbuddy.domain.engine

import com.jacobrozell.dartbuddy.domain.model.MatchConfigCricket
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class CricketEngineTest {
    @Test
    fun closureAndOverflowScoring() {
        val players = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val config = MatchConfigCricket()
        var state = CricketEngine.makeInitialState(config, players)

        state = CricketEngine.submitTurn(
            state,
            listOf(DartInput(DartMultiplier.Triple, DartSegment.OneToTwenty(20))),
        ).updatedState

        state = CricketEngine.submitTurn(state, emptyList()).updatedState

        val outcome = CricketEngine.submitTurn(
            state,
            listOf(DartInput(DartMultiplier.Triple, DartSegment.OneToTwenty(20))),
        )

        assertEquals(60, outcome.updatedState.players[0].score)
    }

    @Test
    fun bullMarksMapOuterAndInnerCorrectly() {
        val players = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        val config = MatchConfigCricket()
        val state = CricketEngine.makeInitialState(config, players)

        val outcome = CricketEngine.submitTurn(
            state,
            listOf(
                DartInput(DartMultiplier.Single, DartSegment.OuterBull),
                DartInput(DartMultiplier.Double, DartSegment.InnerBull),
            ),
        )

        assertEquals(3, outcome.updatedState.players[0].marks["bull"])
    }
}
